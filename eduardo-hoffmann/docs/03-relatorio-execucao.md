# Relatório da Execução dos Testes: Time › Attendance (Punch In / Punch Out)

> Issue: #17. Plano: [`02-casos-de-teste.md`](02-casos-de-teste.md). Jornada: [`01-jornada.md`](01-jornada.md).
> Execução em 16/11/2026, no commit `56e23b3b09e7af29317a0943523b825843fff527` de `orangehrm/orangehrm`.

## 1. Resumo

| Nível | Ferramenta | Casos do plano | Testes executados | Resultado |
|---|---|---|---|---|
| Unitário | PHPUnit | UT-01 a UT-05 | 40 | 40 passaram |
| Integração | PHPUnit + MariaDB | IT-01 a IT-11 | 27 | 26 passaram, **1 falhou de propósito (IT-10, defeito R1)** |
| Sistema | Java, JUnit 5 e Selenium | ST-01 a ST-09 | 11 | 11 passaram |
| **Total** | | **25 casos** | **78** | **77 passaram, 1 defeito confirmado** |

Um caso do plano vira mais de um teste quando usa valores diferentes (por exemplo, UT-01 tem 5 valores de margem).

**Defeito encontrado: 1** (D1, seção 4). **Observações sobre o comportamento:** 2 (seção 5).

## 2. Ambiente

| Item | Valor |
|---|---|
| Sistema sob teste | OrangeHRM 5.9, commit `56e23b3b` (código no clone local) e imagem `orangehrm/orangehrm:5.9` (instância local, `http://localhost:8201`) |
| Unitário e integração | PHP 8.3.36, MariaDB 10.11.19, PHPUnit 9.6.16, em Docker (`eduardo-hoffmann/ambiente`) |
| Sistema | Windows 11, Java 23.0.2 (compilação para 17), Maven 3.9.16, Selenium 4.52.0, JUnit 5.11.4, Chrome 157.0.8103.51 com `chromedriver` 157.0.8103.78, fuso America/Sao_Paulo |
| Servidor de demonstração | usado só para **leitura** na análise (#15); nenhum teste foi executado nele |

## 3. Como foi executado

```bash
# unitários e integração (em eduardo-hoffmann/ambiente)
docker compose exec phpunit ./src/vendor/bin/phpunit --group T2Attendance --exclude-group defeito-conhecido   # 66 testes
docker compose exec phpunit ./src/vendor/bin/phpunit --group defeito-conhecido                                # 1 teste (IT-10)

# sistema (em eduardo-hoffmann/sistema)
mvn test                                                                                                      # 11 testes
```

| Execução | Resultado | Tempo |
|---|---|---|
| PHPUnit, testes que devem passar | OK, 66 testes, 125 asserções | cerca de 27 s |
| PHPUnit, quarentena | 1 teste, 1 falha (IT-10) | cerca de 15 s |
| Maven, sistema (3 execuções seguidas) | 11 testes, 0 falhas, `BUILD SUCCESS` nas três | 76 a 79 s cada |

Depois de corrigir erros de sincronização do meu próprio código de teste (cliques antes de a página carregar e um
seletor impreciso), os 11 testes de sistema passaram em 3 execuções consecutivas, sem instabilidade.

## 4. Defeito encontrado

### D1: o endpoint de Punch In/Out não verifica a permissão do papel (risco R1)

- **Onde:** `MyAttendanceRecordAPI::userAllowedPunchInActions` e `userAllowedPunchOutActions`
  (`src/plugins/orangehrmAttendancePlugin/Api/MyAttendanceRecordAPI.php`).
- **O que acontece:** os dois métodos calculam `$allowed = in_array(..., $allowedActions)`, mas **não usam o
  resultado e não lançam exceção**. A classe-mãe, `EmployeeAttendanceRecordAPI`, lança `ForbiddenException` quando a
  ação não é permitida; o método sobrescrito em `MyAttendanceRecordAPI` não.
- **Como foi confirmado (IT-10, `PermissaoDePunchTest`):** com o banco de teste sem nenhuma linha de fluxo de
  Attendance para o papel ESS (portanto sem a ação *Punch In*), `POST /api/v2/attendance/records` por um usuário ESS
  **foi aceito e gravou 1 registro**. O esperado era `Forbidden` e nenhum registro.
- **Resultado esperado × obtido:** esperado `ForbiddenException`, 0 registros; obtido sucesso, 1 registro `PUNCHED IN`.
- **Alcance:** na instalação limpa o papel ESS **tem** as ações de Punch In e Punch Out, então o defeito só aparece
  quando um administrador retira essas ações do fluxo. O comportamento é inconsistente com o de
  `EmployeeAttendanceRecordAPI`, que respeita a permissão. A gravidade é um julgamento que cabe a quem mantém o
  produto; aqui o fato confirmado é que a checagem não tem efeito.
- **Situação nos testes:** o IT-10 fica em quarentena (`@group defeito-conhecido`) e falha de propósito.

## 5. Observações sobre o comportamento (não são defeitos)

| ID | Observação | Onde |
|---|---|---|
| O1 | **Saída igual à entrada é aceita.** A regra só rejeita entrada posterior à saída (`punchIn > punchOut`), então um registro com duração zero é gravado. Resolve o risco R3, que estava "a confirmar". | IT-07 (`testIT07SaidaIgualAEntradaComportamentoObservado`) |
| O2 | **Na tela, apagar a hora faz sumir o erro "Required" da data**, que só volta (junto com o da hora) quando se clica em **In**. Nenhum registro é gravado em nenhum dos dois casos. | ST-04 |

## 6. Resultado por caso

### Unitários (PHPUnit)

| Caso | Testes | Resultado | O que confirmou |
|---|---|---|---|
| UT-01 | 5 | passaram | margem de 180 s: 0, 179 e -179 s aceitos; 180 e -180 s recusados |
| UT-02 | 3 | passaram | opção ligada aceita qualquer hora; desligada aceita 120 s e recusa 180 s com "Provided Date And Time Invalid" |
| UT-03 | 14 | passaram | nota de 1, 250, vazia e nula aceita, 251 recusada; `00:00` e `23:59` aceitas; `25:00`, `10:60` e texto recusados; datas inexistentes recusadas |
| UT-04 | 9 | passaram | offset -12, 0, 5,5 e 14 aceitos; -12,01, 14,01 e texto recusados; `America/Sao_Paulo` aceito e `Foo/Bar` recusado |
| UT-05 | 9 | passaram | as 9 mensagens de `AttendanceServiceException` |

### Integração (PHPUnit + banco real; oráculo = registro gravado)

| Caso | Resultado | O que confirmou |
|---|---|---|
| IT-01 | passou | T1: primeira entrada grava `PUNCHED IN`, UTC convertido (10:00 em +05:30 vira 04:30), nota e fuso |
| IT-02 | passou | T2: saída conclui o mesmo registro e não altera a entrada |
| IT-03 | passou | T3: nova entrada depois de saída cria **outro** registro; o antigo não muda |
| IT-04 | passou | T4: entrada com registro aberto é recusada e nada é gravado |
| IT-05 | passou | T5 e T6: saída sem entrada aberta é recusada, sem registro e com registro concluído |
| IT-06 | passou | fronteiras da sobreposição: recusa em `in`, `in+1 min` e `out-1 min`; aceita em `in-1 min` e `out` |
| IT-07 | passou | saída 1 min antes da entrada recusada; 1 min depois aceita; **igual aceita (O1)** |
| IT-08 | passou | hora retroativa e fusos -12, +5,5 e +14 gravam o UTC correto |
| IT-09 | passou | margem de 180 s: 179 s aceito, 180 s recusado, nos dois sentidos |
| IT-10 | **falhou (D1)** | ver seção 4 |
| IT-11 | passou | a consulta de sobreposição devolve `valid` coerente com as fronteiras do IT-06 |

### Sistema (Java, JUnit 5 e Selenium, instância local)

| Caso | Testes | Resultado | O que confirmou |
|---|---|---|---|
| ST-01 | 1 | passou | **In** leva a `/attendance/punchOut`, o botão vira **Out** e grava `PUNCHED IN` |
| ST-02 | 1 | passou | **Out** grava `PUNCHED OUT`; *My Records* mostra "(1) Record Found" com Punch In e Punch Out |
| ST-03 | 2 | passaram | nota de 250 aceita; de 251 mostra "Should not exceed 250 characters" e não grava |
| ST-04 | 1 | passou | com a opção ligada, data e hora vazias mostram "Required" e impedem o envio (ver O2) |
| ST-05 | 1 | passou | hora dentro de registro concluído mostra "Overlapping Records Found"; nada é gravado |
| ST-06 | 1 | passou | dois ciclos completos criam 2 registros; *My Records* mostra "(2) Records Found" |
| ST-07 | 1 | passou | entrada retroativa grava a data e a hora digitadas |
| ST-08 | 2 | passaram | data e hora desabilitadas com a opção desligada; habilitadas com ela ligada |
| ST-09 | 1 | passou | `/attendance/punchIn` com entrada aberta redireciona para `/attendance/punchOut` |

## 7. Os testes detectam defeitos? (verificação por mutação)

Para não confiar em testes que nunca falham, alterei **o código original no meu clone** (e o restaurei depois) e rodei
os testes:

| Mutação | Testes que falharam |
|---|---|
| margem de 180 s: `<` trocado por `<=` | UT-01 (2 valores) e UT-02 (1 teste) |
| sobreposição da entrada: `lte` trocado por `lt` | IT-06 e IT-11 no caso "igual à entrada" |
| saída anterior à entrada: `>` trocado por `>=` | IT-07, caso "saída igual à entrada" |
| entrada com registro aberto deixa de ser recusada (variante `ForUpdate` do DAO) | IT-04 |

**Limites:** a primeira tentativa da última mutação atingiu o método errado (a criação usa a variante `ForUpdate`) e os
testes passaram; refiz no método certo. Os testes de sistema **não** passaram por mutação (a instância local roda numa
imagem Docker); a verificação deles foi a estabilidade em 3 execuções.

## 8. Limitações e o que não foi coberto

- **E5** (saída que engloba outro registro, "Punch-Out Overlap Found") e **R2** (concorrência na saída, já que
  `update()` não abre transação) não têm caso de teste.
- **Casos exploratórios do plano que não foram implementados:** hora `24:00` e nome de fuso vazio em UT-03 e UT-04.
- Os testes de integração usam relógio simulado e o fluxo de uma instalação limpa; não cobrem permissões de Admin
  e de Supervisor (fora do escopo do plano).
- Os testes de sistema dependem do navegador Chrome, do fuso do sistema e dos formatos da instância local
  (`yyyy-mm-dd` e hora de 12 h). Não foram executados no servidor de demonstração.
- O servidor de demonstração só foi **lido** (análise, #15), nunca alterado.

## 9. Conclusão

A jornada de registro de entrada e saída funciona conforme as regras do código: transições de estado, margem de
180 s, sobreposição, limites de nota e fuso, e a interface. **Um defeito foi confirmado (D1)**: o endpoint de
Punch In/Out deixa de verificar a permissão do papel. Os demais 77 testes passaram.

## Fontes

- [`../testes-php/`](../testes-php/) (UT e IT), [`../sistema/`](../sistema/) (ST) e [`../ambiente/`](../ambiente/).
- Análise dos testes existentes: [`../analise/01-analise-critica.md`](../analise/01-analise-critica.md).
