# Plano de Testes e Casos de Teste: Time › Attendance (Punch In / Punch Out)

> Issue: #16. Base: [01-jornada.md](01-jornada.md). Versão do código analisada: commit `56e23b3b` de
> `orangehrm/orangehrm`. Análise dos testes existentes: [`../analise/01-analise-critica.md`](../analise/01-analise-critica.md).
> Este documento **planeja** os casos. Nenhum deles foi implementado ou executado ainda (isso é a #17); as colunas
> de resultado esperado vêm das regras confirmadas no código e estão marcadas **a confirmar** quando o código não
> define o comportamento.

## 1. Plano de Testes

### 1.1 Objetivo
Verificar se o registro de entrada e saída (fluxo principal, A1–A3 e exceções E1–E9) segue as regras da jornada, e
investigar os riscos R1 e R3. O plano também ataca as lacunas da análise (#15): não há teste unitário puro nem de
sistema para Attendance, e o oráculo dos testes de endpoint existentes não confere o estado gravado no banco.

### 1.2 Escopo
- **Dentro:** `POST /api/v2/attendance/records` (entrada) e `PUT /api/v2/attendance/records` (saída) de
  `MyAttendanceRecordAPI`; `GET /api/v2/attendance/punch-in/overlaps` e `.../punch-out/overlaps`; regras de
  validação; regra de ±180 s; telas *Punch In*, *Punch Out* e *My Records*.
- **Fora:** edição e exclusão de registros, relatório-resumo, registro por Admin/Supervisor (*proxy*), salvar a
  configuração de Attendance (exceto como pré-condição dos casos A1/CA8), concorrência real (R2).

### 1.3 Níveis, ferramentas e ambiente

| Nível | Alvo | Ferramenta | Ambiente |
|---|---|---|---|
| Unitário | classes PHP do `orangehrmAttendancePlugin` e as regras de validação | **PHPUnit** (infra do próprio OrangeHRM), com dublês para o relógio e a configuração | Docker, `eduardo-hoffmann/ambiente` (já funciona) |
| Integração | API v2 de Attendance com banco real | **PHPUnit** + MariaDB, conferindo o **estado gravado** (repositório Doctrine) além da resposta | Docker, `eduardo-hoffmann/ambiente` |
| Sistema | jornada pela interface | **Java 17 + JUnit 5 + Selenium**, padrão Page Object (conforme a #17) | instância **local** do OrangeHRM 5.9 em Docker |

Por que PHPUnit nos dois primeiros níveis: o código sob teste é PHP e o ambiente já está funcionando; testar em PHP
usa o mesmo autoload, as mesmas fixtures e o mesmo banco de teste do projeto. Java entra onde a #17 pede: no sistema.

Por que o sistema roda no ambiente local e não no demo: a jornada **grava** dados (registros de ponto) e depende de
configuração (CA8, A1), e o demo é compartilhado e reiniciado. Por isso a exploração do demo na #15 foi só de leitura.

**Pendências de ambiente para a #17** (verificadas na máquina em 16/11/2026): o Maven não está instalado; e falta
acrescentar o serviço `app` (OrangeHRM 5.9) ao `docker-compose.yml`, como no ambiente do Mateus.

### 1.4 Técnicas de projeto
- **Particionamento em classes de equivalência (PE)** para data, hora, fuso e nota.
- **Análise de valor-limite (AVL):** nota (250/251), fuso (-12, 14), margem de 180 s e fronteiras do intervalo de
  sobreposição.
- **Teste de transição de estados (TE)**, com cobertura das transições T1–T6 do diagrama da jornada.
- **Tabela de decisão (TD)** para a regra da hora (configuração × diferença para o servidor × sobreposição).
- **Casos de uso (CU)** para o fluxo principal e os alternativos.
- **Teste guiado por risco** para R1 e R3.

### 1.5 Critérios
- **Entrada:** ambiente Docker de pé; banco de testes criado; instância local do OrangeHRM acessível (sistema).
- **Saída:** todos os casos de prioridade Alta executados; falhas analisadas e registradas como defeito com
  evidência, ou como comportamento esperado.
- **Oráculo:** regras confirmadas no código (seção 3 da jornada) e mensagens de
  `AttendanceServiceException` e `installer/Migration/V5_0_0/lang-string/attendance.yaml`.

### 1.6 Isolamento e dados
- Cada teste cria os próprios dados e usa fixtures próprias, sem depender de ordem. No sistema, cada teste começa
  sem registro aberto (limpeza do registro criado ao terminar).
- Datas e horas dos casos de integração e unitários são **fixas** (relógio simulado), para o resultado não depender
  da hora real.

### 1.7 Riscos do próprio plano
- O comportamento de saída igual à entrada, de `24:00` e de nome de fuso vazio **não está definido** no código lido;
  esses casos são exploratórios e o resultado esperado será fixado depois de executados.
- A tela mostra os textos na língua configurada; os casos de sistema devem conferir o texto em inglês, que é o padrão
  da instalação.
- A tela de sistema usa o formato de data configurado na instância (o demo mostrou `yyyy-dd-mm`); os testes não podem
  presumir um formato fixo.

## 2. Classes de equivalência e valores-limite

| Campo | Classes válidas | Classes inválidas | Limites testados |
|---|---|---|---|
| Nota | V1: ausente ou vazia; V2: 1–250 caracteres | I1: > 250 | 250, 251 |
| Timezone offset | V3: numérico em [-12,00; 14,00] | I2: < -12,00; I3: > 14,00; I4: não numérico | -12,00, 14,00, -12,01, 14,01 |
| Timezone name | V4: nome aceito por `DateTimeZone` (ex.: `Asia/Kolkata`) | I5: nome inexistente (ex.: `Foo/Bar`) | – |
| Hora (API) | V5: `H:i` | I6: fora de `H:i` (ex.: `25:00`) | `00:00`, `23:59`, `24:00` (a confirmar) |
| Data (API) | V6: data válida | I7: data inexistente (ex.: `2026-02-30`) | – |
| Diferença para a hora do servidor (opção desligada) | V7: \|d\| < 180 s | I8: \|d\| ≥ 180 s | 0, 179, 180, -179, -180 |
| Nova entrada × registro concluído `[in, out]` | V8: `< in` ou `>= out` | I9: `in <= nova < out` | `in-1 min`, `in`, `in+1 min`, `out-1 min`, `out` |

## 3. Teste de transição de estados

| Transição | De → Para | Evento | Resultado esperado | Casos |
|---|---|---|---|---|
| T1 | `INITIAL` → `PUNCHED IN` | Punch In | registro criado | IT-01, ST-01 |
| T2 | `PUNCHED IN` → `PUNCHED OUT` | Punch Out | registro concluído | IT-02, ST-02 |
| T3 | `PUNCHED OUT` → `PUNCHED IN` | Punch In | **novo** registro criado | IT-03, ST-06 |
| T4 | `PUNCHED IN` → (recusado) | Punch In | "Cannot Proceed Punch In Employee Already Punched In" | IT-04 |
| T5 | `PUNCHED OUT` → (recusado) | Punch Out | "Cannot Proceed Punch Out Employee Already Punched Out" | IT-05 |
| T6 | `INITIAL` → (recusado) | Punch Out | "Cannot Proceed Punch Out Employee Already Punched Out" | IT-05 |

Cobertura de transições: 6 de 6, três pela interface (T1, T2, T3) e as recusas pela API, porque a interface
redireciona antes (ver jornada, seção 3).

## 4. Tabela de decisão (hora informada)

| Regra | Opção "alterar hora" | Diferença para o servidor | Sobreposição | Resultado esperado | Casos |
|---|---|---|---|---|---|
| D-1 | ligada | qualquer | não | registro criado | IT-08, ST-07 |
| D-2 | desligada | < 180 s | não | registro criado | UT-01, IT-09 |
| D-3 | desligada | ≥ 180 s | – | "Provided Date And Time Invalid" | UT-01, UT-02, IT-09 |
| D-4 | ligada ou desligada | dentro da margem | sim | sobreposição recusada | IT-06, ST-05 |

## 5. Casos de teste

Prioridade: **A** alta, **M** média, **B** baixa. Rastreabilidade: CA = critério de aceitação; FP/A/E/R = itens da
jornada.

### 5.1 Unitários (PHPUnit)

| ID | Objetivo | Técnica | Rastreia | Prio |
|---|---|---|---|---|
| UT-01 | `MyAttendanceRecordAPI::isCurrantDateTimeValid` (protegido, testado por subclasse ou reflexão) com relógio simulado: diferença de 0, 179 e -179 s retorna verdadeiro; 180 e -180 s retorna falso | AVL | CA8, E6 | A |
| UT-02 | `extractPunchDateTime` com a opção ligada aceita qualquer hora; desligada e hora fora da margem lança `AttendanceServiceException` "Provided Date And Time Invalid" | TD | CA8, E6 | A |
| UT-03 | Regras de validação de criação: nota com 250 aceita e com 251 rejeitada; nota ausente aceita; data inexistente e hora `25:00` rejeitadas; `00:00` e `23:59` aceitas; `24:00` exploratório | PE/AVL | CA3, E8 | A |
| UT-04 | Regras de fuso: offset -12 e 14 aceitos; -12,01, 14,01 e texto rejeitados; nome `Asia/Kolkata` aceito e `Foo/Bar` rejeitado | AVL/PE | CA9, E7 | M |
| UT-05 | `AttendanceServiceException`: cada método de fábrica produz exatamente a mensagem documentada (9 mensagens) | PE | E1–E7 | B |

### 5.2 Integração (PHPUnit + MariaDB), API v2 de Attendance

Cada caso verifica a resposta **e** o estado gravado em `ohrm_attendance_record`. Relógio simulado, usuário ESS.

| ID | Requisição | Entrada | Resultado esperado | Rastreia | Prio |
|---|---|---|---|---|---|
| IT-01 | POST records | sem registro anterior; data/hora/fuso válidos; nota "entrada" | sucesso; 1 registro com estado `PUNCHED IN`, `punchInUtcTime` convertido do fuso, nota gravada | CA1, FP, T1 | A |
| IT-02 | PUT records | registro `PUNCHED IN`; saída posterior | sucesso; mesmo registro passa a `PUNCHED OUT`, com `punchOutUtcTime` | CA2, FP, T2 | A |
| IT-03 | POST records | último registro `PUNCHED OUT`; nova entrada depois da saída | sucesso; passam a existir **2** registros, o novo `PUNCHED IN` | A3, T3 | A |
| IT-04 | POST records | último registro `PUNCHED IN` | `BadRequest` "Cannot Proceed Punch In Employee Already Punched In"; continua 1 registro | CA4, E1, T4 | A |
| IT-05 | PUT records | (a) sem nenhum registro; (b) último registro `PUNCHED OUT` | `BadRequest` "Cannot Proceed Punch Out Employee Already Punched Out" em ambos | CA5, E2, T5, T6 | A |
| IT-06 | POST records | registro concluído `[in, out]`; nova entrada em `in-1 min`, `in`, `in+1 min`, `out-1 min`, `out` | recusa ("Punch-In Overlap Found") em `in`, `in+1 min` e `out-1 min`; aceita em `in-1 min` e `out` | CA7, E4 | A |
| IT-07 | PUT records | `PUNCHED IN` às 10:00; saída às 09:59, 10:00 e 10:01 | 09:59 recusa "Punch Out Time Should Be Later Than Punch In Time"; 10:01 aceita; **10:00 a confirmar** | CA6, E3, R3 | A |
| IT-08 | POST records | opção ligada; hora retroativa; fusos -12, +5,5 e +14 | registro criado; UTC gravado = hora local menos o offset (ex.: 10:00 em +5,5 vira 04:30 UTC) | A1, A2, D-1 | M |
| IT-09 | POST records | opção desligada; hora 179 s e 180 s distante do relógio simulado | 179 s aceita; 180 s recusa "Provided Date And Time Invalid"; nada gravado | CA8, E6, D-3 | A |
| IT-10 | POST records | remover a ação `PUNCH IN` do papel ESS no banco de teste e tentar registrar entrada | esperado: `Forbidden`. **Se o sistema criar o registro, R1 se confirma como defeito** | R1 | A |
| IT-11 | GET punch-in/overlaps | mesma massa de IT-06 | `valid` falso nas datas de sobreposição e verdadeiro nas demais (é o que a tela usa) | CA7, E4 | M |

### 5.3 Sistema (Java + JUnit 5 + Selenium), pela interface

Instância local, usuário com funcionário vinculado. Cada teste termina removendo o registro que criou.

| ID | Passos | Resultado esperado | Rastreia | Prio |
|---|---|---|---|---|
| ST-01 | Abrir *Punch In*, deixar data/hora, clicar em **In** | aviso de sucesso; a tela passa a ser *Punch Out* (URL `/attendance/punchOut`) | CA1, FP, T1 | A |
| ST-02 | Depois de ST-01, clicar em **Out** | aviso de sucesso; em *My Records* aparece um registro com entrada e saída | CA2, FP, T2 | A |
| ST-03 | Abrir *Punch In*, escrever uma nota de 250 e outra de 251 caracteres | 250: aceita; 251: mensagem de limite de caracteres no campo e botão sem gravar | CA3, E8 | M |
| ST-04 | Abrir *Punch In* com opção ligada, apagar a data e depois a hora | mensagem "Required" em cada campo | E9 | M |
| ST-05 | Com um registro concluído, abrir *Punch In* (opção ligada) e informar hora dentro do intervalo dele | mensagem "Overlapping Records Found" no campo de hora | CA7, E4, D-4 | A |
| ST-06 | Fazer entrada e saída; depois nova entrada | o segundo ciclo cria outro registro; *My Records* mostra 2 | A3, T3 | M |
| ST-07 | Ligar a opção de alterar hora; informar uma data/hora retroativa e registrar entrada | registro criado com o horário informado | A1, D-1 | M |
| ST-08 | Desligar a opção; abrir *Punch In* | campos Date e Time desabilitados | CA8 | A |
| ST-09 | Estando `PUNCHED IN`, abrir diretamente `/attendance/punchIn` | redirecionado para `/attendance/punchOut` | CA4 (via UI) | M |

## 6. Matriz de rastreabilidade (resumo)

| Item da jornada | Casos |
|---|---|
| CA1 (entrada) | IT-01, ST-01 |
| CA2 (saída) | IT-02, ST-02 |
| CA3 (nota ≤ 250) | UT-03, ST-03 |
| CA4 (entrada com registro aberto) | IT-04, ST-09 |
| CA5 (saída sem registro aberto) | IT-05 |
| CA6 (saída antes da entrada) | IT-07 |
| CA7 (sobreposição) | IT-06, IT-11, ST-05 |
| CA8 (hora fixa pela configuração) | UT-01, UT-02, IT-09, ST-08 |
| CA9 (fuso inválido) | UT-04 |
| A1 / A2 / A3 | IT-08, ST-07 / IT-08 / IT-03, ST-06 |
| E1–E9 | IT-04, IT-05, IT-07, IT-06, (E5: **sem caso**), IT-09, UT-04, UT-03/ST-03, ST-04 |
| R1 / R3 | IT-10 / IT-07 |
| R2 | sem caso (limitação do plano) |

Lacunas assumidas deste plano: **E5** (saída que engloba outro registro, "Punch-Out Overlap Found") e **R2**
(concorrência na saída) não têm caso. E5 pode ser acrescentado na #17 se houver tempo.

Contagem: **5 unitários, 11 de integração e 9 de sistema (25 casos)**.

## Fontes

- [`01-jornada.md`](01-jornada.md) e as fontes citadas nela.
- `src/plugins/orangehrmAttendancePlugin/test/` (padrão dos testes existentes) e
  `src/test/phpunit/Util/EndpointIntegrationTestCase.php`.
- Formato dos documentos: [`../../mateus-neubarth/docs/casos-de-teste.md`](../../mateus-neubarth/docs/casos-de-teste.md).
