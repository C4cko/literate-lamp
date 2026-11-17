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
npm test
```

Resultado: 2 testes descobertos, 2 ignorados. A lista da demo nao apresentou Timesheet pendente acessivel durante essa execucao; por isso a pre-condicao marcou IT-01 e ST-01 como ignorados antes das assercoes funcionais. O comando terminou sem erro de infraestrutura depois da instalacao do Chromium, mas isso nao significa que os casos funcionais passaram.

## Exploracao manual

Em uma navegacao anterior na mesma data, foi possivel consultar uma Timesheet submetida na demo. A tela mostrou `Status: Submitted` e uma entrada `Submitted` na tabela de historico. A verificacao foi somente de leitura. Como a demo e compartilhada e os dados podem mudar, essa observacao nao substitui uma execucao automatizada reproduzivel.

## Defeitos e limitacoes

Nenhum defeito foi confirmado. Como os testes automatizados foram ignorados, tambem nao ha base para concluir que a funcionalidade esta livre de defeitos. Para executar os casos funcionais, e necessario que o responsavel pelo ambiente disponibilize uma Timesheet submetida acessivel ou uma instalacao local com dados de teste controlados.

Nao foram implementados nesta contribuicao testes unitarios PHPUnit ou Vue. O caso unitario esta especificado no plano, mas depende de preparar o ambiente de testes do OrangeHRM/cliente. Esta limitacao deve ser considerada na avaliacao da cobertura.

## Uso de IA

GitHub Copilot auxiliou na analise, implementacao inicial dos testes e redacao. O participante revisou o escopo e deve confirmar os resultados antes da entrega.