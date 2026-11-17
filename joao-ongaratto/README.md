# Joao Ongaratto: auditoria de Timesheets no OrangeHRM

Contribuicao individual para o Trabalho 2 de Verificacao e Validacao de Software.

## Escopo

A jornada verifica, sem alterar dados, a consistencia entre o estado de uma Timesheet submetida, o historico exibido pela interface e o endpoint REST de historico de acoes. O plano esta registrado na Issue #57. A mudanca de escopo foi identificada durante a analise do codigo: o OrangeHRM ja possui testes de integracao para aprovar, rejeitar e resetar Timesheets.

## Documentos

- [Analise critica](docs/01-analise-critica.md)
- [Plano de testes](docs/02-plano-de-testes.md)
- [Testes automatizados de sistema e integracao](testes/sistema/)
- [Resultado da execucao](docs/03-resultados.md)

Versao analisada: OrangeHRM OS 5.9, commit `56e23b3b09e7af29317a0943523b825843fff527`.

## Uso de IA

GitHub Copilot foi usado como assistente para pesquisa, analise, planejamento, programacao e redacao. O autor deve revisar o material e confirmar os resultados antes da entrega.