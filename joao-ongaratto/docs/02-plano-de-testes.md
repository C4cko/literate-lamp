# Plano de testes: auditoria de historico de Timesheet

Issue de planejamento: [#57](https://github.com/masmangan/literate-lamp/issues/57).

## Jornada

**Historia:** como supervisor ou administrador, quero consultar uma Timesheet submetida e seu historico para confirmar o estado atual, a acao registrada e quem a executou.

**Pre-condicoes:** OrangeHRM OS 5.9 acessivel; usuario `Admin` da demo; existe pelo menos uma Timesheet submetida e acessivel. A lista e compartilhada e pode mudar; o teste deve ser ignorado como bloqueado quando nao houver registro elegivel, nunca substituido por uma Timesheet criada pelo teste.

**Fluxo principal:** autenticar; abrir Time > Timesheets pendentes; visualizar uma Timesheet submetida; confirmar o estado `Submitted`; conferir a acao `Submitted` no historico; consultar os endpoints de leitura e comparar estado, periodo e evento registrado com o que aparece na interface.

O fluxo e somente de leitura. Nenhum caso envia, rejeita, aprova, edita ou exclui uma Timesheet.

## Tecnicas e casos

| ID | Nivel | Tecnica | Caso | Resultado esperado |
|---|---|---|---|---|
| UT-01 | Unitario | Particao de equivalencia | Estado `SUBMITTED` acompanhado de evento `SUBMITTED` | Oraculo aceita o par consistente. |
| UT-02 | Unitario | Particao de equivalencia | Estado `APPROVED` acompanhado de evento `SUBMITTED` | Oraculo rejeita estado divergente. |
| UT-03 | Unitario | Particao de equivalencia | Estado `SUBMITTED` sem evento `SUBMITTED` | Oraculo rejeita historico incompleto ou divergente. |
| UT-04 | Unitario | Robustez/entrada ausente | Timesheet ou historico ausentes | Oraculo retorna falso sem lancar excecao. |
| IT-01 | Integracao/API | Contrato e consistencia | Ler a Timesheet pelo endpoint `GET /api/v2/time/timesheets/default` e buscar `GET /api/v2/time/timesheets/{id}/action-logs` | Respostas 200; estado `SUBMITTED`; historico contem evento `Submitted` com ator e data. |
| IT-02 | Integracao/API | Particao de equivalencia | Consultar o historico com identificador inexistente ou usuario sem acesso | API retorna erro apropriado (404 ou 403). O codigo original ja possui casos equivalentes; o caso e mantido como referencia, nao reimplementado aqui. |
| ST-01 | Sistema/UI | Caso de uso e oraculo cruzado | Abrir uma Timesheet submetida na lista de pendencias e comparar status/historico da pagina com a API | A pagina mostra `Status: Submitted` e uma linha `Submitted`, coerente com a resposta REST. |
| ST-02 | Sistema/UI | Pre-condicao negativa | Lista sem Timesheets submetidas acessiveis | Caso marcado como ignorado/bloqueado por ausencia de dado, sem criar ou modificar registros. |

## Automacao e execucao

Os casos UT-01 a UT-04 usam o runner nativo `node:test`. IT-01 e ST-01 estao automatizados com Playwright em `testes/sistema/`. A autenticacao usa a conta publica da demo por padrao; credenciais e o empregado/período do registro podem ser substituidos por `ADMIN_USER`, `ADMIN_PASS`, `TIMESHEET_EMPLOYEE_ID` e `TIMESHEET_START_DATE`. O projeto executa serialmente e nao faz chamadas HTTP de escrita.

```sh
cd joao-ongaratto/testes/sistema
npm install
npx playwright install chromium
npm run test:all
```

## Criterios

- UT-01 a UT-04 passam independentemente do servidor.
- IT-01 e ST-01 passam quando ha Timesheet submetida acessivel e os dados da interface e API coincidem.
- Sem dado elegivel, o teste de sistema e ignorado com justificativa; isso nao conta como aprovacao funcional.
- Erro de rede, autenticacao ou resposta inesperada e falha, nao defeito confirmado do OrangeHRM sem investigacao adicional.
- A execucao contra demo compartilhada e somente de leitura.

## Uso de IA

GitHub Copilot foi usado para apoio na analise, no planejamento e na implementacao inicial dos testes. O participante deve revisar os casos e executar a validacao antes de apresentar o resultado.