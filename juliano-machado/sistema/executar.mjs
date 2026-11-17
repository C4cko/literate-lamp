// Reproduz as gravações do Chrome Recorder (pasta gravacoes/) com o @puppeteer/replay.
// O texto dos posts usa {{ID}}, trocado aqui por um número único a cada execução,
// assim o teste só mexe no post que ele mesmo criou.
//
// Uso: npm test            (todas as gravações)
//      npm run defeito     (gravações que mostram defeito, falham de propósito)
//      node executar.mjs ts01-jornada-completa.json
// Variáveis: CHROME=caminho do Chrome, HEADLESS=false para ver o navegador,
//            LENTO=150 para deixar as ações mais devagar.

import fs from 'node:fs';
import path from 'node:path';
import puppeteer from 'puppeteer';
import { createRunner, PuppeteerRunnerExtension } from '@puppeteer/replay';

// Captura só o card do post que o teste criou (achado pelo texto único), para a evidência ficar legível.
// Se o card não estiver na tela, captura a página inteira.
async function capturar(page, textoDoPost, arquivo) {
  const card = await page.$(
    `xpath///div[contains(concat(' ', normalize-space(@class), ' '), ' orangehrm-buzz ') and .//p[normalize-space()='${textoDoPost}']]`,
  );
  if (card) {
    await card.screenshot({ path: arquivo });
  } else {
    await page.screenshot({ path: arquivo });
  }
}

// Tira a captura sempre antes de abrir o menu do post. Fica a última,
// que é o estado logo antes da exclusão (no TS-03, já depois do "No, Cancel").
class ComEvidencia extends PuppeteerRunnerExtension {
  constructor(browser, page, opcoes, textoDoPost, arquivoImagem) {
    super(browser, page, opcoes);
    this.textoDoPost = textoDoPost;
    this.arquivoImagem = arquivoImagem;
  }

  async beforeEachStep(step, flow) {
    if (step.type === 'click' && JSON.stringify(step.selectors).includes('bi-three-dots')) {
      await new Promise((r) => setTimeout(r, 500));
      await capturar(this.page, this.textoDoPost, this.arquivoImagem);
    }
    await super.beforeEachStep?.(step, flow);
  }

  // Passo especial (não existe no Chrome Recorder): envia um comentário direto pela API,
  // usando a sessão do navegador, para depois conferir na tela o que aparece (TS-05, defeito D1).
  async runStep(step, flow) {
    if (step.type === 'customStep' && step.name === 'comentarPelaApi') {
      const { textoDoPost, comentario } = step.parameters;
      const status = await this.page.evaluate(async (base, textoDoPost, comentario) => {
        const r = await fetch(`${base}/api/v2/buzz/feed?limit=50&offset=0&sortOrder=DESC&sortField=share.createdAtUtc`);
        const share = (await r.json()).data.find((s) => s.text === textoDoPost);
        if (!share) return 'post não encontrado';
        const c = await fetch(`${base}/api/v2/buzz/shares/${share.id}/comments`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ text: comentario }),
        });
        return c.status;
      }, BASE, textoDoPost, comentario);
      // 422 é o esperado (comentário recusado); 200 é o defeito D1. Os dois seguem para a conferência na tela.
      if (status !== 200 && status !== 422) {
        throw new Error(`resposta inesperada da API: ${status}`);
      }
      return;
    }
    return super.runStep(step, flow);
  }
}

const DEFEITO = process.argv.includes('--defeito');
const PASTA = path.join(import.meta.dirname, 'gravacoes', DEFEITO ? 'defeito' : '');
const RESULTADOS = path.join(import.meta.dirname, '..', 'resultados');
const CHROME = process.env.CHROME || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const BASE = 'https://opensource-demo.orangehrmlive.com/web/index.php';

const argumentos = process.argv.slice(2).filter((a) => a !== '--defeito');
const arquivos = argumentos.length
  ? argumentos
  : fs.readdirSync(PASTA).filter((f) => f.endsWith('.json')).sort();

fs.mkdirSync(RESULTADOS, { recursive: true });

const browser = await puppeteer.launch({
  executablePath: fs.existsSync(CHROME) ? CHROME : undefined,
  headless: process.env.HEADLESS !== 'false',
  slowMo: Number(process.env.LENTO || 0), // pausa em ms entre as ações, para acompanhar na tela
  defaultViewport: null,
});

let falhas = 0;
for (const arquivo of arquivos) {
  const id = Date.now();
  const texto = fs.readFileSync(path.join(PASTA, arquivo), 'utf8').replaceAll('{{ID}}', String(id));
  const gravacao = JSON.parse(texto);
  const textoDoPost = (texto.match(/JM-TS\d\d-\d+/) || [''])[0];
  // cada gravação em um contexto novo (sem cookies da anterior), pois todas começam pelo login
  const contexto = await browser.createBrowserContext();
  const page = await contexto.newPage();
  const inicio = Date.now();
  try {
    const runner = await createRunner(gravacao, new ComEvidencia(browser, page, { timeout: 30000 }, textoDoPost, path.join(RESULTADOS, arquivo.replace('.json', '.png'))));
    await runner.run();
    console.log(`PASSOU  ${gravacao.title}  (${((Date.now() - inicio) / 1000).toFixed(1)} s)`);
  } catch (erro) {
    falhas++;
    console.log(`FALHOU  ${gravacao.title}\n        ${erro.message.split('\n')[0]}`);
    await capturar(page, textoDoPost, path.join(RESULTADOS, arquivo.replace('.json', '-falha.png'))).catch(() => {});
  } finally {
    await limpar(page, id);
    await contexto.close();
  }
}

await browser.close();
console.log(`\n${arquivos.length - falhas} de ${arquivos.length} gravações passaram`);
process.exit(falhas ? 1 : 0);

// Se o teste parar no meio, o post dele fica no feed. Aqui ele é apagado pela API,
// usando a sessão do próprio navegador e só se o texto tiver o ID desta execução.
async function limpar(page, id) {
  await page.evaluate(async (base, id) => {
    const r = await fetch(`${base}/api/v2/buzz/feed?limit=50&offset=0&sortOrder=DESC&sortField=share.createdAtUtc`);
    if (!r.ok) return;
    for (const s of (await r.json()).data) {
      if ((s.text || '').startsWith('JM-TS') && s.text.endsWith(`-${id}`)) {
        await fetch(`${base}/api/v2/buzz/shares/${s.id}`, { method: 'DELETE' });
      }
    }
  }, BASE, id).catch(() => {});
}
