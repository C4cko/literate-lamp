# Execução dos testes: Claim

> Issue #7. Casos do [plano de testes](../plano/plano-de-testes-claim.md) (#6). Defeitos e riscos:
> [`defeitos.md`](defeitos.md).

## Resultado (17/11/2026)

| Nível | Ferramenta | Onde rodou | Execução padrão | Quarentena (`defeito-conhecido`) |
|---|---|---|---|---|
| Unitário (UT-01 a UT-07) | PHPUnit 9.6 | Docker local, commit `56e23b3b` | **34 testes, 47 asserções: todos passaram** | 1 teste, **falha** (UT-04: D1) |
| Integração (IT-01 a IT-26) | PHPUnit 9.6 + MariaDB 10.11 | Docker local, commit `56e23b3b` | **109 testes, 260 asserções: todos passaram** | 1 teste, **falha** (IT-18: D1) |
| Sistema (ST-01 a ST-13) | Java 21, Selenium 4.27, JUnit 5.11, Chrome 157 | servidor de demonstração | **17 testes: todos passaram** (12 min 14 s) | 1 teste, **falha** (ST-13: D1) |
| **Total** | | | **160 testes, todos passaram** | **3 testes, todos expõem o D1** |

Os testes de quarentena falham **de propósito**: descrevem o comportamento correto e passarão quando o D1 for
corrigido. Ficam fora da execução padrão para que uma falha nova não se esconda atrás de uma já conhecida.

A suíte `Claim` original (255 testes) rodou junto com os nossos unitários e com a integração da máquina de
estados, num total de 343 testes, com uma única falha, a do UT-04: os nossos testes não interferem nos originais.

## Cobertura do plano

| Critério (plano, seção 5.4) | Resultado |
|---|---|
| Transições W1–W12 | **12/12** na integração (IT-01 a IT-11) e **10/12** no sistema (W6 e W8 só na integração, como planejado) |
| Pares (estado, papel) de *sneak paths* | **18/18** (IT-13: 22 casos, banco inalterado em todos) |
| Ações por papel e estado (seção 2.5) | **18/18** combinações conferidas (IT-14), inclusive as 9 não observadas na #6 |
| Critérios de aceitação CA1–CA8 | todos com pelo menos um caso passando na integração e no sistema |
| Casos de prioridade A | todos executados |

## Evidências

| Arquivo | Conteúdo |
|---|---|
| [`phpunit/T2ClaimUnitario.xml`](phpunit/T2ClaimUnitario.xml), [`phpunit/T2ClaimUnitario-testdox.txt`](phpunit/T2ClaimUnitario-testdox.txt) | relatório JUnit e lista legível dos 35 unitários (inclui o UT-04 em quarentena) |
| [`phpunit/T2ClaimIntegracao.xml`](phpunit/T2ClaimIntegracao.xml), [`phpunit/T2ClaimIntegracao-testdox.txt`](phpunit/T2ClaimIntegracao-testdox.txt) | idem para os 110 de integração (inclui o IT-18) |
| [`sistema/TEST-t2.claim.ClaimJourneySystemTest.xml`](sistema/TEST-t2.claim.ClaimJourneySystemTest.xml), [`sistema/TEST-t2.claim.ClaimValidationSystemTest.xml`](sistema/TEST-t2.claim.ClaimValidationSystemTest.xml) e os `.txt` | relatórios do Surefire da execução padrão (01:20) |
| [`sistema/quarentena-ClaimValidationSystemTest.txt`](sistema/quarentena-ClaimValidationSystemTest.txt) | `mvn test -Pdefeitos` (01:36): ST-13 com `expected: <10.5> but was: <0.0>` |
| [`../plano/evidencias/`](../plano/evidencias/) | conferência da máquina de estados no servidor (#6) |

## Como repetir

Ver [`../README.md`](../README.md), seção 2. Em resumo:

```powershell
# PHPUnit (a partir de renan-bertoletti/ambiente, com o ambiente preparado)
docker compose exec phpunit ./src/vendor/bin/phpunit --group T2Claim --exclude-group defeito-conhecido
docker compose exec phpunit ./src/vendor/bin/phpunit --group defeito-conhecido --filter T2Claim

# Sistema (a partir de renan-bertoletti/sistema)
mvn test
mvn test -Pdefeitos
```

## Execuções anteriores do nível de sistema

Duas execuções falharam antes da oficial, por causa do ambiente e de um erro do próprio teste, e não do
OrangeHRM: troca de usuário com corrida de cookies (00:43), reinício do servidor de demonstração (00:58) e lentidão
do servidor (01:28). Causas e correções em [`../sistema/README.md`](../sistema/README.md), seção "Problemas
encontrados ao executar".
