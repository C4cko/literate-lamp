# Defeitos e comportamentos encontrados: Claim

> Issue #7. Riscos levantados no [plano de testes](../plano/plano-de-testes-claim.md) (seção 2.7) e verificados pelos
> testes dos três níveis. Código: `orangehrm/orangehrm`, commit `56e23b3b`. Sistema: servidor de demonstração
> (testes de sistema) e banco de teste local (PHPUnit), em 17/11/2026.

## Resumo

| ID | Título | Severidade | Status | Origem |
|---|---|---|---|---|
| **D1** | Editar uma despesa aceita valor 0, que a criação recusa | Baixa | **Confirmado nos 3 níveis**: UT-04, IT-18 e ST-13 falham | Risco R2, valor-limite 0 |

Os demais riscos não são defeitos, porque não há requisito que os contradiga; o resultado de cada um está na
seção "Outros riscos".

## D1: editar uma despesa aceita valor 0, que a criação recusa

**Versão:** código no commit `56e23b3b`; servidor de demonstração em 17/11/2026.

### Passos para reproduzir (pela tela)

1. Entrar como funcionário (ESS) e abrir uma reivindicação **INITIATED** com uma despesa de 10,50.
2. Na lista *Expenses*, clicar no ícone de edição (lápis) da despesa.
3. Trocar **Amount** para `0` e clicar em *Save*.

**Resultado esperado:** a despesa é recusada, como acontece ao **criar** uma despesa com valor 0 (CA7 do plano: o
valor mínimo é 0,01).
**Resultado obtido:** a tela não mostra mensagem de validação, envia a alteração e o servidor a grava: a despesa
passa a valer 0,00 (conferido pela API depois do *Save*).

**Controle:** criar uma despesa nova com valor 0 é recusado pela API (IT-17, ST-10), o que mostra que a regra
existe e só falta na edição.

### Evidência

| Nível | Teste | Resultado | Arquivo |
|---|---|---|---|
| Unitário | `ClaimValidationRulesTest::testUT04ZeroNaEdicaoDeveSerRecusadoComoNaCriacao` | regra de edição aceita `amount = 0` | [`phpunit/T2ClaimUnitario.xml`](phpunit/T2ClaimUnitario.xml) |
| Integração | `InputValidationTest::testIT18EdicaoParaZeroDeveSerRecusada` | `PUT /api/v2/claim/requests/1/expenses/1` com `amount: 0` → **200** (esperado 422) | [`phpunit/T2ClaimIntegracao.xml`](phpunit/T2ClaimIntegracao.xml) |
| Sistema | `ClaimValidationSystemTest.st13EdicaoParaZero` | `expected: <10.5> but was: <0.0>`: o valor gravado passou a 0 | [`sistema/quarentena-ClaimValidationSystemTest.txt`](sistema/quarentena-ClaimValidationSystemTest.txt) |

### Causa (leitura do código)

`src/plugins/orangehrmClaimPlugin/Api/ClaimExpenseAPI.php`:

```php
// getValidationRuleForCreate()
new ParamRule(self::PARAMETER_AMOUNT,
    new Rule(Rules::POSITIVE),                       // recusa 0
    new Rule(Rules::BETWEEN, [0, 9999999999.99]))

// getValidationRuleForUpdate()
new ParamRule(self::PARAMETER_AMOUNT,
    new Rule(Rules::BETWEEN, [0, 9999999999.99]))    // aceita 0
```

A expressão regular aplicada nos dois caminhos (`/^\d+(\.\d{1,2})?$/`) também aceita `0`. Na tela, o modal de
edição (`EditExpenseModal.vue`) valida obrigatório, número, máximo e 2 casas decimais, e nenhuma dessas regras
recusa 0.

**Correção sugerida:** acrescentar `new Rule(Rules::POSITIVE)` à regra de `amount` em
`getValidationRuleForUpdate()`. Quando isso for feito, os três testes passam e podem sair da quarentena.

**Severidade baixa:** não há perda de dados, e quem aprova vê o valor 0. Mas uma despesa de valor 0 não faz sentido
num reembolso, e a mesma regra se comporta diferente conforme o caminho.

## Outros riscos

| Risco | Resultado | Leitura |
|---|---|---|
| R1: Admin aprovar leva direto a PAID; reivindicação atribuída e submetida pelo Admin nasce PAID | **Caracterizado** (IT-03, IT-08, ST-03, ST-05) | É o que a migração cadastra (W5, W7). Não há etapa de pagamento separada quando o Admin aprova; nenhum documento do OrangeHRM diz se isso é intencional. |
| R3: submeter sem despesas | **Caracterizado** (IT-23, ST-09): a API e a tela aceitam | A tela oferece *Submit* com total 0,00. Não é defeito pelo código, mas é uma lacuna de regra de negócio: uma reivindicação vazia chega ao gestor. |
| R4: número de referência com mais de 7 dígitos de id | **Caracterizado** (UT-01) | Com o id 10 000 000, o número passa a ter 16 caracteres. Sem requisito de tamanho fixo. |
| R5: data do número de referência em UTC | **Caracterizado** (UT-02, #6) | O prefixo usa a data do servidor (UTC na demo). Entre 21h e 0h de Brasília, a data do número é a do dia seguinte. |
| R6: supervisor não rejeita depois de aprovar | **Confirmado** (IT-13 SP-10, IT-14, ST-07) | Coerente com a migração; só o Admin rejeita uma reivindicação APPROVED. |
| R7: gestor submete o rascunho do funcionário | **Caracterizado** (IT-09, IT-11) | Admin e supervisor podem submeter uma reivindicação que o funcionário ainda está editando; com o Admin, ela vai para PAID. |
| R8: estado resultante ambíguo para quem tem dois papéis | **Descartado** (IT-12) | O empate é resolvido pela coluna `priority` (`BasicUserRoleManager::getUniqueActionsBasedOnPriority`), e não pela ordem das linhas. A hipótese do plano estava errada. |

## Achados sobre testabilidade (não são defeitos do sistema)

| Achado | Onde | Impacto para quem testa |
|---|---|---|
| Cache **estático** de transições (`AccessFlowStateMachineService`), cujas entidades são alteradas por `EmployeeClaimRequestAPI::getAllowedActions()` | integração | Num mesmo processo, consultar as ações disponíveis e depois executar uma ação dá 422. Em produção não acontece (cada requisição é um processo novo). Solução no teste: `resetWorkflowCache()` a cada chamada. |
| Servidor de demonstração reiniciado e lento durante a execução | sistema | Dados e sessões apagados no meio da execução; páginas presas. Solução: dados por teste, logout real e limite de carregamento de página (ver [`../sistema/README.md`](../sistema/README.md)). |
