const {defineConfig, devices} = require('@playwright/test');

module.exports = defineConfig({
  testDir: './tests',
  testMatch: '**/*.spec.js',
  timeout: 60000,
  expect: {timeout: 10000},
  workers: 1,
  reporter: 'list',
  use: {
    baseURL: (process.env.ORANGEHRM_URL || 'https://opensource-demo.orangehrmlive.com/web/index.php') + '/',
    locale: 'en-US',
    trace: 'retain-on-failure',
  },
  projects: [{name: 'chromium', use: {...devices['Desktop Chrome']}}],
});