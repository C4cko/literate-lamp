# Análise crítica dos testes existentes: módulo Claim (reivindicações de despesa)

> Issue: #5
> Repositório `orangehrm/orangehrm`, branch `main`, commit `56e23b3b09e7af29317a0943523b825843fff527` (29/06/2026),
> o mesmo commit usado na análise da jornada Add Employee (#2), para que o relatório unificado trate de uma única versão.
> Escopo: testes que exercitam o plugin `orangehrmClaimPlugin` (back-end), as telas de Claim (front-end) e projetos
> de terceiros que automatizam Claim no servidor de demonstração.
> Grade de avaliação (a mesma de #2 e #4): nível, técnica, cobertura, isolamento, oráculo, legibilidade e lacunas.

## 1. Panorama

O que vale para o sistema todo já foi descrito por Mateus Neubarth em
[`mateus-neubarth/analise/codigo-original.md`](../../mateus-neubarth/analise/codigo-original.md) (#2): três frentes
de teste (PHPUnit, Jest e Cypress), CI em `.github/workflows/test.yml` e o padrão de teste de endpoint orientado a
dados, cujo oráculo fica em `src/test/phpunit/Util/EndpointIntegrationTestCase.php::assertValidTestCase`. Esta
análise reaproveita essas conclusões e se concentra no Claim.

| Frente | Ferramenta | Testes de Claim |
|---|---|---|
| Back-end | PHPUnit 9 (suíte `Claim` em `phpunit.xml`) | **17 classes, 65 métodos de teste**, dos quais 7 são data-driven e expandem para **217 casos** em YAML. |
| Front-end | Jest | **Nenhum.** Não há `__tests__` nem `*.spec.*` em `src/client/src/orangehrmClaimPlugin/` (telas `submitClaim`, `assignClaim`, `myClaims`, `employeeClaims`, `claimExpenseTypes`). |
| E2E | Cypress | **Nenhum.** As 11 specs de `src/test/functional/cypress/e2e/` cobrem login, Leave e Admin. |

### Inventário do back-end

Caminhos relativos a `src/plugins/orangehrmClaimPlugin/test/`.

| Pasta | Arquivo | Métodos | Casos YAML | Classe base | Alvo |
|---|---|---|---|---|---|
| `Api/` | `ClaimRequestActionAPITest.php` | 5 | 18 (`Update`) | `EndpointIntegrationTestCase` | **Transições de estado** (SUBMIT, APPROVE, REJECT, PAY, CANCEL) |
| `Api/` | `MyClaimRequestAPITest.php` | 3 | 26 | idem | Funcionário cria e consulta as próprias reivindicações |
| `Api/` | `EmployeeClaimRequestAPITest.php` | 3 | 22 | idem | Admin/supervisor atribui (*Assign Claim*) e consulta reivindicações de outros |
| `Api/` | `ClaimExpenseAPITest.php` | 5 | 40 | idem | Despesas de uma reivindicação (valor, data, tipo, nota) |
| `Api/` | `ClaimAttachmentAPITest.php` | 5 | 42 | idem | Anexos |
| `Api/` | `ClaimEventAPITest.php` | 5 | 36 | idem | Cadastro de eventos (configuração) |
| `Api/` | `ClaimExpenseTypeAPITest.php` | 5 | 33 | idem | Cadastro de tipos de despesa (configuração) |
| `Dao/` | `ClaimDaoTest.php` | 9 | – | `KernelTestCase` + fixtures | Eventos, tipos, `saveClaimRequest`, `getNextId` |
| `Dao/` | `ClaimDaoRequestTest.php` | 5 | – | idem | Consulta de reivindicações e **bloqueio pessimista** (`FOR UPDATE`) |
| `Dao/` | `ClaimDaoExpenseTest.php`, `ClaimDaoExpenseTypeTest.php`, `ClaimDaoAttachmentTest.php` | 4 + 4 + 7 | – | idem | CRUD de despesas, tipos e anexos |
| `Entity/` | 5 arquivos (`ClaimRequestTest.php` etc.) | 1 cada | – | `EntityTestCase` | Getters e setters das entidades |

Classes de produção **sem teste**: `Service/ClaimService.php` (gera o número de referência da reivindicação),
`Api/Traits/ClaimRequestAPIHelperTrait.php` (só exercitado indiretamente) e os 12 controllers de `Controller/`.

## 2. Como o fluxo de aprovação é definido

As transições de estado do Claim **não estão no código PHP**: ficam na tabela `ohrm_workflow_state_machine`
(fluxo `WorkflowStateMachine::FLOW_CLAIM = 7`). `ClaimRequestActionAPI::update()` pergunta ao
`UserRoleManager` quais ações o papel do usuário pode fazer no estado atual e grava o estado resultante.

As transições são cadastradas pela migração `installer/Migration/V5_5_0/Migration.php` (linhas 322–407). Nos
testes, o arquivo `src/test/phpunit/fixtures/WorkflowStateMachine.yaml` **não está versionado**: ele é gerado a
partir do banco instalado pelo comando `i:create-test-db` (`WorkflowStateMachineFixture`), que o CI executa antes
do PHPUnit. Portanto, os testes usam as mesmas transições da migração.

| # | Estado | Papel | Ação | Estado resultante | Coberto por caso de sucesso? |
|---|---|---|---|---|---|
| W1 | INITIATED | ESS | SUBMIT | SUBMITTED | Sim ("ESS SUBMIT own INITIATED", "Admin SUBMIT own INITIATED") |
| W2 | INITIATED | ESS | CANCEL | CANCELLED | Sim ("ESS CANCEL own INITIATED", "Admin CANCEL own INITIATED") |
| W3 | SUBMITTED | ESS | CANCEL | CANCELLED | **Não** |
| W4 | REJECTED | ESS | SUBMIT | SUBMITTED | Sim ("Admin SUBMIT own REJECTED") |
| W5 | INITIATED | ADMIN | SUBMIT | **PAID** | Sim ("Admin SUBMIT ESS INITIATED") |
| W6 | APPROVED | ADMIN | REJECT | REJECTED | **Não** |
| W7 | SUBMITTED | ADMIN | APPROVE | **PAID** | **Não** |
| W8 | SUBMITTED | ADMIN | REJECT | REJECTED | **Não** |
| W9 | APPROVED | ADMIN | PAY | PAID | **Não** |
| W10 | INITIATED | SUPERVISOR | SUBMIT | APPROVED | Sim ("Supervisor SUBMIT ESS INITIATED") |
| W11 | SUBMITTED | SUPERVISOR | APPROVE | APPROVED | **Não** |
| W12 | SUBMITTED | SUPERVISOR | REJECT | REJECTED | **Não** |

**Cobertura de transições: 5 de 12 (≈ 42%).** Os seis estados aparecem nos dados de teste, mas o estado
REJECTED nunca é **alcançado** por uma ação; ele só aparece como estado de partida carregado da fixture.
Nenhum caso aprova, rejeita ou paga uma reivindicação com sucesso. É justamente a metade "gestor" da jornada
escolhida (#6).

Dois pontos do modelo chamam atenção e serão conferidos no servidor de demonstração durante o plano (#6):

1. **W7:** quando o admin aprova uma reivindicação SUBMITTED, ela vai direto para PAID, sem passar por APPROVED.
   Nesse caso, a transição W9 (APPROVED → PAY) só é alcançável quando um **supervisor** aprovou antes.
2. **W5:** quando o admin atribui e submete uma reivindicação em nome do funcionário, ela já nasce PAID.

Esses comportamentos podem ser intencionais. O problema, do ponto de vista de teste, é que **nenhum documento
os especifica**. O único caso que cobre W5 usa como resultado esperado o que o código faz (`status: "PAID"`),
então o oráculo foi derivado da implementação, e não de um requisito.

## 3. Avaliação por arquivo

### C1: `ClaimRequestActionAPITest` + `ClaimRequestActionAPITestCases.yaml`

Os 18 casos (seção `Update`) dividem-se em 8 de sucesso e 10 de erro: ação inválida, ação nula, `requestId`
inválido ou inexistente, reivindicação excluída e cinco tentativas de transição proibida (*sneak paths*).

| Critério | Avaliação |
|---|---|
| Nível | **Integração**: kernel real, banco com fixtures (`ClaimRequestActionAPITest.yaml`), `UserRoleManager` real e máquina de estados carregada do banco. |
| Técnica | Há uma tentativa de teste baseado em estados: transições válidas e algumas inválidas por papel. Mas falta método: os casos não seguem uma tabela de transições, e por isso 7 das 12 transições válidas ficaram de fora (seção 2). |
| Cobertura | Fluxo do funcionário (submeter, cancelar, ressubmeter) bem coberto. **Aprovação, rejeição e pagamento: zero casos de sucesso.** Também não há casos para o efeito colateral `submittedDate`, que `update()` grava só no SUBMIT. |
| Isolamento | Bom: `populateFixtures(..., true)` recarrega o banco a cada caso, e a data é fixada com `registerMockDateTimeHelper`. |
| Oráculo | Forte para a resposta (JSON completo, incluindo `status`). Mas, como em #2, **não confere o banco**: um caso passaria mesmo que `saveClaimRequest` não fosse chamado, desde que o objeto em memória tivesse o estado certo. |
| Legibilidade | Os nomes dizem papel, ação e estado, o que ajuda. Porém: **"Supervisor PAY APPROVED claim request" envia `action: APPROVE`**, então o nome promete um caso (supervisor tentando pagar) que não é testado; há erro de digitação em "Admin PAY for **onw** PAID"; e o bloco `services`/`factories` se repete nos 18 casos. |
| Lacunas | W3, W6–W9, W11, W12; ESS tentando REJECT/PAY; supervisor tentando PAY; CANCEL depois de APPROVED/PAID; valor de `submittedDate` após SUBMIT e após ressubmissão. |

### C2: `MyClaimRequestAPITest` e `EmployeeClaimRequestAPITest` (criação)

Casos de `Create`: reivindicação válida, evento desativado, sem descrição, descrição nula, evento inexistente,
moeda inexistente; e, no `EmployeeClaimRequestAPI`, ESS criando para si mesmo, admin para si mesmo e funcionário
desligado.

| Critério | Avaliação |
|---|---|
| Nível | Integração (mesmo padrão de C1). |
| Técnica | **Particionamento de equivalência** explícito para evento (ativo, desativado, inexistente), moeda (válida, inexistente) e descrição (presente, ausente, nula). É o conjunto mais bem projetado do plugin. |
| Cobertura | Boa para as classes de entrada. **Não há valor-limite** para a descrição (o campo tem 1000 caracteres na migração 5.5.0: faltam 1000 e 1001). |
| Isolamento | Bom. |
| Oráculo | Compara a resposta completa, incluindo o `referenceId` gerado. Como a data é fixada, o oráculo é determinístico. |
| Lacunas | Limite da descrição; reivindicação para funcionário de outro supervisor; formato do `referenceId` quando o próximo id passa de 7 dígitos (ver C4). |

### C3: `ClaimExpenseAPITest` (despesas)

A regra do valor está em `ClaimExpenseAPI.php`: `Rules::POSITIVE`, `Rules::BETWEEN [0, 9999999999.99]` e a
expressão regular `/^\d+(\.\d{1,2})?$/` (até duas casas decimais).

| Critério | Avaliação |
|---|---|
| Nível | Integração. |
| Técnica | **Valor-limite parcial.** Há casos para o máximo (`9999999999.99`, aceito), muito acima do máximo (`10000000001.0`) e 3 casas decimais (`7000.899`, `7000.877`). Os demais 30+ casos usam o mesmo valor, `7000.0`. |
| Cobertura | Boa para **estado da reivindicação × permissão**: criar despesa em INITIATED (permitido), em SUBMITTED, PAID e REJECTED de outro usuário (proibidos). Isso confirma, por outro caminho, as regras da máquina de estados. |
| Isolamento | Bom. |
| Oráculo | Resposta completa ou exceção esperada. |
| Lacunas | Limites inferiores: **`0`, `0.01` e valor negativo** não são testados, e a regra é ambígua: `POSITIVE` rejeita 0, mas `BETWEEN [0, …]` o aceitaria. Também falta o limite superior exato (`10000000000.00`, primeiro valor acima do máximo), além de 1 e 2 casas decimais como par (`7000.1`/`7000.12`) e data futura ou anterior à criação. |

### C4: `ClaimDaoTest`, `ClaimDaoRequestTest` e `ClaimService` (sem teste)

| Critério | Avaliação |
|---|---|
| Nível | Integração com o banco (fixtures `ClaimRequest.yaml`, `ClaimEvent.yaml`...). |
| Técnica | Testes de exemplo único: um caso por método do DAO. |
| Destaque positivo | `testGetClaimRequestByIdForUpdateLocksRowAgainstConcurrentWriter` abre **duas conexões** e prova que o `SELECT … FOR UPDATE` bloqueia um segundo escritor (*lock wait timeout*). É um teste de concorrência real, documentado, com limpeza em `finally`, raro em projetos desse porte. Ele protege `ClaimExpenseAPI` contra uma condição de corrida entre "adicionar despesa" e "submeter". |
| Oráculo | Valores exatos (`assertEquals(4, $nextId)`), mas dependentes do conteúdo da fixture: se um registro for acrescentado, vários números mudam. |
| Lacuna principal | **`ClaimService::getReferenceId()` não tem teste.** Ele monta `data (Ymd) + id com 7 dígitos` (`str_pad`). É uma função quase pura, ideal para teste unitário com mock do DAO e da data, com limites em 9 999 999 → 10 000 000 (o `str_pad` deixa de preencher e o número passa a ter 16 caracteres). |

### C5: testes de entidade (`Entity/*.php`)

| Critério | Avaliação |
|---|---|
| Nível | Nominalmente unitários, mas **acessam o banco**: o `setUp` chama `TestDataService::populate` e os testes usam `getReference(Employee::class, 1)`. |
| Técnica | Nenhuma: atribuem valores e leem de volta (*getter/setter*). |
| Oráculo | Quase tautológico: só falharia se um getter devolvesse outro campo. |
| Valor | Baixo. Elevam a cobertura de linhas sem testar regra de negócio, o que ilustra que **cobertura não é qualidade de teste**. |

## 4. Material de terceiros

Busca feita em 16/11/2026 na busca de repositórios do GitHub (`orangehrm claim`, `orangehrm claims test`) e em
mecanismos de busca. A maioria dos projetos públicos de OrangeHRM cobre só login e PIM. Os que de fato têm
código de Claim estão abaixo (commit = último commit no momento da análise).

| ID | Projeto | Stack | Commit | O que automatiza |
|---|---|---|---|---|
| P1 | [vishnuvnairqa/OrangeHRM-Website-Automation-Testing](https://github.com/vishnuvnairqa/OrangeHRM-Website-Automation-Testing) | Java, Selenium, TestNG, ExtentReports | `4d92f4c5` (10/11/2025) | Admin atribui reivindicação, adiciona despesa e busca por nome (`src/test/java/testpkg/G_ClaimsTest .java`) |
| P2 | [Dartas22/Automation-tests-orangehmr](https://github.com/Dartas22/Automation-tests-orangehmr) | Python, pytest-bdd, Selenium | `9c56eba3` (11/10/2023) | Admin cria reivindicação via *Assign Claim* (`tests/features/add_claim.feature`) |
| P3 | [lalithagonnuri/cypresswithCucumber_orangehrmportal](https://github.com/lalithagonnuri/cypresswithCucumber_orangehrmportal) | Cypress, Cucumber | `57f94335` (26/02/2026) | Criar reivindicação, adicionar despesa e submeter (`cypress/e2e/claim.feature`) |
| P4 | [praffulvats003/Project-2---HR-Management-Web-Application](https://github.com/praffulvats003/Project-2---HR-Management-Web-Application) | Python, pytest, Selenium | `e03a8811` (21/09/2026) | Só verifica que o menu Claim existe (`tests/test_10_claim_request.py`) |
| P5 | [davigotardi/orangehrm-qa-tests](https://github.com/davigotardi/orangehrm-qa-tests) | Casos manuais (Markdown) | `549047c1` (04/11/2026) | Casos de teste manuais de Claim (`docs/casos-claim.md`) |

### P1: vishnuvnairqa (Java + Selenium + TestNG)

| Critério | Avaliação |
|---|---|
| Nível | Sistema (UI), contra o servidor de demonstração. |
| Técnica | Nenhuma: um caminho feliz com dados fixos (funcionário "Vishnu Nair", valor `3500000`). |
| Cobertura | Cria a reivindicação e adiciona uma despesa, mas **nunca clica em Submit**. O comentário no código (`create >>> addexpense >>> save >> submite`) promete mais do que o teste faz. Aprovação e rejeição não aparecem. |
| Isolamento | **Ruim.** Três métodos encadeados por `priority` (login → atribuir → buscar), no mesmo navegador. Cada execução cria mais uma reivindicação no servidor compartilhado, e nada é removido. |
| Oráculo | **Inexistente na prática.** `Assign()` não tem nenhuma verificação. `searchByName()` compara o nome, mas em caso de diferença só chama `test.fail(...)` do **ExtentReports**, sem `assert`. Além disso, um `catch (Exception e)` engole qualquer erro. O TestNG, portanto, nunca registra falha: o relatório JUnit versionado no próprio repositório (`test-output/junitreports (1)/TEST-testpkg.G_ClaimsTest (1).xml`) mostra `tests="3" failures="0"`, independentemente do que aconteceu na tela. |
| Legibilidade | Mistura de bons locators relativos (`//label[text()='Amount']/following::input`) com XPaths absolutos (`//*[@id="app"]/div[1]/div[1]/aside/nav/div[2]/ul/li[11]/a`, que depende de Claim ser o 11º item do menu). Usa `Thread.sleep(2000)` e `Thread.sleep(3000)` entre os passos. Os nomes de pacote (`B_addcandidate` para Claim) e de método (`clameclick`) confundem. O nome do arquivo tem um espaço antes de `.java`. |
| Lacunas | Oráculo real (status, número de referência, total); submissão; aprovação; dados únicos; limpeza. |

**Veredito:** é o projeto com a mesma stack que vou usar (Java + Selenium), e por isso é o contraexemplo mais
útil. Um teste que relata a falha num relatório HTML, mas não no *runner*, é um teste que **não pode falhar**.

### P2: Dartas22 (pytest-bdd + Selenium)

| Critério | Avaliação |
|---|---|
| Nível | Sistema (UI). |
| Técnica | Usa `Scenario Outline` com `Examples`, o formato certo para particionamento, mas com **uma única linha** de exemplo. |
| Cobertura | Só a criação (estado INITIATED). Sem despesa, sem submissão. |
| Isolamento | O `webdriver.Chrome` é criado no *import* do módulo. Os caminhos do chromedriver e do `sys.path` são absolutos de Windows (`C:\Program Files (x86)\chrome-win64\chromedriver.exe`, `D:\VC\testing_proj\...`): **o projeto não roda em outra máquina** sem edição. |
| Oráculo | Fraco: confere só que o título "Assign Claim" está visível. Não confere status, funcionário nem número de referência. |
| Dados | O nome do funcionário é `"a"` seguido de seta para baixo e Enter, ou seja, **qualquer funcionário** cujo nome contenha "a" e apareça primeiro no autocompletar do servidor compartilhado. O teste não é determinístico. |
| Defeito no próprio teste | O passo "Select Currency" usa o locator do **evento** (`eventSelect`) e depende de um `TAB` para chegar à moeda. Funciona por acaso da ordem dos campos. |
| Legibilidade | Todos os passos usam o nome de função `step_imp`, que se sobrescreve no módulo (funciona só porque o decorador registra antes). Há erros de digitação nos passos ("Opem", "empleyee"). |

### P3: lalithagonnuri (Cypress + Cucumber)

| Critério | Avaliação |
|---|---|
| Nível | Sistema (UI). |
| Técnica | BDD com `Background` para o login; a escrita em Gherkin é legível e próxima de uma história de usuário. |
| Cobertura | É o único projeto que chega a **submeter** a reivindicação. Não testa aprovação. |
| Isolamento | **Ruim.** O 2º cenário ("open the latest claim") abre a primeira linha da tabela do servidor, que pode ser uma reivindicação de qualquer outro usuário do demo. Os dois cenários dependem um do outro e do estado do servidor. |
| Oráculo | **Ausente.** O 1º cenário não tem nenhum `Then`. No 2º, o `Then` final só confere que a tabela está visível: a verificação do status está **comentada** (`//cy.get('.oxd-table-cell').eq(5).should('contain.text', 'Submitted')`). |
| Defeito no próprio teste | O 1º cenário clica em *Assign Claim* e **não preenche Employee Name**, campo obrigatório nesse formulário, e ainda clica em *Create* antes de preencher qualquer campo. Como não há `Then`, o cenário passa mesmo que a reivindicação não seja criada. A data `2025-05-12` é digitada num campo com máscara `yyyy-dd-mm` e pode ser interpretada como 5 de dezembro. |
| Legibilidade | Boa: locators por texto e rótulo (`cy.contains('label', 'Amount')`) em vez de XPath absoluto. É a prática que vale reaproveitar. |

### P4 e P5

- **P4** é honesto sobre o que faz: o próprio comentário diz que o teste só verifica que o módulo é acessível e que
  o formulário "is NOT yet implemented". Não testa a jornada.
- **P5** não é automação, mas casos manuais. Vale citar o caso CLM-006, que propõe exatamente os limites que faltam
  em C3 (0, valor negativo, mínimo e máximo inclusivos, um incremento fora e uma casa decimal a mais). Os casos
  são genéricos: dependem de uma "ficha de parâmetros" que o próprio projeto não preenche com os valores do
  OrangeHRM.

### Comparação

| Critério | P1 Java/TestNG | P2 pytest-bdd | P3 Cypress/Cucumber | Código original (C1–C3) |
|---|---|---|---|---|
| Chega a SUBMITTED | Não | Não | Sim | Sim |
| Testa aprovação/rejeição | Não | Não | Não | **Não** (só casos proibidos) |
| Oráculo pode falhar | **Não** | Sim (fraco) | **Não** | Sim (forte na resposta) |
| Dados determinísticos | Não | Não | Não | Sim (fixtures) |
| Independência entre testes | Não | Sim (1 cenário) | Não | Sim |
| Valor-limite | Não | Não | Não | Parcial |

## 5. Síntese

| Critério | Situação geral |
|---|---|
| Nível | Back-end com muitos testes de **integração de endpoint**; nenhum teste unitário puro de regra de negócio do Claim; nenhum teste de sistema no projeto original. Os terceiros só têm testes de sistema. |
| Técnica | O original usa particionamento bem (C2) e valor-limite em parte (C3). O teste de estados (C1) existe, mas sem uma tabela de transições guiando os casos. Os terceiros não aplicam técnica de projeto. |
| Cobertura | Fluxo do **funcionário** razoavelmente coberto; fluxo do **gestor** (aprovar, rejeitar, pagar) **sem nenhum caso de sucesso** em qualquer fonte analisada. |
| Isolamento | Bom no original (fixtures por caso, data fixada). Ruim nos terceiros (servidor compartilhado, dados não determinísticos, ordem entre testes). |
| Oráculo | O ponto mais crítico: no original, falta conferir o banco e falta especificação para W5/W7; em P1 e P3, **os testes não conseguem falhar**. |
| Legibilidade | Boa nos YAML do original (nomes descritivos), com um nome enganoso em C1. Nos terceiros, prejudicada por XPaths absolutos e `sleep`, com exceção dos locators por rótulo de P3. |

### Lacunas que os testes da jornada vão cobrir (planejadas em #6)

| Lacuna | Nível previsto |
|---|---|
| Transições W3, W6–W9, W11, W12 e *sneak paths* restantes (cobertura de todas as transições) | Integração e sistema |
| `ClaimService::getReferenceId()` e o limite de 7 dígitos | Unitário |
| Limites do valor: `0`, `0.01`, negativo, `9999999999.99`, `10000000000.00`, 1/2/3 casas decimais | Unitário/integração |
| Limite da descrição (1000/1001 caracteres) | Integração |
| Estado no banco e `submittedDate` após SUBMIT e ressubmissão | Integração |
| Confirmação no servidor de W5 e W7 (admin → PAID) | Sistema |
| Jornada ponta a ponta: funcionário submete → gestor aprova ou rejeita, com oráculo no status exibido | Sistema |

### Boas práticas que serão reaproveitadas

- Oráculo com a resposta completa e dados controlados por fixture (original, C1–C3).
- Teste de concorrência com duas conexões e limpeza em `finally` (original, C4).
- Locators por rótulo/texto em vez de XPath absoluto (P3).
- Gherkin para descrever critérios de aceitação da jornada (P2, P3), mas **sempre com um `Then` verificável**.

### O que será evitado

- Relatar falha só no relatório, sem `assert` (P1); `catch` que engole exceções.
- `Thread.sleep` fixo; esperas explícitas no lugar (P1, P2).
- Dependência de dados alheios no servidor compartilhado (P2, P3): cada teste de sistema criará os próprios dados
  com identificador único.
- Usar o comportamento atual como oráculo sem documentar a regra (W5 em C1).

### Execução

Os testes originais **não foram executados** nesta issue; a análise é estática (leitura do código e dos YAML).
A execução fica para a issue #7, aproveitando o ambiente Docker descrito por Mateus Neubarth em
[`mateus-neubarth/docs/ambiente.md`](../../mateus-neubarth/docs/ambiente.md).

## Fontes

- OrangeHRM, https://github.com/orangehrm/orangehrm, commit `56e23b3b`; arquivos citados nas seções 1–3, em especial
  `src/plugins/orangehrmClaimPlugin/test/`, `src/plugins/orangehrmClaimPlugin/Api/ClaimRequestActionAPI.php`,
  `src/plugins/orangehrmClaimPlugin/Api/ClaimExpenseAPI.php`, `src/plugins/orangehrmClaimPlugin/Service/ClaimService.php`,
  `installer/Migration/V5_5_0/Migration.php`, `src/test/phpunit/Util/Fixture/WorkflowStateMachineFixture.php`,
  `phpunit.xml` e `.github/workflows/test.yml`.
- NEUBARTH, Mateus. Análise dos testes do código original (Add Employee), issue #2:
  [`mateus-neubarth/analise/codigo-original.md`](../../mateus-neubarth/analise/codigo-original.md).
- NEUBARTH, Mateus. Análise de testes de terceiros (Add Employee), issue #4:
  [`mateus-neubarth/analise/terceiros.md`](../../mateus-neubarth/analise/terceiros.md).
- Projetos de terceiros P1–P5, com links e commits na seção 4.
- PHPUnit 9 Manual: https://docs.phpunit.de/en/9.6/

## Uso de IA

Levantamento dos arquivos, leitura dos testes e redação inicial feitos com Claude Code (Claude Opus 5.5,
Anthropic); revisão, conferência das conclusões e ajustes feitos pelo autor.
