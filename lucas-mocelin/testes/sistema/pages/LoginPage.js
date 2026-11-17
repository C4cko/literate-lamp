// Uso de IA: escrito com Claude (Anthropic, Claude Opus 5.5), revisado pelo autor.
// Page Object da tela de login e das telas de recuperação de senha.
// Centraliza os seletores: se a interface mudar, só este arquivo muda.
const { expect } = require('@playwright/test');

const ADMIN = {
  username: process.env.ADMIN_USER || 'Admin',
  password: process.env.ADMIN_PASS || 'admin123',
};

class LoginPage {
  /** @param {import('@playwright/test').Page} page */
  constructor(page) {
    this.page = page;
    this.username = page.locator('input[name="username"]');
    this.password = page.locator('input[name="password"]');
    this.loginButton = page.locator('button[type="submit"]');
    this.alert = page.locator('.oxd-alert-content-text');
    this.fieldErrors = page.locator('.oxd-input-field-error-message');
    this.forgotLink = page.locator('.orangehrm-login-forgot-header');
  }

  async goto() {
    await this.page.goto('auth/login');
    await expect(this.username).toBeVisible();
  }

  async login(username, password) {
    if (username !== undefined) await this.username.fill(username);
    if (password !== undefined) await this.password.fill(password);
    await this.loginButton.click();
  }

  /** Erro exibido abaixo de um campo (Username / Password). */
  fieldError(label) {
    return this.page
      .locator('.oxd-input-group')
      .filter({ has: this.page.locator('label', { hasText: label }) })
      .locator('.oxd-input-field-error-message');
  }
}

class DashboardPage {
  /** @param {import('@playwright/test').Page} page */
  constructor(page) {
    this.page = page;
    this.header = page.locator('.oxd-topbar-header-breadcrumb');
    this.userMenu = page.locator('.oxd-userdropdown-tab');
    this.userName = page.locator('.oxd-userdropdown-name');
  }

  async expectLoaded() {
    await expect(this.page).toHaveURL(/dashboard\/index/);
    await expect(this.header).toContainText('Dashboard');
    await expect(this.userName).toBeVisible();
  }

  async logout() {
    await this.userMenu.click();
    await this.page.getByRole('menuitem', { name: 'Logout' }).click();
    await expect(this.page).toHaveURL(/auth\/login/);
  }
}

class ForgotPasswordPage {
  /** @param {import('@playwright/test').Page} page */
  constructor(page) {
    this.page = page;
    this.username = page.locator('input[name="username"]');
    this.submit = page.locator('button[type="submit"]');
    this.cancel = page.getByRole('button', { name: 'Cancel' });
    this.title = page.locator('h6');
  }
}

module.exports = { LoginPage, DashboardPage, ForgotPasswordPage, ADMIN };
