# Testes unitários (PHPUnit): Claim

Issue #7. Casos UT-01 a UT-07 do [plano de testes](../plano/plano-de-testes-claim.md) (seção 6.1). O compose
([`../ambiente/docker-compose.yml`](../ambiente/docker-compose.yml)) monta esta pasta em
`src/plugins/orangehrmClaimPlugin/test/T2ClaimUnitario/`, no namespace `OrangeHRM\Tests\Claim\T2ClaimUnitario`.

| Arquivo | Casos | Técnica | Isolamento |
|---|---|---|---|
| `ReferenceIdTest.php` | UT-01 (id com 7 dígitos: 1, 999 999, 1 000 000, 9 999 999, 10 000 000) e UT-02 (prefixo de data na virada do ano e em 29/02) | AVL | `ClaimDao` e `DateTimeHelperService` simulados; sem banco |
| `ClaimValidationRulesTest.php` | UT-03 (valor na criação), UT-04 (valor na edição), UT-05 (1000/1001 e 200/201 caracteres), UT-06 (parâmetro `action`), UT-07 (data da despesa) | PE + AVL | regras aplicadas pelo `Validator` do framework; `ConfigService` simulado (a regra de anexo o lê no construtor); sem banco |

**Oráculo preciso:** nos casos inválidos, o teste exige que a recusa venha **exatamente** do parâmetro testado
(`assertInvalidParamException(..., ['amount'])`). Assim, um corpo mal montado não passaria como "recusa
esperada".

## Como rodar

A partir de `../ambiente`, com o ambiente pronto ([`../README.md`](../README.md), seção 1):

```powershell
docker compose exec phpunit ./src/vendor/bin/phpunit --group T2ClaimUnitario --testdox                                 # todos (35)
docker compose exec phpunit ./src/vendor/bin/phpunit --group T2ClaimUnitario --exclude-group defeito-conhecido          # 34, devem passar
docker compose exec phpunit ./src/vendor/bin/phpunit --group defeito-conhecido --filter T2ClaimUnitario                 # 1, expõe R2
```

## Resultado (17/11/2026, commit `56e23b3b`)

**35 testes, 48 asserções: 34 passaram e 1 falhou.**

| Teste | Resultado | Leitura |
|---|---|---|
| `testUT04ZeroNaEdicaoDeveSerRecusadoComoNaCriacao` | **falha** | Confirma o risco **R2**: a regra de edição (`ClaimExpenseAPI::getValidationRuleForUpdate`) aceita valor 0, que a regra de criação recusa. |
| UT-03 "zero" (criação) | passa | Controle do caso acima: o mesmo valor é recusado na criação, então a falha não vem da montagem do teste. |
| UT-01 "primeiro id com 8 dígitos" | passa | Caracteriza **R4**: com o id 10 000 000, o número de referência passa a ter 16 caracteres. Não há requisito sobre o tamanho, então o caso documenta o comportamento e não acusa defeito. |

O teste que falha não foi marcado como `skipped`: ele descreve o comportamento esperado e deve passar quando a
regra for corrigida. Fica em **quarentena** (`@group defeito-conhecido`), fora da execução padrão, como fez
Mateus Neubarth em #10.
