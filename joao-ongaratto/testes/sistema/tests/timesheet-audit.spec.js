const {test, expect} = require('@playwright/test');

const username = process.env.ADMIN_USER || 'Admin';
const password = process.env.ADMIN_PASS || 'admin123';

async function openSubmittedTimesheet(page) {
  await page.goto('auth/login');
  await page.getByRole('textbox', {name: 'Username'}).fill(username);
  await page.getByRole('textbox', {name: 'Password'}).fill(password);
  await page.getByRole('button', {name: 'Login'}).click();
  await expect(page).toHaveURL(/dashboard/);

  await page.goto('time/viewEmployeeTimesheet');
  await expect(page.getByRole('heading', {name: 'Timesheets Pending Action'})).toBeVisible();

  const viewButtons = page.getByRole('table').getByRole('button', {name: 'View'});
  if ((await viewButtons.count()) === 0) {
    test.skip(true, 'A demo nao tem Timesheet pendente acessivel neste momento.');
  }

  await viewButtons.first().click();
  await expect(page.getByText(/^Status:/)).toContainText('Submitted');

  const currentUrl = new URL(page.url());
  const employeeMatch = currentUrl.pathname.match(/employeeId\/(\d+)/);
  expect(employeeMatch, 'A URL da Timesheet deve conter o identificador do funcionario.').not.toBeNull();

  return {
    employeeNumber: Number(employeeMatch[1]),
    startDate: currentUrl.searchParams.get('startDate'),
  };
}

test('IT-01 status e historico REST correspondem a Timesheet submetida', async ({page}) => {
  const {employeeNumber, startDate} = await openSubmittedTimesheet(page);
  expect(startDate, 'A URL da Timesheet deve informar a data inicial.').toBeTruthy();

  const response = await page.evaluate(async ({employeeNumber, startDate}) => {
    const timesheetResponse = await fetch(
      `/api/v2/time/timesheets/default?date=${encodeURIComponent(startDate)}&empNumber=${employeeNumber}`,
    );
    const timesheetBody = await timesheetResponse.json();
    if (!timesheetResponse.ok) {
      return {status: timesheetResponse.status, timesheetBody};
    }

    const timesheetId = timesheetBody.data?.id;
    const historyResponse = await fetch(
      `/api/v2/time/timesheets/${timesheetId}/action-logs?sortField=date&sortOrder=ASC`,
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
  expect(response.timesheet.status.id).toBe('SUBMITTED');
  expect(response.historyStatus).toBe(200);
  expect(response.history.some((entry) => entry.action?.label === 'Submitted')).toBeTruthy();
});

test('ST-01 interface exibe status e acao de submissao no historico', async ({page}) => {
  await openSubmittedTimesheet(page);
  await expect(page.getByRole('heading', {name: 'Actions Performed on the Timesheet'})).toBeVisible();
  await expect(page.getByRole('table').nth(1).getByText('Submitted', {exact: true})).toBeVisible();
});