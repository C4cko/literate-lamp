// Uso de IA: escrito com Claude (Anthropic, Claude Opus 5.5), revisado pelo autor.
// Testes de sistema - requisitos não funcionais de segurança da autenticação.
// Técnica: teste baseado em riscos (OWASP ASVS V2/V3 como oráculo de referência).
const { test, expect } = require('@playwright/test');
const { LoginPage, DashboardPage, ADMIN } = require('../pages/LoginPage');

test.describe('Segurança do login', () => {
  test('ST-18 POST de login sem token CSRF é rejeitado', async ({ page }) => {
    const resp = await page.request.post('auth/validate', {
      form: { username: ADMIN.username, password: ADMIN.password },
      maxRedirects: 0,
    });
    expect(resp.status()).toBe(302);
    expect(resp.headers()['location']).toMatch(/auth\/login/);

    // a sessão do navegador continua anônima
    await page.goto('dashboard/index');
    await expect(page).toHaveURL(/auth\/login/);
  });

  test('ST-19 [observação] tentativas inválidas seguidas não bloqueiam a conta', async ({ page }) => {
    // Este caso DOCUMENTA o comportamento atual: o OrangeHRM não possui bloqueio
    // de conta nem limitação de tentativas (rate limiting). Ver relatório, OBS-01.
    // São 11 envios de formulário; na demo pública cada um leva ~5 s, por isso o limite maior.
    test.setTimeout(180_000);
    const login = new LoginPage(page);
    for (let i = 1; i <= 10; i++) {
      // recarrega a tela a cada tentativa: preencher enquanto a página ainda
      // recarrega após o erro anterior apagava o usuário (falha vista na demo)
      await login.goto();
      await login.login(ADMIN.username, `Errada@${i}`);
      await expect(login.alert).toHaveText('Invalid credentials');
    }
    await login.goto();
    await login.login(ADMIN.username, ADMIN.password);
    await new DashboardPage(page).expectLoaded();
  });
});