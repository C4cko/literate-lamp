const {test} = require('node:test');
const assert = require('node:assert/strict');
const {hasSubmittedAuditEvent} = require('../../src/timesheet-audit');

test('UT-01 aceita status submetido com evento de submissao', () => {
  const timesheet = {status: {id: 'SUBMITTED'}};
  const history = [{action: {name: 'SUBMITTED'}}];

  assert.equal(hasSubmittedAuditEvent(timesheet, history), true);
});

test('UT-02 rejeita status diferente de submetido', () => {
  const timesheet = {status: {id: 'APPROVED'}};
  const history = [{action: {name: 'SUBMITTED'}}];

  assert.equal(hasSubmittedAuditEvent(timesheet, history), false);
});

test('UT-03 rejeita historico sem evento de submissao', () => {
  const timesheet = {status: {id: 'SUBMITTED'}};
  const history = [{action: {name: 'APPROVED'}}];

  assert.equal(hasSubmittedAuditEvent(timesheet, history), false);
});

test('UT-04 lida com dados ausentes sem lancar excecao', () => {
  assert.equal(hasSubmittedAuditEvent(null, null), false);
  assert.equal(hasSubmittedAuditEvent({status: {id: 'SUBMITTED'}}, null), false);
});