// Uso de IA: escrito com Claude (Anthropic, Claude Opus 5.5), revisado pelo autor.
// Testes de sistema - Jornada "Autenticação", fluxo alternativo "Esqueci minha senha".
// Técnicas: casos de uso (fluxo alternativo), classes de equivalência
// (usuário existente / inexistente / vazio) e teste ponta a ponta com e-mail real.
const fs = require('fs');
const path = require('path');
const { test, expect } = require('@playwright/test');
const { LoginPage, DashboardPage, ForgotPasswordPage, ADMIN } = require('../pages/LoginPage');

const MAILBOX_DIR = process.env.MAILBOX_DIR; // Maildir do SMTP local (ver README)

/** Lê o link de redefinição do e-mail mais recente da caixa local. */
function lastResetLink(since) {
  const dir = path.join(MAILBOX_DIR, 'new');
  const files = fs
    .readdirSync(dir)
    .map((f) => path.join(dir, f))
    .filter((f) => fs.statSync(f).mtimeMs >= since)
    .sort((a, b) => fs.statSync(b).mtimeMs - fs.statSync(a).mtimeMs);
  if (!files.length) return null;
  const raw = fs.readFileSync(files[0], 'utf8').replace(/=\r?\n/g, ''); // quoted-printable
  const m = raw.match(/https?:\/\/\S+?resetCode\/[^<\s]+/);
  return m ? m[0] : null;
}

test.describe('Recuperação de senha', () => {
  test('ST-14 link "Forgot your password?" abre a tela e "Cancel" volta ao login', async ({ page }) => {
    const login = new LoginPage(page);
    await login.goto();
    await login.forgotLink.click();
    await expect(page).toHaveURL(/auth\/requestPasswordResetCode/);

    const forgot = new ForgotPasswordPage(page);
    await expect(forgot.title).toHaveText('Reset Password');
    await forgot.cancel.click();
    await expect(page).toHaveURL(/auth\/login/);
  });

  test('ST-15 usuário em branco na recuperação: "Required"', async ({ page }) => {
    await page.goto('auth/requestPasswordResetCode');
    const forgot = new ForgotPasswordPage(page);
    await forgot.submit.click();
    await expect(page.locator('.oxd-input-field-error-message')).toHaveText('Required');
    await expect(page).toHaveURL(/auth\/requestPasswordResetCode/);
  });

  test('ST-16 usuário inexistente recebe a mesma resposta de sucesso (sem enumeração de usuários)', async ({ page }) => {
    await page.goto('auth/requestPasswordResetCode');
    const forgot = new ForgotPasswordPage(page);
    await forgot.username.fill('usuario_inexistente_t2');
    await forgot.submit.click();
    await expect(page).toHaveURL(/auth\/sendPasswordReset$/);
    await expect(page.locator('h6')).toHaveText('Reset Password link sent successfully');
  });

  test('ST-17 redefinição ponta a ponta: e-mail -> nova senha -> login -> senha antiga recusada', async ({ page }) => {
    test.skip(!MAILBOX_DIR, 'Requer instalação local com SMTP de teste (variável MAILBOX_DIR)');
    const NEW_PASS = 'NovaSenha@T2#2026';
    const started = Date.now() - 1000;

    // 1. solicita o link
    await page.goto('auth/requestPasswordResetCode');
    const forgot = new ForgotPasswordPage(page);
    await forgot.username.fill(ADMIN.username);
    await forgot.submit.click();
    await expect(page.locator('h6')).toHaveText('Reset Password link sent successfully');

    // 2. lê o e-mail
    let link = null;
    await expect.poll(() => (link = lastResetLink(started)), { timeout: 15_000 }).not.toBeNull();

    // 3. abre o link e define a nova senha
    await page.goto(link);
    await expect(page.locator('input[name="username"]')).toHaveValue(ADMIN.username);
    await expect(page.locator('input[name="username"]')).toHaveAttribute('readonly', '');
    await page.locator('input[name="password"]').fill(NEW_PASS);
    await page.locator('input[name="confirmPassword"]').fill(NEW_PASS);
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(page).toHaveURL(/auth\/login/);

    // 4. o link é de uso único
    await page.goto(link);
    await expect(page.locator('input[name="confirmPassword"]')).toHaveCount(0);

    // 5. senha antiga recusada, nova aceita
    const login = new LoginPage(page);
    await login.goto();
    await login.login(ADMIN.username, ADMIN.password);
    await expect(login.alert).toHaveText('Invalid credentials');
    await login.login(ADMIN.username, NEW_PASS);
    await new DashboardPage(page).expectLoaded();

    // 6. restaura a senha original pela tela "Change Password"
    await page.goto('pim/updatePassword');
    const pw = page.locator('form input[type="password"]');
    await pw.nth(0).fill(NEW_PASS);
    await pw.nth(1).fill(ADMIN.password);
    await pw.nth(2).fill(ADMIN.password);
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(page.locator('.oxd-toast')).toContainText('Successfully');
  });
});
