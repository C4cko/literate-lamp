/**
 * Cliente HTTP da API v2 do OrangeHRM no servidor de demonstração.
 * A sessão é a do formulário web: o cookie devolvido por /auth/validate
 * autoriza /api/v2. Credenciais públicas do demo, documentadas pela OrangeHRM.
 */

export const BASE =
  process.env.ORANGEHRM_BASE ?? "https://opensource-demo.orangehrmlive.com";

export const USUARIO = process.env.ORANGEHRM_USER ?? "Admin";
export const SENHA = process.env.ORANGEHRM_PASSWORD ?? "admin123";

const PREFIXO = "/web/index.php";

export class SessaoOrangeHrm {
  constructor(base = BASE) {
    this.base = base.replace(/\/$/, "");
    this.cookies = new Map();
  }

  cabecalhoCookie() {
    return [...this.cookies.entries()].map(([k, v]) => `${k}=${v}`).join("; ");
  }

  guardarCookies(resposta) {
    const lista =
      typeof resposta.headers.getSetCookie === "function"
        ? resposta.headers.getSetCookie()
        : [];
    for (const item of lista) {
      const par = item.split(";", 1)[0];
      const eq = par.indexOf("=");
      if (eq > 0) this.cookies.set(par.slice(0, eq), par.slice(eq + 1));
    }
  }

  async requisicao(caminho, { method = "GET", body, json } = {}) {
    const headers = { Accept: "application/json" };
    const cookie = this.cabecalhoCookie();
    if (cookie) headers.Cookie = cookie;
    let payload = body;
    if (json !== undefined) {
      headers["Content-Type"] = "application/json";
      payload = JSON.stringify(json);
    } else if (typeof body === "string") {
      headers["Content-Type"] = "application/x-www-form-urlencoded";
    }
    const resposta = await fetch(this.base + caminho, {
      method,
      headers,
      body: payload,
      redirect: "manual",
    });
    this.guardarCookies(resposta);
    return resposta;
  }

  async entrar(usuario = USUARIO, senha = SENHA) {
    const pagina = await this.requisicao(`${PREFIXO}/auth/login`);
    const html = await pagina.text();
    const token = html.match(/:token="&quot;([^&]+)&quot;"/)?.[1];
    if (!token) {
      throw new Error(
        `Token de login ausente (HTTP ${pagina.status}). O HTML da tela mudou?`,
      );
    }
    const formulario = new URLSearchParams({
      _token: token,
      username: usuario,
      password: senha,
    });
    const validacao = await this.requisicao(`${PREFIXO}/auth/validate`, {
      method: "POST",
      body: formulario.toString(),
    });
    if (![302, 303].includes(validacao.status)) {
      throw new Error(`Login recusado (HTTP ${validacao.status}).`);
    }
    return this;
  }

  async api(caminho, opcoes = {}) {
    const resposta = await this.requisicao(`${PREFIXO}${caminho}`, opcoes);
    const texto = await resposta.text();
    let corpo = null;
    if (texto) {
      try {
        corpo = JSON.parse(texto);
      } catch {
        corpo = texto;
      }
    }
    return { status: resposta.status, corpo };
  }
}
