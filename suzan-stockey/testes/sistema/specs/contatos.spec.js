const {test: base, expect} = require('@playwright/test');
const {randomUUID} = require('node:crypto');

const labels = {
  name: 'Name', relationship: 'Relationship', homePhone: 'Home Telephone',
  mobilePhone: 'Mobile', officePhone: 'Work Telephone',
};
const phoneFields = ['homePhone', 'mobilePhone', 'officePhone'];
const digits = (n) => '1234567890'.repeat(11).slice(0, n);
function requiredEnv(name) {
  if (!process.env[name]) throw new Error(`Defina ${name} conforme o README da #28.`);
  return process.env[name];
}
function input(page, label) {
  // O componente original usa label visual sem for/id; escopo pelo grupo rotulado.
  return page.locator('.oxd-input-group').filter({
    has: page.locator('label').filter({hasText: new RegExp(`^${label}$`)}),
  }).locator('input');
}

class ContactJourney {
  constructor(page) {
    this.page = page;
    this.name = `T2-Suzan-${randomUUID().slice(0, 8)}`;
  }
  get rows() { return this.page.locator('.oxd-table-card'); }
  row(name = this.name) {
    return this.rows.filter({has: this.page.getByText(name, {exact: true})});
  }
  field(key) { return input(this.page, labels[key]); }
  async fill(values) {
    for (const [key, value] of Object.entries(values)) await this.field(key).fill(value);
  }
  async add() {
    await this.page.getByRole('button', {name: /Add$/}).first().click();
    await expect(this.page.getByRole('heading', {name: 'Save Emergency Contact', exact: true})).toBeVisible();
    await this.fill({name: this.name, relationship: 'Irma', homePhone: '51999990000'});
  }
  async save() {
    // Aguardar o formulario fechar antes de conferir a linha salva.
    const candidate = await this.field('name').inputValue();
    await this.page.getByRole('button', {name: 'Save', exact: true}).click();
    await expect(this.page.getByRole('button', {name: 'Save', exact: true})).toHaveCount(0);
    this.name = candidate;
    await expect(this.row()).toHaveCount(1);
  }
  async edit() {
    await expect(this.row()).toHaveCount(1);
    await this.row().locator('button').filter({has: this.page.locator('.bi-pencil-fill')}).click();
    await expect(this.page.getByRole('heading', {name: 'Edit Emergency Contact', exact: true})).toBeVisible();
  }
  async reload() {
    await this.page.reload();
    await expect(this.page.getByRole('heading', {name: 'Assigned Emergency Contacts', exact: true})).toBeVisible();
    await expect(this.page.locator('.oxd-loading-spinner')).toHaveCount(0);
  }
  async cancel() {
    await this.page.getByRole('button', {name: 'Cancel', exact: true}).click();
    await expect(this.page.getByRole('button', {name: 'Save', exact: true})).toHaveCount(0);
  }
  async reject(key, message) {
    await this.page.getByRole('button', {name: 'Save', exact: true}).click();
    await expect(this.field(key).locator('..').locator('..').getByText(message, {exact: true})).toBeVisible();
    await expect(this.page.getByRole('button', {name: 'Save', exact: true})).toBeVisible();
  }
  async cleanup() {
    await this.reload();
    // O pre-requisito de funcionario proprio vazio e verificado antes de qualquer gravacao.
    // Cada teste cria no maximo um contato; limpar tambem se o produto salvar um valor rejeitado.
    const count = await this.rows.count();
    expect(count, 'Limpeza espera no maximo o unico contato criado por este caso').toBeLessThanOrEqual(1);
    if (count === 1) {
      await this.rows.locator('button').filter({has: this.page.locator('.bi-trash')}).click();
      await this.page.getByRole('button', {name: /Yes, Delete$/}).click();
    }
    await expect(this.rows).toHaveCount(0);
  }
}

const test = base.extend({
  journey: async ({page}, use) => {
    const baseURL = requiredEnv('OHRM_URL');
    const number = requiredEnv('OHRM_EMP_NUMBER');
    if (!/^\d+$/.test(number)) throw new Error('OHRM_EMP_NUMBER deve ser numerico.');
    await page.goto(`${baseURL.replace(/\/$/, '')}/auth/login`);
    await page.getByPlaceholder('Username').fill(requiredEnv('OHRM_USER'));
    await page.getByPlaceholder('Password').fill(requiredEnv('OHRM_PASSWORD'));
    await page.getByRole('button', {name: 'Login', exact: true}).click();
    await expect(page).not.toHaveURL(/auth\/login/);
    // Selecao do funcionario pela interface, sem API de preparacao.
    await page.goto(`${baseURL.replace(/\/$/, '')}/pim/viewEmployeeList`);
    await input(page, 'Employee Id').fill(requiredEnv('OHRM_EMPLOYEE_ID'));
    await page.getByRole('button', {name: 'Search', exact: true}).click();
    const employee = page.locator('.oxd-table-card');
    await expect(employee).toHaveCount(1);
    await expect(employee).toContainText(process.env.OHRM_EMPLOYEE_ID);
    await employee.locator('button').filter({has: page.locator('.bi-pencil-fill')}).click();
    await expect(page).toHaveURL(new RegExp(`/empNumber/${number}(?:[/?#]|$)`));
    await page.getByRole('link', {name: 'Emergency Contacts', exact: true}).click();
    const journey = new ContactJourney(page);
    await journey.reload();
    // Funcionario sintetico dedicado e vazio: garante que limites numericos nao atinjam contatos alheios.
    await expect(journey.rows, 'Use funcionario sintetico proprio sem contatos').toHaveCount(0);
    try { await use(journey); } finally { await journey.cleanup(); }
  },
});

test('ST01 cadastro e atualizacao persistidos', async ({journey: j}) => {
  await j.add(); await j.save(); await j.reload(); await j.edit();
  await expect(j.field('name')).toHaveValue(j.name);
  await expect(j.field('relationship')).toHaveValue('Irma');
  await expect(j.field('homePhone')).toHaveValue('51999990000');
  await expect(j.field('mobilePhone')).toHaveValue('');
  await expect(j.field('officePhone')).toHaveValue('');
  await j.fill({homePhone: '5133330000'}); await j.save(); await j.reload();
  await expect(j.rows).toHaveCount(1); await j.edit();
  await expect(j.field('homePhone')).toHaveValue('5133330000');
  await expect(j.field('name')).toHaveValue(j.name); await j.cancel();
});

for (const operation of ['criacao', 'edicao']) {
  test(`ST02 cancelar ${operation}`, async ({journey: j}) => {
    await j.add();
    if (operation === 'edicao') {
      await j.save(); await j.edit(); await j.fill({name: 'NAO DEVE SALVAR'});
    }
    await j.cancel(); await j.reload();
    await expect(j.rows).toHaveCount(operation === 'criacao' ? 0 : 1);
    if (operation === 'edicao') {
      await j.edit(); await expect(j.field('name')).toHaveValue(j.name); await j.cancel();
    }
  });
}

for (const field of ['name', 'relationship']) {
  for (const [variant, value] of [['vazio', ''], ['espacos', '   ']]) {
    test(`ST03 ${field} ${variant}`, async ({journey: j}) => {
      await j.add(); await j.fill({[field]: value});
      await j.reject(field, 'Required'); await j.cancel(); await j.reload();
      await expect(j.rows).toHaveCount(0);
    });
  }
}

for (const selected of [null, ...phoneFields]) {
  test(`ST04 telefone ${selected || 'nenhum'}`, async ({journey: j}) => {
    await j.add(); await j.fill({homePhone: '', mobilePhone: '', officePhone: ''});
    if (selected) {
      await j.fill({[selected]: '51999990000'}); await j.save(); await j.reload(); await j.edit();
      for (const field of phoneFields) await expect(j.field(field)).toHaveValue(field === selected ? '51999990000' : '');
      await j.cancel();
    } else {
      await j.reject('homePhone', 'At least one phone number is required');
      await j.cancel(); await j.reload(); await expect(j.rows).toHaveCount(0);
    }
  });
}

for (const value of ['abc', '123@456', '+++++++++']) {
  test(`ST05 formato ${value}`, async ({journey: j}) => {
    await j.add(); await j.fill({homePhone: value});
    if (value === '+++++++++') {
      await j.save(); await j.reload(); await j.edit();
      await expect(j.field('homePhone')).toHaveValue(value); await j.cancel();
    } else {
      await j.reject('homePhone', 'Allows numbers and only + - / ( )');
      await j.cancel(); await j.reload(); await expect(j.rows).toHaveCount(0);
    }
  });
}

for (const field of Object.keys(labels)) {
  const max = phoneFields.includes(field) ? 30 : 100;
  for (const length of [max - 1, max, max + 1]) {
    test(`ST06 ${field} ${length} caracteres`, async ({journey: j}) => {
      await j.add(); const value = digits(length); await j.fill({[field]: value});
      if (length <= max) {
        await j.save(); await j.reload(); await j.edit();
        await expect(j.field(field)).toHaveValue(value); await j.cancel();
      } else {
        await j.reject(field, `Should not exceed ${max} characters`);
        await j.cancel(); await j.reload(); await expect(j.rows).toHaveCount(0);
      }
    });
  }
}
