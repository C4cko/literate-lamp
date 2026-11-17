# Testes de sistema (Java + Selenium + JUnit 5): Claim

Issue #7. Casos ST-01 a ST-12 do [plano de testes](../plano/plano-de-testes-claim.md) (seção 6.3), executados pela
interface do **servidor de demonstração** (https://opensource-demo.orangehrmlive.com).

## Pré-requisitos

Java 17+, Maven 3.8+ e Google Chrome. O Selenium Manager (incluso no Selenium 4) baixa o ChromeDriver compatível
na primeira execução.

## Como rodar

```powershell
cd renan-bertoletti\sistema
mvn test                                  # ST-01 a ST-12, com o Chrome visível
mvn test "-Dheadless=true"                # sem abrir janela
mvn test "-Dtest=ClaimJourneySystemTest"  # só uma classe
```

Relatórios: `target/surefire-reports/` (um `.txt` e um `.xml` por classe).

| Propriedade (`-D`) | Padrão | Para quê |
|---|---|---|
| `ohrm.baseUrl` | `https://opensource-demo.orangehrmlive.com/web/index.php` | outro servidor (ex.: local) |
| `ohrm.user` / `ohrm.password` | `Admin` / `admin123` | administrador do servidor |
| `ohrm.event` / `ohrm.expenseType` | `Travel Allowance` / `Transport` | cadastros ativos usados nos testes |
| `ohrm.currencyId` / `ohrm.currencyName` | `USD` / `United States Dollar` | moeda |
| `headless` | `false` | Chrome sem janela |
| `timeout` | `30` | espera máxima, em segundos |

## Organização

| Arquivo | Conteúdo |
|---|---|
| `support/Config.java` | propriedades acima |
| `support/OrangeApi.java` | cliente da API REST v2 (login com token CSRF, como no navegador); usado **só** para preparar estados de partida e conferir efeitos que a tela não mostra |
| `support/Cenario.java` | dados próprios de cada classe: funcionário, supervisor, usuários e vínculo *Report-to*, com sufixo único; removidos no fim |
| `pages/Browser.java` | ChromeDriver, esperas explícitas e localização de campos pelo rótulo |
| `pages/ClaimFormPage.java` | *Submit Claim* e *Assign Claim* (criação) |
| `pages/ClaimDetailPage.java` | tela de detalhes: status, número de referência, despesas, total e botões de ação |
| `ClaimJourneySystemTest.java` | ST-01 a ST-07 e ST-12: jornada, fluxos alternativos, botões por papel |
| `ClaimValidationSystemTest.java` | ST-08 a ST-11: obrigatórios, submissão sem despesa (R3), valor da despesa, limite de Remarks |

## Cuidados (lições da análise de terceiros, #5)

| Problema nos projetos de terceiros | Aqui |
|---|---|
| Verificação sem `assert` (P1, P3) | toda verificação usa `assert` do JUnit; status e botões são lidos na tela |
| `catch` que engole exceções (P1) | nenhum `catch` nos testes; o cliente da API transforma qualquer resposta diferente de 200 em erro |
| `Thread.sleep` (P1) | só `WebDriverWait` com condições (status esperado, aviso de sucesso, total atualizado) |
| XPath absoluto (P1) | campos localizados pelo rótulo (`//label[normalize-space()='Amount']/ancestor::div[...]`) e botões pelo texto |
| Dados de outras pessoas no servidor compartilhado (P2, P3) | cada classe cria os próprios funcionários e usuários (`VVEss <sufixo>`, `VVSup <sufixo>`); cada teste cria a própria reivindicação e a abre pelo id |
| Testes encadeados por ordem (P1, P3) | um navegador novo por teste; nenhum teste depende de outro |

Os rótulos, rotas e mensagens vêm do código do front-end no commit `56e23b3b`
(`src/client/src/orangehrmClaimPlugin/` e as lang-strings em `installer/Migration/`).

## Resultado

> Preenchido após a execução.
