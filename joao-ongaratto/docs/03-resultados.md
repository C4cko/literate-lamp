# Relatorio de execucao

Data: 17/11/2026.

## Ambiente

- OrangeHRM OS 5.9, demo publica.
- Node.js 24.14.0, npm 11.9.0, Playwright 1.49+ e Chromium instalado pelo Playwright.
- Os testes usam apenas navegacao e requisicoes HTTP GET.

## Resultado automatizado

Comando executado em `testes/sistema/`:

```sh
npm install
npx playwright install chromium
npm run test:all
```

Resultado unitario: 4 casos (UT-01 a UT-04), todos passaram.

Resultado de integracao e sistema: 2 casos. IT-01 e ST-01 passaram usando a Timesheet ja existente do empregado 7, periodo iniciado em 2023-01-16. Nenhuma escrita foi feita na demo.

## Exploracao manual

Durante a exploracao e a execucao automatizada, foi possivel consultar uma Timesheet submetida na demo. A tela mostrou `Status: Submitted`; os endpoints GET retornaram estado `SUBMITTED` e evento `SUBMITTED`. A verificacao foi somente de leitura. O teste permite configurar outro empregado e data com variaveis de ambiente, pois os dados da demo sao compartilhados e podem mudar.

## Defeitos e limitacoes

Nenhum defeito foi confirmado nos casos executados. O resultado cobre apenas a consistencia de estado e historico para um registro submetido; nao demonstra ausencia de defeitos em outras jornadas ou estados.

Os testes unitarios cobrem o oraculo local de consistencia em JavaScript/Node, nao classes de producao do OrangeHRM. Nao foram implementados testes unitarios PHPUnit nem testes Vue do codigo original.

## Uso de IA

GitHub Copilot auxiliou na analise, implementacao inicial dos testes e redacao. O participante revisou o escopo e deve confirmar os resultados antes da entrega.