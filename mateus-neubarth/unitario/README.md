# Testes unitários (PHPUnit): PIM › Add Employee

Issue #10. Casos UT-01 a UT-08 de [`../docs/casos-de-teste.md`](../docs/casos-de-teste.md), escritos com a
infraestrutura de teste do próprio OrangeHRM (PHPUnit 9.6, `EndpointIntegrationTestCase`, `KernelTestCase`,
fixtures YAML).

## Onde os testes ficam

A pasta `T2AddEmployee/` foi feita para viver dentro do plugin PIM, em
`src/plugins/orangehrmPimPlugin/test/T2AddEmployee/`. Assim:

- o autoload de testes (`OrangeHRM\Tests\Pim\` → `plugins/orangehrmPimPlugin/test`) encontra as classes,
  no namespace `OrangeHRM\Tests\Pim\T2AddEmployee`;
- a suíte `Pim` do `phpunit.xml` passa a incluí-los.

No ambiente local, o `docker-compose.yml` monta a pasta nesse caminho (somente leitura); nada é copiado para
o clone do OrangeHRM.

## Como rodar

Com o ambiente pronto (`../docs/ambiente.md`, seções 2 e 3), a partir de `../ambiente`:

```bash
docker compose exec phpunit ./src/vendor/bin/phpunit --group T2AddEmployee --testdox                       # todos (49)
docker compose exec phpunit ./src/vendor/bin/phpunit --group T2AddEmployee --exclude-group defeito-conhecido  # 47, devem passar
docker compose exec phpunit ./src/vendor/bin/phpunit --group defeito-conhecido                               # 2, expõem D1 e D2
```

## Arquivos

| Arquivo | Casos | Técnica | Nível real |
|---|---|---|---|
| `AddEmployeeApiTestCase.php` | base comum (cria o endpoint com usuário simulado, relógio fixo, fixtures) | – | – |
| `EmployeeCreateValidationTest.php` | UT-01 a UT-04: tamanhos dos nomes, obrigatoriedade, Employee Id, foto | PE + AVL (data providers) | componente (regras de validação com banco de teste) |
| `EmployeeCreateBehaviourTest.php` | UT-05 (contador de ID com e sem foto; foto gravada) e UT-06 (sem permissão) | teste guiado por risco; oráculo sobre o **estado do banco** | componente/integração |
| `EmployeeIdGeneratorTest.php` | UT-07: formato do ID sugerido e argumento de `incrementId` | AVL (transições de dígitos); DAO mockado | **unitário** isolado |
| `PasswordPolicyTest.php` | UT-08: uma senha por classe inválida (I13–I20) e limites 7/8/9 e 64/65 | PE + AVL; oráculo = lista completa de mensagens | componente (política lida do banco) |
| `fixtures/AddEmployee.yml` | cópia de `EmployeeAPITest.yml` + contador de IDs (`last_id = 7`) | – | – |

## Resultado (12/11/2026, commit `56e23b3b`)

**49 testes, 60 asserções: 47 passaram e 2 falharam.** As duas falhas são os defeitos documentados em
[`../docs/defeitos.md`](../docs/defeitos.md):

| Teste | Defeito |
|---|---|
| `EmployeeCreateBehaviourTest::testUT05CriarComFotoAvancaOIdSugerido` | **D1**: após cadastrar com foto, o ID sugerido continua `0008` |
| `PasswordPolicyTest::testUT08SenhaDe8CaracteresQueCumpreAsRegrasEhAceita` | **D2**: senha de 8 caracteres que cumpre as regras é recusada como "guessable" |

Os testes que falham **não** foram marcados como `skipped` ou `incomplete`: eles representam o comportamento
correto esperado e devem passar quando os defeitos forem corrigidos. Ficam em **quarentena** com
`@group defeito-conhecido`: fora da execução padrão (que deve ficar verde: 47 testes, 57 asserções) e rodados à
parte. Execução confirmada em 16/11/2026 sobre banco de testes recém-criado.

## Decisões e cuidados

- **Oráculo sobre efeitos.** O `EmployeeAPITest` original compara só o JSON de resposta; aqui, UT-05 lê o
  contador de IDs pelo mesmo método que o formulário usa (`getNextID(Employee::class, false)`) e confere a
  foto gravada no banco.
- **Controle.** Junto com o caso que expõe D1 existe o caso sem foto, que passa. Assim a falha não pode ser
  atribuída à montagem do teste.
- **EntityManager entre leituras.** Na aplicação, cada requisição tem o próprio EntityManager. No teste, a
  entidade `UniqueId` lida antes ficaria no *identity map* do Doctrine e o `UPDATE` em DQL de
  `incrementId()` não seria visto; por isso `clear()` antes de reler o contador. Sem isso, até o controle
  falhava, um falso positivo que foi investigado e corrigido.
- **Uma fixture só.** Cada `TestDataService::populate()` restaura o banco antes de inserir, então
  `EmployeeAPITest.yml` e o contador de IDs foram unidos em `fixtures/AddEmployee.yml`.
- **Relógio fixo.** `create()` registra o evento de criação com a data atual; o `DateTimeHelperService` é
  mockado para 12/11/2026 10:00.
- **Política de senha.** A política é lida do banco; usamos a mesma fixture do teste original
  (`orangehrmAuthenticationPlugin/test/fixtures/PasswordStrengthValidation.yml`), com os valores padrão 5.4+.

## Convivência com a suíte original

Suíte `Pim` completa com os nossos testes, sobre banco de testes recém-criado (12/11/2026):
**723 testes (674 originais + 49 nossos), 2106 asserções; 2 falhas**, exatamente UT-05 com foto (D1) e UT-08 com
8 caracteres (D2). Os 53 *risky* são os mesmos da suíte original (avisos de depreciação do PHP 8.3).

Recrie o banco de testes antes de cada execução completa: a suíte original deixa dados em `hs_hr_custom_fields`
e falha na 2ª execução (ver `../analise/codigo-original.md`, "Isolamento").
