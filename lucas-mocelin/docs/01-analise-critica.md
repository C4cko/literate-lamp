# 1. Análise crítica dos testes existentes

Escopo: testes do repositório oficial (versão 5.9, commit `56e23b3`), com foco no que toca a
jornada de autenticação, e material de terceiros publicado sobre o OrangeHRM.
Critérios usados (vistos na disciplina): nível e tipo do teste, técnica de projeto,
qualidade do oráculo, independência/isolamento, legibilidade e manutenção, cobertura
e **eficácia** (capacidade de detectar defeitos, medida por mutação).

## 1.1 Inventário dos testes do repositório oficial

| Tipo | Ferramenta | Onde | Volume | Observação |
|---|---|---|---|---|
| "Unitários" de back-end | PHPUnit 9.6 | `src/plugins/*/test/` | 596 arquivos `*Test.php` | Organizados por camada: `Dao/`, `Service/`, `Api/`, `Entity/` |
| Testes de API | PHPUnit + YAML | `test/Api/*` + `fixtures/testcases/*.yaml` | parte dos 596 | Dirigidos por dados (cada caso é um bloco YAML) |
| Unitários de front-end | Jest | `src/client/src/core/util/**/__tests__` | 6 arquivos | Só utilitários (validação, datas, URL); nenhum componente Vue |
| Sistema (E2E) | Cypress 13 | `src/test/functional/cypress/e2e` | 11 arquivos | Admin, Leave e 1 arquivo de login |
| CI | GitHub Actions | `.github/workflows` | `test.yml`, `cypress.yml`, `linting.yml`… | Suítes rodam a cada *push* |

Distribuição por plugin (arquivos de teste PHPUnit): PIM 103, Admin 86, Core 53, Leave 51,
Maintenance 44, Time 37, …, **Authentication 14**. A suíte `Authentication` tem **54 testes /
160 asserções** e passou integralmente no nosso ambiente (ver `resultados/`).

## 1.2 Pontos fortes

1. **Massa de dados declarativa e reprodutível**: cada teste carrega um arquivo YAML
   (`TestDataService::populate`) que trunca e repovoa só as tabelas citadas.
2. **Uso consistente de dublês** (`getMockBuilder()->onlyMethods()`), isolando serviços
   (ex.: `AuthenticationServiceTest` simula o `UserService`).
3. **Testes de API dirigidos por dados**: os casos ficam em YAML, separados do código.
4. **Execução contínua** em GitHub Actions e banco de testes separado (`test_orangehrm_mysql`).
5. O teste Cypress de login usa um **oráculo forte**: verifica o cabeçalho `Location` da
   resposta (`/dashboard/index`), e não apenas algo visível na tela. Também reinicia o banco
   (`db:reset`) antes de rodar, evitando dependência de estado.

## 1.3 Pontos fracos (com evidência)

| # | Problema | Evidência | Consequência |
|---|---|---|---|
| F1 | **Exceção verificada com `try/catch` sem `$this->fail()`** | `AuthenticationServiceTest`: 4 testes (`…WithNotAssignedEss`, `…WithDisabledUser`, `…WithTerminatedEmployee`, `…WithoutEmployee`), 0 chamadas a `fail()` | Se a exceção deixar de ser lançada, o teste **passa**. Confirmado pela mutação **M9** (retornar `false` em vez de lançar a exceção): a suíte original não detecta. |
| F2 | **Classe central do login sem nenhum teste** | Cobertura (pcov) de `LocalAuthProvider`: **0 %** de linhas | A aplicação da política de senha forte no login não é verificada (mutação **M7** sobrevive). |
| F3 | **Cobertura alta não significa eficácia** | `AuthenticationService` tem **100 %** de linhas cobertas, mas a mutação **M8** (usuário excluído volta a logar) sobrevive | O ramo é executado, mas nenhum teste verifica o resultado para usuário excluído. |
| F4 | **Valores-limite ausentes** | `testIsValidPassword` tem só 2 casos; o comprimento máximo (64) nunca é testado | Mutação **M4** (`>` → `>=`) sobrevive. Regra de espaços também não é testada (**M5** sobrevive). |
| F5 | **Vários cenários num mesmo método e argumentos invertidos** | `testCheckPasswordPolicies` testa 2 senhas em sequência e chama `assertEquals($messages[0], 'Your password…')` (o esperado vem depois do obtido) | Se a 1ª asserção falha, a 2ª não roda; mensagens de falha ficam trocadas ("expected" = valor obtido). |
| F6 | **Nível do teste mal classificado** | Os testes de Service/Dao estendem `KernelTestCase` e exigem banco e *bootstrap* completo | São, na prática, testes de **integração**; não existe camada unitária leve. Rodar a suíte exige instalar MySQL. |
| F7 | **E2E de login raso** | `login.cy.js` tem 2 casos: login válido e campos vazios | Sem credenciais inválidas, logout, sessão, recuperação de senha, conta desabilitada. |
| F8 | **Front-end sem testes de componente** | Jest cobre só utilitários | Validação do formulário de login só é verificada no E2E. |
| F9 | Pequenos problemas de legibilidade | Nomes `testsSetCredentials…` (com "s"), sem uso de `@dataProvider` nos testes de serviço | Manutenção mais difícil. |

### Análise de mutação (eficácia)

Foram injetados 9 defeitos, um por vez, no código da autenticação (`scripts/mutacao.sh`).
"MORTO" significa que pelo menos um teste falhou, ou seja, o defeito foi detectado.

| ID | Defeito injetado | Suíte original (Authentication) | Suíte da jornada |
|---|---|---|---|
| M1 | conta desabilitada deixa de ser bloqueada | morto | morto |
| M2 | funcionário desligado deixa de ser bloqueado | morto | morto |
| M3 | limite mínimo de comprimento `<` → `<=` | morto | morto |
| M4 | limite máximo de comprimento `>` → `>=` | **sobrevive** | morto |
| M5 | regra de espaços invertida | **sobrevive** | morto |
| M6 | login com hash legado (MD5) quebra | **sobrevive*** | morto |
| M7 | política de senha forte ignorada no login | **sobrevive** | morto |
| M8 | usuário excluído volta a logar | **sobrevive** | morto |
| M9 | conta desabilitada retorna `false` em vez de exceção | **sobrevive** | morto |
| | **Escore de mutação** | **3/9 (33 %)** | **9/9 (100 %)** |

\* M6 é detectado pela suíte do plugin **Admin** (2 falhas em `UserServiceTest`), ou seja, a
regra é testada, mas fora do módulo de autenticação.

## 1.4 Material de terceiros

O OrangeHRM é muito usado como alvo de exercícios de automação. Uma busca no GitHub e em
blogs mostra dezenas de projetos, por exemplo:

* Playwright + TypeScript com testes E2E e de API (`code-pace/playwright-orangeHrm`);
* Selenium + Python + pytest com Page Object Model (`vrun545/orangehrm_with_selenium_python`, `imranc07/orangehrmpom`);
* Selenium + Java + TestNG/Cucumber (`Fayrose96/OrangeHRM-testNG`, `ManarBedeir/OrangeHRM-SeleniumJava`);
* Robot Framework + SeleniumLibrary (`fat3mi/simplerobotframework`);
* tutoriais "primeiro teste com Playwright" que fazem apenas o login na demo.

Padrões observados nas descrições e na estrutura desses projetos:

* **Positivo**: uso frequente de Page Object Model e de testes dirigidos por dados
  (credenciais válidas e inválidas em CSV ou Excel).
* **Só testam no nível de sistema**: todos testam pela interface ou pela API REST do servidor
  de demonstração. Nenhum testa o código PHP, o que inverte a pirâmide de testes.
* **Dependem de um ambiente compartilhado e instável**: a demo pública é reiniciada
  periodicamente e alterada por outros usuários ao mesmo tempo, o que causa falhas
  intermitentes e impede preparar dados controlados (ex.: conta desabilitada).
* **Cobertura da jornada de login limitada** ao par "credencial válida / inválida". Casos de
  sessão, logout, recuperação de senha e segurança (CSRF, enumeração de usuários) raramente
  aparecem.

## 1.5 Como este trabalho responde às fraquezas

| Fraqueza | Resposta na jornada |
|---|---|
| F1 | Exceções verificadas com `expectException()` ou `try { …; $this->fail() }` |
| F2, F7 | 3 testes de integração para `LocalAuthProvider` e 19 casos E2E |
| F3, F4 | Tabela de decisão completa e análise de valor-limite, validadas por mutação |
| F5, F9 | Um cenário por caso, via `@dataProvider` com nomes descritivos (UT-PW-xx) |
| F6 | Testes unitários que **não** acessam banco (dublês para configuração e i18n) separados dos de integração (`@group Unit` / `@group Integration`) |
| Terceiros | Testes E2E rodam também contra uma **instalação local controlada** (banco e SMTP próprios), o que permite o fluxo completo de recuperação de senha por e-mail |
