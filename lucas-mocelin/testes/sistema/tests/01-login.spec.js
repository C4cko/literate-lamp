// Uso de IA: escrito com Claude (Anthropic, Claude Opus 5.5), revisado pelo autor.
// Testes de sistema (caixa-preta, E2E) - Jornada "Autenticação"
// Passos 1-3 da jornada: abrir a tela, informar credenciais, acessar o painel.
// Técnicas: classes de equivalência (credenciais), valores-limite (campos
// vazios / espaços), casos de uso (fluxo principal e alternativos).
const { test, expect } = require('@playwright/test');
const { LoginPage, DashboardPage, ADMIN } = require('../pages/LoginPage');

test.describe('Login', () => {
  /** @type {LoginPage} */ let login;

  test.beforeEach(async ({ page }) => {
    login = new LoginPage(page);
    await login.goto();
  });

  test('ST-01 credenciais válidas levam ao Dashboard', async ({ page }) => {
    await login.login(ADMIN.username, ADMIN.password);
    await new DashboardPage(page).expectLoaded();
  });

  test('ST-02 senha incorreta exibe "Invalid credentials" e permanece no login', async ({ page }) => {
    await login.login(ADMIN.username, 'SenhaErrada@123');
    await expect(login.alert).toHaveText('Invalid credentials');
    await expect(page).toHaveURL(/auth\/login/);
  });

  test('ST-03 usuário inexistente exibe a MESMA mensagem (não revela se o usuário existe)', async ({ page }) => {
    await login.login('usuario_inexistente_t2', 'SenhaErrada@123');
    await expect(login.alert).toHaveText('Invalid credentials');
    await expect(page).toHaveURL(/auth\/login/);
  });

  test('ST-04 usuário e senha vazios: ambos marcados como "Required"', async ({ page }) => {
    await login.login();
    await expect(login.fieldError('Username')).toHaveText('Required');
    await expect(login.fieldError('Password')).toHaveText('Required');
    await expect(page).toHaveURL(/auth\/login/);
  });

  test('ST-05 apenas usuário preenchido: senha "Required"', async () => {
    await login.login(ADMIN.username);
    await expect(login.fieldError('Password')).toHaveText('Required');
    await expect(login.fieldError('Username')).toHaveCount(0);
  });

  test('ST-06 apenas senha preenchida: usuário "Required"', async () => {
    await login.login(undefined, ADMIN.password);
    await expect(login.fieldError('Username')).toHaveText('Required');
    await expect(login.fieldError('Password')).toHaveCount(0);
  });

  test('ST-07 a senha digitada fica mascarada', async () => {
    await expect(login.password).toHaveAttribute('type', 'password');
  });

  test('ST-08 tecla Enter envia o formulário', async ({ page }) => {
    await login.username.fill(ADMIN.username);
    await login.password.fill(ADMIN.password);
    await login.password.press('Enter');
    await new DashboardPage(page).expectLoaded();
  });

  test('ST-09 espaços antes/depois do usuário não são removidos (login recusado)', async ({ page }) => {
    await login.login(` ${ADMIN.username} `, ADMIN.password);
    await expect(login.alert).toHaveText('Invalid credentials');
    await expect(page).toHaveURL(/auth\/login/);
  });

  test('ST-10 nome de usuário não diferencia maiúsculas/minúsculas', async ({ page }) => {
    await login.login(ADMIN.username.toLowerCase(), ADMIN.password);
    await new DashboardPage(page).expectLoaded();
  });
});
