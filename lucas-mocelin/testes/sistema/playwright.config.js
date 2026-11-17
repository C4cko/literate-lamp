// @ts-check
const { defineConfig, devices } = require('@playwright/test');

/**
 * Variáveis de ambiente:
 *   ORANGEHRM_URL  URL base até /web/index.php
 *                  (padrão: servidor de demonstração público)
 *   ADMIN_USER     usuário administrador (padrão: Admin)
 *   ADMIN_PASS     senha do administrador (padrão: admin123, a da demo)
 *   MAILBOX_DIR    pasta Maildir onde um SMTP local grava os e-mails.
 *                  Só existe na instalação local; sem ela, o caso
 *                  ST-15 (recuperação de senha ponta a ponta) é ignorado.
 *   CHROME_PATH    (opcional) executável do Chrome/Chromium a usar.
 */
module.exports = defineConfig({
  testDir: './tests',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1, // os casos compartilham o usuário Admin; execução serial evita interferência
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }], ['junit', { outputFile: 'results/junit.xml' }]],
  use: {
    baseURL: (process.env.ORANGEHRM_URL || 'https://opensource-demo.orangehrmlive.com/web/index.php') + '/',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off',
    locale: 'en-US',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        launchOptions: process.env.CHROME_PATH ? { executablePath: process.env.CHROME_PATH } : {},
      },
    },
  ],
});
