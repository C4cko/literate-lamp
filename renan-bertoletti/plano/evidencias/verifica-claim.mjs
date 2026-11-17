// Confere no servidor de demonstração as transições de Claim (W1–W12) e alguns caminhos proibidos.
// Uso: node verifica-claim.mjs   (Node 18+; grava verificacao-<sufixo>.json nesta pasta)
// Cria dados próprios com sufixo único: 2 funcionários (VVEss/VVSup), 2 usuários ESS, o vínculo
// Report-to e uma reivindicação por cenário. Issue #6.
const BASE = 'https://opensource-demo.orangehrmlive.com/web/index.php';
const SUF = Date.now().toString(36);
const PWD = 'Vv#Teste2026';
const log = [];

class Session {
  constructor(name) { this.name = name; this.cookies = {}; }
  store(res) {
    for (const c of res.headers.getSetCookie?.() ?? []) {
      const [kv] = c.split(';'); const i = kv.indexOf('=');
      this.cookies[kv.slice(0, i)] = kv.slice(i + 1);
    }
  }
  header() { return Object.entries(this.cookies).map(([k, v]) => `${k}=${v}`).join('; '); }
  async login(user, pass) {
    let r = await fetch(`${BASE}/auth/login`, { headers: { cookie: this.header() } });
    this.store(r); const html = await r.text();
    const token = html.match(/:token="&quot;([^&]+)&quot;"/)[1];
    r = await fetch(`${BASE}/auth/validate`, {
      method: 'POST', redirect: 'manual',
      headers: { cookie: this.header(), 'content-type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ _token: token, username: user, password: pass }),
    });
    this.store(r);
    if (!String(r.headers.get('location')).includes('dashboard')) throw new Error(`login ${user} falhou`);
  }
  async api(method, path, body) {
    const r = await fetch(`${BASE}/api/v2/${path}`, {
      method, headers: { cookie: this.header(), 'content-type': 'application/json', accept: 'application/json' },
      body: body ? JSON.stringify(body) : undefined,
    });
    this.store(r);
    const text = await r.text(); let json; try { json = JSON.parse(text); } catch { json = text.slice(0, 200); }
    return { status: r.status, json };
  }
}

const admin = new Session('ADMIN'), ess = new Session('ESS'), sup = new Session('SUPERVISOR');
const S = { ADMIN: admin, ESS: ess, SUPERVISOR: sup };
let essEmp;

async function must(sess, method, path, body) {
  const r = await sess.api(method, path, body);
  if (r.status !== 200) throw new Error(`${sess.name} ${method} ${path} -> ${r.status} ${JSON.stringify(r.json)}`);
  return r.json.data;
}

const refs = {};
async function view(id) {
  // a listagem usa EmployeeClaimRequestModel, que traz status e submittedDate
  const r = await must(admin, 'GET', `claim/employees/requests?referenceId=${refs[id]}&limit=1`);
  return r[0];
}
async function allowed(sessName, id) {
  const path = sessName === 'ESS' ? `claim/requests/${id}` : `claim/employees/${essEmp}/requests/${id}`;
  const r = await S[sessName].api('GET', path);
  return r.status === 200 ? (r.json.meta?.allowedActions ?? []).map(a => a.action) : [`HTTP ${r.status}`];
}

async function novaReivindicacao(criador = 'ESS') {
  const body = { claimEventId: 1, currencyId: 'USD', remarks: `VV ${SUF} ${criador}` };
  const d = criador === 'ESS'
    ? await must(ess, 'POST', 'claim/requests', body)
    : await must(S[criador], 'POST', `claim/employees/${essEmp}/requests`, body);
  refs[d.id] = d.referenceId;
  await must(S[criador], 'POST', `claim/requests/${d.id}/expenses`, { expenseTypeId: 1, date: '2026-11-10', amount: 10.5, note: 'VV' });
  return d.id;
}

async function acao(id, papel, action, rotulo) {
  const antes = (await view(id)).status;
  const permitidas = await allowed(papel, id);
  const r = await S[papel].api('PUT', `claim/requests/${id}/action`, { action });
  const v = await view(id);
  const depois = v.status;
  const linha = { rotulo, id, referenceId: refs[id], papel, antes, permitidas: permitidas.join(','), action,
    http: r.status, erro: r.status === 200 ? undefined : r.json?.error?.message, depois,
    submittedDate: v.submittedDate ?? null };
  log.push(linha); console.log(JSON.stringify(linha));
  return depois;
}

async function main() {
  await admin.login('Admin', 'admin123');
  const e = await must(admin, 'POST', 'pim/employees', { firstName: 'VVEss', lastName: SUF });
  const s = await must(admin, 'POST', 'pim/employees', { firstName: 'VVSup', lastName: SUF });
  essEmp = e.empNumber;
  await must(admin, 'POST', 'admin/users', { username: `vvess${SUF}`, password: PWD, status: true, userRoleId: 2, empNumber: e.empNumber });
  await must(admin, 'POST', 'admin/users', { username: `vvsup${SUF}`, password: PWD, status: true, userRoleId: 2, empNumber: s.empNumber });
  await must(admin, 'POST', `pim/employees/${e.empNumber}/supervisors`, { empNumber: s.empNumber, reportingMethodId: 1 });
  await ess.login(`vvess${SUF}`, PWD); await sup.login(`vvsup${SUF}`, PWD);
  console.log(JSON.stringify({ sufixo: SUF, essEmp: e.empNumber, supEmp: s.empNumber }));

  let id;
  id = await novaReivindicacao(); await acao(id, 'ESS', 'SUBMIT', 'W1');
  await acao(id, 'ESS', 'APPROVE', 'proibido: ESS aprova a própria');
  await acao(id, 'ESS', 'CANCEL', 'W3');
  id = await novaReivindicacao(); await acao(id, 'ESS', 'CANCEL', 'W2');
  await acao(id, 'ESS', 'SUBMIT', 'proibido: SUBMIT em CANCELLED');
  id = await novaReivindicacao(); await acao(id, 'ESS', 'SUBMIT', 'W1');
  await acao(id, 'ADMIN', 'APPROVE', 'W7 (admin aprova)');
  await acao(id, 'ADMIN', 'CANCEL', 'proibido: CANCEL em PAID');
  id = await novaReivindicacao(); await acao(id, 'ESS', 'SUBMIT', 'W1');
  await acao(id, 'ADMIN', 'REJECT', 'W8'); await acao(id, 'ESS', 'SUBMIT', 'W4 (ressubmete)');
  id = await novaReivindicacao(); await acao(id, 'ESS', 'SUBMIT', 'W1');
  await acao(id, 'SUPERVISOR', 'APPROVE', 'W11');
  await acao(id, 'SUPERVISOR', 'PAY', 'proibido: supervisor paga');
  await acao(id, 'ESS', 'CANCEL', 'proibido: ESS cancela APPROVED');
  await acao(id, 'ADMIN', 'PAY', 'W9');
  id = await novaReivindicacao(); await acao(id, 'ESS', 'SUBMIT', 'W1');
  await acao(id, 'SUPERVISOR', 'APPROVE', 'W11'); await acao(id, 'ADMIN', 'REJECT', 'W6');
  id = await novaReivindicacao(); await acao(id, 'ESS', 'SUBMIT', 'W1');
  await acao(id, 'SUPERVISOR', 'REJECT', 'W12');
  await acao(id, 'ESS', 'CANCEL', 'proibido: ESS cancela REJECTED');
  id = await novaReivindicacao('ADMIN'); await acao(id, 'ADMIN', 'SUBMIT', 'W5 (admin atribui e submete)');
  id = await novaReivindicacao('SUPERVISOR'); await acao(id, 'SUPERVISOR', 'SUBMIT', 'W10 (supervisor atribui e submete)');
  id = await novaReivindicacao('ADMIN');
  const vis = await ess.api('GET', `claim/requests/${id}`);
  console.log(JSON.stringify({ rotulo: 'ESS vê reivindicação atribuída pelo admin', id, http: vis.status,
    allowedActions: vis.json?.meta?.allowedActions ?? vis.json }));
  await acao(id, 'ESS', 'SUBMIT', 'ESS submete reivindicação atribuída pelo admin').catch(e => console.log('erro:', e.message));
}

main().catch(err => { console.error(err.message); process.exitCode = 1; })
  .finally(async () => {
    const fs = await import('node:fs');
    fs.writeFileSync(new URL(`./verificacao-${SUF}.json`, import.meta.url), JSON.stringify(log, null, 2));
  });
