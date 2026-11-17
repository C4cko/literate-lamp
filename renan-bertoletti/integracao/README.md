# Testes de integração (PHPUnit): Claim

Issue #7. Casos IT-01 a IT-26 do [plano de testes](../plano/plano-de-testes-claim.md) (seção 6.2). O compose
([`../ambiente/docker-compose.yml`](../ambiente/docker-compose.yml)) monta esta pasta em
`src/plugins/orangehrmClaimPlugin/test/T2ClaimIntegracao/`, no namespace `OrangeHRM\Tests\Claim\T2ClaimIntegracao`.

Nível: **integração**. Kernel, serviços, `UserRoleManager` e máquina de estados reais; banco de teste MariaDB
carregado com a fixture `fixtures/T2Claim.yaml` antes de **cada** caso.

| Arquivo | Casos | Técnica |
|---|---|---|
| `ClaimIntegrationTestCase.php` | base: monta o endpoint com o usuário autenticado, valida, executa e traduz exceções em código HTTP; lê o estado direto da tabela | – |
| `fixtures/T2Claim.yaml` | 4 usuários (Admin, ESS, supervisor do ESS, ESS sem vínculo); uma reivindicação por estado de partida, mais as atribuídas pelo Admin e pelo supervisor, a do próprio Admin e uma sem despesas | – |
| `StateMachineTest.php` | IT-01 a IT-11 (caminhos TT-01 a TT-09b), IT-13 (*sneak paths* SP-01 a SP-22) e IT-14 (ações por papel em cada estado: 18 combinações) | árvore de transição, *sneak paths*, tabela de estados |
| `ResultingStatePriorityTest.php` | IT-12: Admin submete a própria reivindicação, com as linhas da máquina de estados na ordem padrão e invertida | guiado por risco (R8) |

## Oráculo

Cada ação é conferida em três pontos:

1. **Resposta:** código HTTP e estado no corpo.
2. **Banco:** `status` e `submitted_date` relidos de `ohrm_claim_request` por SQL, sem passar pela API nem pelo
   Doctrine. É a lacuna apontada em #5 nos testes originais, que só conferem a resposta.
3. **Ações disponíveis:** `meta.allowedActions`, que decide os botões da tela.

Nos casos proibidos (IT-13), o teste exige que o banco fique **idêntico** ao de antes da chamada.

## Como rodar

A partir de `../ambiente`, com o ambiente pronto ([`../README.md`](../README.md), seção 1):

```powershell
docker compose exec phpunit ./src/vendor/bin/phpunit --group T2ClaimIntegracao --testdox
```

## Resultado (17/11/2026, commit `56e23b3b`)

**53 testes, 162 asserções, todos passaram.** Execução repetida em seguida com o mesmo resultado. A suíte `Claim`
completa (255 testes originais + 35 unitários + 53 de integração = 343) teve uma única falha, a do UT-04 (R2,
quarentena), o que mostra que os nossos testes não interferem nos originais.

| Item | Resultado |
|---|---|
| Transições W1–W12 | as 12 confirmadas, com estado e data de submissão gravados no banco |
| `submittedDate` | gravado no SUBMIT, preservado no CANCEL e **atualizado na ressubmissão** (IT-04) |
| *Sneak paths* | 21 recusados com 403 e 1 com 422 (ação inexistente); banco inalterado em todos |
| Ações por papel (IT-14) | as 18 combinações batem com a seção 2.5 do plano, inclusive as 9 "n. o." |
| R1, R7 | caracterizados: Admin aprovando ou submetendo leva a PAID, inclusive o rascunho do funcionário |
| R6 | confirmado: o supervisor não tem ação em APPROVED (SP-10 → 403) |
| **R8** | **descartado:** com as linhas invertidas, o Admin submetendo a própria reivindicação continua indo para SUBMITTED. O empate é resolvido pela coluna `priority` (ESS = 10, ADMIN = 0) em `BasicUserRoleManager::getUniqueActionsBasedOnPriority()`, e não pela ordem das linhas, como o plano supunha. |

## Decisões e cuidados

- **Cache estático de transições.** `AccessFlowStateMachineService` guarda as transições num cache **estático**, e
  `EmployeeClaimRequestAPI::getAllowedActions()` altera essas entidades (troca `action` de `'1'` para `'SUBMIT'`).
  No servidor, cada requisição é um processo novo e o problema não aparece; no PHPUnit, tudo roda num processo só,
  e a primeira versão de IT-04 falhou com 422 depois de consultar as ações disponíveis. O teste agora zera o cache
  (`resetWorkflowCache()`) e o *identity map* do Doctrine no início de cada chamada, como numa requisição real.
  **Não é defeito do sistema**, mas é uma armadilha para quem testa a API em sequência no mesmo processo.
- **Fixture restaurada.** IT-12 altera a tabela de transições; o `tearDown` a recarrega para não afetar as outras
  classes.
- **Data fixa por chamada.** Cada ação recebe a própria data simulada, o que permite verificar a troca do
  `submittedDate` entre a submissão e a ressubmissão.
