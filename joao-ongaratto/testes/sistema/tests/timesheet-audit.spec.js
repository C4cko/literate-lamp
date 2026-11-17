const {test, expect} = require('@playwright/test');
const {hasSubmittedAuditEvent} = require('../src/timesheet-audit');

const username = process.env.ADMIN_USER || 'Admin';
const password = process.env.ADMIN_PASS || 'admin123';
const employeeNumber = Number(process.env.TIMESHEET_EMPLOYEE_ID || '7');
const startDate = process.env.TIMESHEET_START_DATE || '2023-01-16';

async function openSubmittedTimesheet(page) {
  await page.goto('auth/login');
  await page.getByRole('textbox', {name: 'Username'}).fill(username);
  await page.getByRole('textbox', {name: 'Password'}).fill(password);
  await page.getByRole('button', {name: 'Login'}).click();
  await expect(page).toHaveURL(/dashboard/);

  await page.goto(`time/viewTimesheet/employeeId/${employeeNumber}?startDate=${startDate}`);
  await expect(page.getByRole('heading', {name: /Timesheet for/})).toBeVisible();
  await expect(page.getByText(/^Status:/)).toContainText('Submitted');
}

test('IT-01 status e historico REST correspondem a Timesheet submetida', async ({page}) => {
  await openSubmittedTimesheet(page);

  const response = await page.evaluate(async ({employeeNumber, startDate}) => {
    const apiBase = window.appGlobal.baseUrl;
    const timesheetResponse = await fetch(
      `${apiBase}/api/v2/time/timesheets/default?date=${encodeURIComponent(startDate)}&empNumber=${employeeNumber}`,
    );
    const timesheetBody = await timesheetResponse.json();
    if (!timesheetResponse.ok) {
      return {status: timesheetResponse.status, timesheetBody};
    }

    const timesheetId = timesheetBody.data?.id;
    const historyResponse = await fetch(
      `${apiBase}/api/v2/time/timesheets/${timesheetId}/action-logs`,
    );
    const historyBody = await historyResponse.json();

    return {
      status: timesheetResponse.status,
      timesheet: timesheetBody.data,
      historyStatus: historyResponse.status,
      history: historyBody.data,
    };
  }, {employeeNumber, startDate});

  expect(response.status).toBe(200);
  expect(response.historyStatus).toBe(200);
  expect(hasSubmittedAuditEvent(response.timesheet, response.history)).toBe(true);
});

test('ST-01 interface exibe status e acao de submissao no historico', async ({page}) => {
  await openSubmittedTimesheet(page);
  await expect(page.getByRole('heading', {name: 'Actions Performed on the Timesheet'})).toBeVisible();
  await expect(page.getByRole('table').nth(1).getByText('Submitted', {exact: true})).toBeVisible();
});