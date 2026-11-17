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
| `support/Cenario.java` | dados próprios de **cada teste**: funcionário, supervisor, usuários e vínculo *Report-to*, com sufixo único; removidos no fim do teste |
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

## Problemas encontrados ao executar

| Execução | Sintoma | Causa | Solução |
|---|---|---|---|
| 17/11/2026 00:43 | 4 testes que trocam de usuário no mesmo navegador ficaram presos na tela de login | o teste apagava os cookies com o dashboard ainda carregando; uma requisição em andamento devolvia o cookie da sessão antiga e o token CSRF da nova página de login deixava de valer (falha intermitente) | troca de usuário por `/auth/logout`, que encerra a sessão no servidor |
| 17/11/2026 00:58 | 7 erros em sequência: login do ESS recém-criado recusado, `401 Session expired` na API do Admin, funcionário ausente no autocompletar | **reinício do servidor de demonstração** durante a execução: uma consulta logo depois mostrou que os funcionários e usuários criados no início já não existiam, embora a limpeza tivesse falhado | dados criados **por teste** (e não por classe), com sessão nova de Admin; a limpeza faz login de novo. Um reinício passa a afetar só o teste em andamento |

| 17/11/2026 01:28 (`-Pdefeitos`) | ST-13 parou ao abrir a tela de detalhes, depois de 311 s | servidor de demonstração lento (a página de login levava de 3 a 11 s para responder, contra menos de 1 s antes); a página ficou presa até o limite padrão de 300 s do ChromeDriver | limite de carregamento de página de 90 s, para a falha aparecer logo; execução repetida |

As advertências `Unable to find CDP implementation matching 157` são inofensivas: o Chrome instalado é mais novo
que as ligações DevTools do Selenium 4.27, mas os testes usam só WebDriver.

## Resultado

> Preenchido após a execução.
