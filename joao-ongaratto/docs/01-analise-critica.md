# Analise critica dos testes de Timesheets

Issue de planejamento: [#57](https://github.com/masmangan/literate-lamp/issues/57).

## Versao examinada

OrangeHRM OS 5.9, commit `56e23b3b09e7af29317a0943523b825843fff527`, obtido em [orangehrm/orangehrm](https://github.com/orangehrm/orangehrm).

## Testes do codigo original

Os testes do plugin ficam em `src/plugins/orangehrmTimePlugin/test/` e usam PHPUnit.

| Arquivo | Escopo observado | Nivel |
|---|---|---|
| `Entity/TimesheetTest.php`, `TimesheetItemTest.php` | Atributos e comportamento das entidades | Unitario |
| `Api/EmployeeTimesheetAPITest.php` e `fixtures/testcases/EmployeeTimesheetAPITestCases.yaml` | Criacao/consulta e acoes de workflow, inclusive `APPROVE`, `REJECT`, `RESET` e autorizacao | Integracao |
| `Api/EmployeeTimesheetItemAPITest.php` e `fixtures/testcases/EmployeeTimesheetItemAPITestCases.yaml` | Consulta, edicao e exclusao de entradas; inclui bloqueio de edicao de Timesheet aprovada | Integracao |
| `Api/TimesheetActionLogAPITest.php` e `fixtures/testcases/TimesheetActionLogTestCase.yaml` | Consulta de historico, acesso proprio/administrador/supervisor, registro inexistente e acesso negado | Integracao |

Os casos parametrizados carregam fixtures e verificam respostas esperadas. Isso favorece repetibilidade e permite cobrir papeis diferentes sem depender da interface publica. A API tambem verifica restricoes de acesso ao historico.

## Limitacoes observadas

- Nas classes e fixtures consultadas, nao encontrei uma verificacao ponta a ponta que compare o estado retornado pela API com o estado e a linha de historico mostrados na interface.
- As acoes isoladas de aprovar/rejeitar/resetar ja estao cobertas; repeti-las nao seria uma contribuicao distinta.
- Nao encontrei testes de sistema do plugin Time nas pastas analisadas do OrangeHRM nem na contribuicao atual de Attendance da turma.
- Testes de entidade que verificam somente getters e setters oferecem pouco poder para encontrar defeitos de regra de negocio.
- Os testes originais exigem o ambiente PHP e banco de testes do projeto; a demo publica nao e uma substituta desse ambiente.

## Material de terceiros

A analise de Attendance de Eduardo Hoffmann cita o projeto [bg-playground/TestingDemo-02](https://github.com/bg-playground/TestingDemo-02): a pasta manual inclui casos de Timesheet para criar, submeter e aprovar. O material serve como referencia de casos manuais, mas nao cobre, nos trechos descritos, a comparacao do historico da interface com a resposta REST. Essa observacao e limitada ao material identificado e nao pretende cobrir todos os projetos publicos existentes.

## Decisao de escopo

A proposta inicial da Issue #57 mencionava rejeicao, correcao e reenvio. A inspecao mostrou que rejeicao e outras acoes ja possuem cobertura de integracao, e executa-las na demo compartilhada alteraria dados de terceiros. A contribuicao foi restringida a uma verificacao somente de leitura: uma Timesheet submetida deve exibir o estado `Submitted` e uma acao `Submitted` no historico, coerentes com a resposta dos endpoints. A demo foi explorada em 17/11/2026; havia uma Timesheet acessivel submetida e o historico mostrava uma submissao. Esse dado e observacao pontual, nao fixture estavel.

## Uso de IA

GitHub Copilot foi usado para localizar os arquivos relevantes, organizar a analise e redigir este documento. O participante deve revisar as conclusoes e confirmar as referencias antes da entrega.