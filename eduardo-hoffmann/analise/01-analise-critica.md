# Análise crítica dos testes existentes: Time › Attendance (Punch In / Punch Out)

> Issue: #15
> Repositório `orangehrm/orangehrm`, commit `56e23b3b09e7af29317a0943523b825843fff527` (29/06/2026), o mesmo usado
> pela turma. Escopo: testes que exercitam o plugin `orangehrmAttendancePlugin` e material de terceiros sobre
> Attendance. Critérios (os mesmos de #2, #4 e #5): nível, técnica, cobertura, isolamento, oráculo, legibilidade e
> lacunas.
> **Situação:** análise estática (leitura do código e dos arquivos YAML) mais a execução da suíte original (seção 6).
> Falta explorar o servidor de demonstração (seção 7).

## 1. Panorama

O que vale para o sistema todo já foi descrito por Mateus Neubarth em
[`mateus-neubarth/analise/codigo-original.md`](../../mateus-neubarth/analise/codigo-original.md) (#2): PHPUnit,
Jest e Cypress, e o padrão de teste de endpoint orientado a dados. Esta análise se concentra em Attendance.

| Frente | Ferramenta | Testes de Attendance |
|---|---|---|
| Back-end | PHPUnit (suíte `Attendance` em `phpunit.xml`) | **14 classes, 71 métodos**, em `src/plugins/orangehrmAttendancePlugin/test/` |
| Front-end | Jest | **Nenhum.** `src/client/src` tem 6 arquivos `*.spec.*`/`*.test.*` no total e nenhum tem "attend" ou "time" no nome (cinco ficam em `core/util/helper/__tests__`) |
| Ponta a ponta | Cypress | **Nenhum de Attendance.** A única ocorrência de "attendance" em `src/test/functional` é um `cy.intercept` de `/api/v2/attendance/timezones` em `workspaceNotification.cy.js` |

Distribuição das 14 classes:

| Nível | Classes | Métodos |
|---|---|---|
| Integração de endpoint (API v2, orientada a dados, `EndpointIntegrationTestCase`) | 12 | 60 |
| Integração de DAO com banco (`KernelTestCase`) | 1 (`AttendanceDaoTest`) | 10 |
| Entidade (`EntityTestCase`) | 1 (`AttendanceRecordTest`) | 1 |
| Unitário puro (sem banco) | **0** | 0 |

Os 12 testes de endpoint usam *data providers* que leem os arquivos de `fixtures/testcases/*.yaml`. Somando os
cenários dos 12 arquivos YAML, são **175 casos** (por exemplo, `AttendanceRecordAPITestCases.yaml` tem 42 e
`EmployeeAttendanceRecordAPITestCases.yaml` tem 30).

A jornada de Punch In / Punch Out é coberta nas operações `Create` (punch in) e `Update` (punch out) de
`MyAttendanceRecordAPI` e `EmployeeAttendanceRecordAPI`. Nos YAML, `Create` tem 7 cenários em `MyAttendanceRecordAPI` e
6 em `EmployeeAttendanceRecordAPI`, e `Update` tem 10 e 8.

## 2. Avaliação por arquivo

### 2.1 `Api/AttendancePunchInRecordOverlapAPITest.php` (e as três variantes de overlap)

- **Nível:** integração de endpoint, com banco populado por fixture e usuário autenticado simulado.
- **Técnica:** orientada a dados. Cada cenário do YAML define usuário, serviços, consulta e resultado ou exceção.
- **Cobertura:** o YAML tem 11 cenários e 2 exceções ("Cannot Proceed Punch In Employee Already Punched In").
  Cobre Admin e Supervisor. Os demais métodos (`testUpdate`, `testDelete` e os dois `getValidationRuleFor*`) apenas
  verificam que lançam "não implementado", o que infla a contagem de testes sem testar regra de negócio.
- **Oráculo:** `assertValidTestCase` (em `src/test/phpunit/Util/EndpointIntegrationTestCase.php`) compara a resposta
  normalizada com o `data` esperado, ou exige a exceção e a mensagem. Ele não consulta o banco depois da chamada.
  Para um GET de verificação de sobreposição isso basta, porque não há efeito colateral.
- **Isolamento:** bom. Cada teste recarrega a fixture. O tempo é controlado por `registerMockDateTimeHelper`.
- **Legibilidade:** boa nos nomes dos cenários; nos cenários que li, o YAML repete o mesmo bloco `services`/`factories`.

### 2.2 `Dao/AttendanceDaoTest.php`

- **Nível:** integração com o banco real de testes. É o arquivo mais próximo de um teste de regra de negócio da
  jornada.
- **Técnica:** testes escritos à mão, com fixture própria (`AttendanceDaoTest.yaml`).
- **Cobertura:** 10 métodos. Cobre último registro por funcionário, busca por id, sobreposição de punch in e de
  punch out (mensagens "Cannot Proceed Punch Out Employee Already Punched Out" e "Punch Out Time Should Be Later Than
  Punch In Time"), relatório-resumo de horas e listagem. Há também testes de bloqueio de transação
  (`...ForUpdateRequiresTransactionForLock`, `...LocksLatestRecordAgainstConcurrentPunchIn`), o que é raro e
  valioso.
- **Oráculo:** asserções diretas e específicas (nomes, totais em segundos, quantidade de registros). Bom.
- **Problemas observados:**
  - Nas linhas 77 e 78 a mesma asserção está repetida (`getPunchInUserTime()->format('Y-m-d')` contra
    `'2011-05-27'`). Parece que uma delas deveria conferir `getPunchInUtcTime()`, que fica sem verificação. É uma
    suspeita pela leitura; a suíte passa com a repetição (seção 6).
  - `testAttendanceSummeryReport` concentra muitas asserções com números "mágicos" (`64800`, `250200`, `217800`),
    difíceis de rastrear até a fixture.

### 2.3 `Entity/AttendanceRecordTest.php`

- **Nível:** entidade (persistência pelo Doctrine).
- **Cobertura:** 1 método que grava um punch in e um punch out e relê os registros.
- **Problema relevante:** o teste grava os estados `'PUNCH IN'` e `'PUNCH OUT'`, mas as constantes da entidade são
  `STATE_PUNCHED_IN = "PUNCHED IN"` e `STATE_PUNCHED_OUT = "PUNCHED OUT"`. Ou seja, o teste usa valores diferentes
  dos que as constantes definem. A execução confirmou que a entidade aceita esses valores (seção 6). O teste confere apenas as notas, não o estado nem as datas.
- **Oráculo:** fraco (só `assertEquals` sobre as notas).

### 2.4 `Api/AttendanceRecordAPITest.php`

- **Nível:** integração de endpoint para a edição de registros (`getOne` e `update`).
- **Cobertura:** 42 cenários (32 de `Update` e 10 de `GetOne`), 20 com exceção. É o arquivo YAML com mais cenários.
  As exceções esperadas são "Unauthorized" (8), "Valid Timezone Offset and Timezone Name Must Be Provided" (4),
  "Record Not Found" (2), "Punch-In Overlap Found" (2), "Punch Out Time Should Be Later Than Punch In Time" (2),
  "Punch-Out Overlap Found" (1) e "Punch Out Date And Time Should Not Be Null" (1).
- **Técnica:** cenários do YAML usam *hooks* (`preAssertValidTestCase`, por exemplo `enableUserCanModifyAttendancePreHook`)
  que inserem no banco regras da máquina de estados (`WorkflowStateMachine`) para um papel. Isso permite testar a
  permissão de edição por papel, mas as transições de estado não são modeladas de forma explícita.
- **Legibilidade:** os dois *hooks* são quase idênticos (mudam só o papel), o que sugere duplicação.

## 3. Pontos fortes do conjunto

- O padrão orientado a dados facilita acrescentar cenários, e os nomes dos cenários descrevem a intenção.
- Boa cobertura de **erros e permissões**: das 175 linhas de cenários, 68 esperam exceção. As mensagens mais
  frequentes são "Unauthorized" (23), "Punch Out Time Should Be Later Than Punch In Time" (9) e "Invalid
  Parameter" (7).
- O relógio é simulado (`registerMockDateTimeHelper`), então os testes não dependem da hora real.
- Há testes de concorrência no DAO.

## 4. Lacunas

1. **Nenhum teste unitário puro.** `AttendanceService` (802 linhas) não tem classe de teste própria; só é exercitado
   indiretamente pelos testes de API e de DAO, e sempre com banco.
2. **Nenhum teste de sistema (ponta a ponta) e nenhum teste de front-end** para as telas de Punch In/Out.
3. **Máquina de estados implícita.** Os estados `INITIAL`, `PUNCHED IN` e `PUNCHED OUT` não são testados por
   cobertura de transições; a ordem Punch In → Punch Out só aparece de forma espalhada nos cenários.
4. **Valores-limite não aparecem de forma sistemática.** Nos YAML não encontrei horários `00:00` nem `23:59`.
   Não verifiquei todos os cenários um a um (por exemplo, saída igual à entrada), então isso fica como hipótese a
   confirmar. Os limites reais dependem de conferir o código e o sistema, o que fica para o plano (#16).
5. **Oráculo só na resposta.** O oráculo padrão não confere o estado final no banco depois de criar ou atualizar um
   registro.
6. **Testes "não implementado" inflam a contagem** (`testDelete`, `testUpdate` e `getValidationRuleFor*` em várias
   classes).
7. **Teste de entidade com valores inválidos** (seção 2.3).

## 5. Material de terceiros

Busca feita em 16/11/2026 por testes automatizados de Attendance/Punch In/Out do OrangeHRM. Dos resultados, li dois
repositórios; os demais apareceram nas buscas mas **não foram abertos** e por isso não são avaliados aqui.

### 5.1 `natashacallula/orangeHRM` (Python, Selenium e unittest) — arquivo `module_time.py`

Fonte: <https://github.com/natashacallula/orangeHRM> (último push em 17/10/2024). O repositório é um trabalho de
bootcamp em grupo; a autora cuidou do módulo Time. O arquivo tem 392 linhas e 10 métodos de teste. O de Attendance
mais relevante é `test_3_punch_in_out`.

- **Nível e técnica:** sistema, pela interface, no servidor de demonstração, com login Admin/admin123.
- **O que `test_3_punch_in_out` faz:** abre Time › Attendance › Punch In/Out, clica em Punch In, espera 15 s, clica
  em Punch Out, espera 10 s e verifica o texto `* Required`.
- **Oráculo:** fraco. A asserção confere o texto `* Required`. No trecho lido não há verificação do registro
  gravado (estado, horário ou presença em My Records), então o teste não demonstra que o punch in/out foi gravado.
- **Independência:** fraca. Roda no demo compartilhado e, no código lido, não desfaz o que cria. Que o resultado
  dependa de já existir um punch in aberto é uma inferência minha, não testada.
- **Robustez:** o arquivo tem **81 chamadas a `time.sleep`** em vez de esperas explícitas, e localizadores XPath
  longos e frágeis, baseados em posição (`li[2]`, `div[3]`). Isso torna o teste lento e quebradiço.
- **Dados e legibilidade:** nos métodos que li, credenciais e URL são repetidas dentro de cada teste; o `setUp` só
  abre o navegador. A classe se chama `TestLogin` embora trate de Time. O README lista passos de instalação, mas o
  passo 4 ("Run the Automation Tests") não traz o comando.
- **Cobertura:** o teste de punch in/out só clica em Punch In e Punch Out. Nele não vi caso de erro, sobreposição,
  nota, fuso nem edição de registros.

### 5.2 `bg-playground/TestingDemo-02` (Playwright e testes manuais)

Fonte: <https://github.com/bg-playground/TestingDemo-02> (último push em 06/03/2026).

- **Automação:** `automated-testing/tests/` tem `api/api.spec.js` e `ui/admin.spec.js`, `ui/auth.spec.js` e
  `ui/pim.spec.js`. **Não há especificação de Time nem de Attendance.**
- **Manual:** `manual-testing/03-test-cases.md` tem a seção "Time Module Test Cases" (`TC-TIME-001` em diante), com
  casos de timesheet (criar, submeter, aprovar). Pelos trechos que li, é timesheet, não Attendance. Não vi casos de
  Punch In/Out nesse arquivo.
- **Conclusão:** não aproveitável para a jornada de Punch In/Out, mas é um bom exemplo de documentação de teste
  manual.

### 5.3 Conclusão sobre terceiros

O único material automatizado que encontrei e que toca Punch In/Out é o de Natasha Callula, com um único caso (punch
in seguido de punch out) e oráculo fraco. Isso reforça a lacuna de testes de sistema bem projetados para a jornada.

## 6. Execução da suíte original (16/11/2026)

Ambiente: `eduardo-hoffmann/ambiente/` (Docker), clone no commit `56e23b3`, PHP 8.3.36, MariaDB 10.11.19,
PHPUnit 9.6.16.

| Comando | Resultado | Tempo |
|---|---|---|
| `phpunit --testsuite Attendance` | **226 testes, 525 asserções, 0 falhas**; 10 marcados como *risky* | 1 min 33 s (2ª execução; a 1ª não foi cronometrada) |
| `phpunit .../test/Entity` | 1 teste, 4 asserções, passou | – |
| `phpunit .../test/Dao` | 10 testes, 69 asserções, 0 falhas; os 10 *risky* | – |

Os 71 métodos expandem para 226 testes porque os *data providers* geram um teste por cenário do YAML.

Os 10 *risky* são todos de `AttendanceDaoTest` e têm a mesma causa: "This test printed output", por um aviso de
depreciação do PHP 8.3, `Creation of dynamic property ...AttendanceDaoTest::$fixture` (linha 64). Não são falhas de
lógica, mas mostram que a classe não declara a propriedade `$fixture`.

**O que a execução confirmou:**
- `AttendanceRecordTest` passa gravando os estados `'PUNCH IN'`/`'PUNCH OUT'`: a entidade aceita valores diferentes
  das constantes `PUNCHED IN`/`PUNCHED OUT`, e o teste não percebe.
- `AttendanceDaoTest` passa com a asserção repetida das linhas 77 e 78, ou seja, a repetição passa despercebida.
  Que uma delas devesse conferir `getPunchInUtcTime()` continua sendo leitura minha, não algo que a execução prove.

## 7. O que ainda falta nesta issue (#15)

- [ ] **Explorar** Time › Attendance no servidor de demonstração (campos, estados e mensagens), para confirmar as
      mensagens e os limites vistos no código.
- [ ] Avaliar com mais cuidado a hipótese sobre valores-limite (por exemplo, saída igual à entrada), conferindo os
      cenários um a um.

## 8. Fontes consultadas

- Código original, commit `56e23b3`: `src/plugins/orangehrmAttendancePlugin/` (`test/`, `Service/AttendanceService.php`,
  `entity/AttendanceRecord.php`, `Api/`), `phpunit.xml` e `src/test/phpunit/Util/EndpointIntegrationTestCase.php`.
- <https://github.com/natashacallula/orangeHRM> (`module_time.py`, `README.md`).
- <https://github.com/bg-playground/TestingDemo-02> (`automated-testing/`, `manual-testing/03-test-cases.md`).
