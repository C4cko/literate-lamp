# Testes de sistema (Java + Playwright): PIM › Add Employee

Issue #18. Casos ST-01 a ST-13 de [`../docs/casos-de-teste.md`](../docs/casos-de-teste.md), pela interface,
com Playwright for Java 1.49, JUnit 5 e o padrão Page Object.

## Como rodar

```bash
mvn test                                     # headless, ambiente local (http://localhost:8200)
mvn test -Pdefeitos                          # só os testes que expõem D1 e D2 (esperado falhar)
mvn test -Dheadless=false -DslowMo=500       # navegador visível e mais lento (para acompanhar)
mvn test -Dtest=AddEmployeeE2ETest#st03_cadastroComLogin
```

Um *trace* por teste fica em `target/traces/<teste>.zip`; ver [`../docs/ambiente.md`](../docs/ambiente.md), seção 5.

## Estrutura

| Arquivo | Conteúdo |
|---|---|
| `AddEmployeeE2ETest` | ST-01 (mínimo), ST-02 (foto), ST-03 (login em User Management), ST-04 (foto + login desativado), ST-12 (D1, com controle sem foto), ST-13 (Cancel) |
| `AddEmployeeValidationE2ETest` | ST-05 (Required), ST-06 (ID duplicado), ST-07 (tipo, 1 MB + 1 e 1 MB exato), ST-08 (username existente), ST-09 (senhas diferentes), ST-10 (política; 8 caracteres expõe D2), ST-11 (limites 30/10/5/40) |
| `pages/LoginPage`, `AddEmployeePage`, `PersonalDetailsPage`, `SystemUsersPage` | Page Objects com localizadores semânticos (placeholder, `name`, rótulo visível, papel ARIA) |
| `support/BaseE2ETest` | navegador por classe, contexto novo por teste, login, trace, apoio via API na mesma sessão e limpeza de funcionários **e** usuários |
| `support/Images` | PNG com tamanho exato, enviado com `setInputFiles` sem arquivo em disco |

## Decisões

- **A ação testada é sempre feita pela interface.** A API só prepara pré-condições (ex.: o funcionário com ID
  existente do ST-06) e confere efeitos (foto gravada, total de funcionários).
- **Toast antes do redirecionamento:** `SaveEmployee.vue` mostra "Successfully Saved" e logo navega para
  Personal Details, recarregando a página; o toast é verificado entre o `save()` e a nova URL.
- **Limpeza robusta:** o funcionário é registrado mesmo que a asserção do toast falhe, e os usuários vinculados
  são excluídos antes do funcionário.

## Resultado (16/11/2026, OrangeHRM 5.9 local)

**23 testes: 21 passam e 2 falham.** Os 2 ficam em quarentena com `@Tag("defeito-conhecido")`: fora do
`mvn test` (verde, 21 testes) e dentro do `mvn test -Pdefeitos`. São exatamente os que expõem os defeitos de
`../docs/defeitos.md`:

| Teste | Defeito |
|---|---|
| `AddEmployeeE2ETest.st12_comFotoIdSugeridoAvanca` | **D1**: depois do cadastro com foto, o formulário sugere de novo o mesmo Employee Id |
| `AddEmployeeValidationE2ETest.st10_senhaDe8Caracteres` | **D2**: "Your password meets the minimum requirements, but it could be guessable" |

Depois da execução, o sistema volta ao estado inicial (só o funcionário e o usuário Admin).
