# Testes de integração (Java + REST Assured): PIM › Add Employee

Issue #14. Casos IT-01 a IT-12 de [`../docs/casos-de-teste.md`](../docs/casos-de-teste.md), contra a API REST v2
do OrangeHRM. Java 17+, Maven, JUnit 5, REST Assured 5.5, AssertJ.

## Como rodar

```bash
mvn test                                                           # ambiente local (http://localhost:8200)
mvn test -Pdefeitos                                                # só os testes que expõem D1 e D2 (esperado falhar)
mvn test -Dohrm.baseUrl=https://opensource-demo.orangehrmlive.com/web/index.php -Dohrm.password=admin123
mvn test -Dtest=CreateLoginApiTest                                 # uma classe
```

Configuração completa em [`../docs/ambiente.md`](../docs/ambiente.md), seção 6.

## Estrutura

| Arquivo | Conteúdo |
|---|---|
| `CreateEmployeeApiTest` | IT-01 (fluxo principal), IT-02 (obrigatórios), IT-03 (30/31 nos nomes), IT-04 (ID duplicado e 10/11), IT-12 (D1, com controle sem foto) |
| `EmployeePictureApiTest` | IT-05 (foto gravada), IT-06 (tipos e extensões proibidos), IT-07 (1 MB − 1, 1 MB, 1 MB + 1 e tamanho declarado falso) |
| `CreateLoginApiTest` | IT-08 (usuário vinculado), IT-09 (username 4/5/40/41 e existente), IT-10 (classes I13–I20, 9 e 64 caracteres, 8 caracteres expõe D2), IT-11 (R1) |
| `support/OrangeHrmSession` | login pelo formulário (token CSRF + `/auth/validate`); a API aceita o cookie de sessão |
| `support/EmployeeApi`, `UserApi` | clientes finos que registram o que criam e excluem no `@AfterEach` |
| `support/Images` | gera PNG válido com tamanho exato em bytes (valor-limite de 1 MB) |
| `support/ApiAssertions` | 422 com exatamente os `invalidParamKeys` esperados |

## Técnicas

- **Classes de equivalência e valor-limite** com `@ParameterizedTest` (`@CsvSource`, `@MethodSource`): cada
  classe inválida tem um representante e cada limite é testado dos dois lados.
- **Oráculo duplo nos casos inválidos:** o 422 aponta exatamente o parâmetro recusado e o total de funcionários
  não muda.
- **Independência:** dados únicos por execução (`TestData`) e limpeza dos funcionários e usuários criados.

## Resultado (16/11/2026, OrangeHRM 5.9 local)

**41 testes: 39 passam e 2 falham.** Os 2 ficam em quarentena com `@Tag("defeito-conhecido")`: fora do
`mvn test` (verde, 39 testes) e dentro do `mvn test -Pdefeitos`. São exatamente os que expõem os defeitos de
`../docs/defeitos.md`:

| Teste | Defeito |
|---|---|
| `CreateEmployeeApiTest.it12_comFotoAvancaIdSugerido` | **D1**: o formulário sugere de novo o ID usado no cadastro com foto |
| `CreateLoginApiTest.it10_senhaDe8Caracteres` | **D2**: senha de 8 caracteres que cumpre as regras → 422 |

Dois riscos da leitura do código foram resolvidos por estes testes:

- **R3 descartado (IT-07):** o limite de 1 MB também é validado no servidor, inclusive contra tamanho declarado falso.
- **R1 caracterizado (IT-11):** com a 2ª requisição recusada, o funcionário fica criado sem usuário. O teste
  descreve o comportamento atual e falhará se o sistema passar a desfazer o cadastro.
