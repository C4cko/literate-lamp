// Uso de IA: escrito com Claude (Anthropic, Claude Opus 5.5), revisado pelo autor.
// Testes de sistema - Jornada "Autenticação", passos 4-5: usar a sessão e sair.
// Técnica: teste de transição de estados (Anônimo -> Autenticado -> Anônimo).
const { test, expect } = require('@playwright/test');
const { LoginPage, DashboardPage, ADMIN } = require('../pages/LoginPage');

test.describe('Sessão', () => {
  test('ST-11 página protegida sem sessão redireciona para o login', async ({ page }) => {
    await page.goto('pim/viewEmployeeList');
    await expect(page).toHaveURL(/auth\/login/);
    await expect(new LoginPage(page).username).toBeVisible();
  });

  test('ST-12 logout encerra a sessão (voltar no navegador e API devolvem ao login / 401)', async ({ page }) => {
    const login = new LoginPage(page);
    const dash = new DashboardPage(page);
    await login.goto();
    await login.login(ADMIN.username, ADMIN.password);
    await dash.expectLoaded();

    await page.goto('pim/viewEmployeeList');
    await expect(page.locator('.oxd-table-card').first()).toBeVisible();

    await dash.logout();

    await page.goBack();
    await expect(page).toHaveURL(/auth\/login/);

    const api = await page.request.get('api/v2/pim/employees?limit=1');
    expect(api.status()).toBe(401);
  });

  test('ST-13 sessão permanece ativa após recarregar a página', async ({ page }) => {
    const login = new LoginPage(page);
    await login.goto();
    await login.login(ADMIN.username, ADMIN.password);
    await new DashboardPage(page).expectLoaded();
    await page.reload();
    await new DashboardPage(page).expectLoaded();
  });
});
