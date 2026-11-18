const {defineConfig} = require('@playwright/test');
module.exports = defineConfig({
  testDir: './specs',
  workers: 1,
  fullyParallel: false,
  retries: 0,
  timeout: 60000,
  expect: {timeout: 10000},
  outputDir: './test-results',
  reporter: [['list'], ['html', {open: 'never'}]],
  use: {
    baseURL: process.env.OHRM_URL,
    viewport: {width: 1440, height: 1000},
    screenshot: 'only-on-failure',
  },
});
