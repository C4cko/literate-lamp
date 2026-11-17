# Defeitos encontrados: PIM › Add Employee

Issue #22. Os dois defeitos foram detectados nos três níveis de teste; os testes que os expõem ficam em
quarentena (`defeito-conhecido`) e rodam à parte (ver `ambiente.md`, seção 6.1).

| ID | Título | Severidade | Status | Origem |
|---|---|---|---|---|
| D1 | Cadastro com foto não avança o Employee Id sugerido | Média | Confirmado nos 3 níveis: UT-05, IT-12 e ST-12 falham | Risco R2 |
| D2 | Senha de 8 caracteres nunca é aceita, embora o mínimo anunciado seja 8 | Baixa | Confirmado nos 3 níveis: UT-08, IT-10 e ST-10 falham | Valor-limite da política de senha |

## D1: cadastro com foto não avança o Employee Id sugerido

**Versão:** OrangeHRM 5.9 (imagem `orangehrm/orangehrm:5.9`); código no commit `56e23b3b`.
**Ambiente:** local (`ambiente/docker-compose.yml`), banco recém-instalado.

### Passos para reproduzir

1. Entrar como Admin e abrir **PIM › Add Employee**. Anotar o Employee Id sugerido (ex.: `0005`).
2. Preencher First Name e Last Name, anexar uma foto PNG válida e **não alterar** o Employee Id. Clicar em *Save*.
3. Abrir **PIM › Add Employee** novamente.

**Resultado esperado:** o Employee Id sugerido é o próximo (`0006`).
**Resultado obtido:** o sistema sugere de novo `0005`. Quem não alterar o campo recebe
"Employee Id already exists" ao salvar (a API responde 422 com `invalidParamKeys: ["employeeId"]`).

**Controle:** repetindo o passo 2 **sem foto**, o ID sugerido avança normalmente (`0005` → `0006`).

### Evidência (execução exploratória via API, 12/11/2026)

| Passo | Resultado |
|---|---|
| ID sugerido antes | `0005` |
| `POST /api/v2/pim/employees` com `empPicture` (PNG) e `employeeId: "0005"` | 200, funcionário criado com `0005` |
| ID sugerido depois do cadastro **com** foto | **`0005`** (não avançou) |
| `POST` usando o ID sugerido | **422**, `employeeId` inválido (duplicado) |
| Controle: cadastro **sem** foto com o ID sugerido | 200; sugerido depois: `0006` |

### Causa provável (leitura do código)

`src/plugins/orangehrmPimPlugin/Api/EmployeeAPI.php`, método `create()`:

```php
if ($empPictureAttachment) {
    // ... monta EmpPicture ...
    $this->getEmployeePictureService()->saveEmployeePicture($empPicture);   // salva em cascata, sem incrementar
} else {
    $this->getEmployeeService()->saveNewEmployee($employee);                // incrementId() + save
}
```

Só `EmployeeService::saveNewEmployee()` chama `IDGeneratorService::incrementId(Employee::class)`. O ramo com
foto persiste o funcionário em cascata pela entidade `EmpPicture` e nunca incrementa o contador em
`hs_hr_unique_id`. O ID sugerido no formulário vem de `getNextID(Employee::class, false)`
(`SaveEmployeeController`), que lê esse contador.

**Correção sugerida:** chamar `IDGeneratorService::incrementId(Employee::class)` também no ramo com foto (ou
salvar o funcionário com `saveNewEmployee()` antes de salvar a foto).

### Por que os testes originais não pegaram

O caso `with all details and emp picture` de `EmployeeAPITestCases.yml` executa exatamente esse ramo, e a
cobertura de linhas de `EmployeeAPI` é 99,66%. Mas o oráculo compara só o JSON de resposta e nunca o contador
de IDs (ver `analise/codigo-original.md`).

### Testes que automatizam a verificação

Os três falham, como esperado, e ficam em quarentena (`defeito-conhecido`, ver `docs/ambiente.md`, seção 6.1);
cada um tem um caso de controle sem foto que passa:

| Nível | Teste |
|---|---|
| Unitário | `unitario/T2AddEmployee/EmployeeCreateBehaviourTest.php::testUT05CriarComFotoAvancaOIdSugerido` |
| Integração | `integracao/.../CreateEmployeeApiTest.java::it12_comFotoAvancaIdSugerido` |
| Sistema | `sistema/.../AddEmployeeE2ETest.java::st12_comFotoIdSugeridoAvanca` |

## D2: senha de 8 caracteres nunca é aceita, embora o mínimo anunciado seja 8

**Versão:** OrangeHRM 5.9, política de senha padrão (`installer/Migration/V5_4_0/Migration.php`: mínimo 8,
1 maiúscula, 1 minúscula, 1 número, 1 especial, sem espaços, força mínima `strong`).

### Passos para reproduzir

1. PIM › Add Employee, ativar *Create Login Details*.
2. Informar uma senha de **8** caracteres que cumpre todas as regras exibidas, ex.: `Qz7#mX2k`.

**Resultado esperado:** a senha é aceita, porque o sistema informa "Should have at least 8 characters" como
limite inferior e todas as demais regras foram cumpridas.
**Resultado obtido:** "Your password meets the minimum requirements, but it could be guessable"; o cadastro
do usuário é recusado (`POST /api/v2/admin/users` → 422, `invalidParamKeys: ["password"]`). Com 9 caracteres
(`t7#Rw!9Zp`), aceita.

### Evidência

| Verificação | Resultado |
|---|---|
| 2.000 senhas aleatórias de **8** caracteres cumprindo as 4 classes, avaliadas pelo zxcvbn do próprio OrangeHRM | score 2 em 1.999, score 1 em 1; **nenhuma** com score ≥ 3 |
| 2.000 senhas aleatórias de **9** e de **10** caracteres | todas com score 3 |
| `POST /api/v2/admin/users` com `Qz7#mX2k` (8) | 422 |
| `POST /api/v2/admin/users` com `t7#Rw!9Zp` (9) | 200 |
| `POST /api/v2/auth/public/validation/password` com `Qz7#mX2k` | `strength: 2`, mensagem "could be guessable" |

### Causa

`PasswordStrengthService::checkRequiredDefaultPasswordStrength()` exige `strength >= 3` quando a configuração
é `strong`. A força vem do zxcvbn (`PasswordStrengthValidation::checkPasswordStrength`), cuja estimativa por
força bruta para 8 caracteres não alcança o score 3. As duas regras juntas tornam o limite inferior anunciado
(8) **inatingível**: o mínimo efetivo é 9.

### Classificação

Inconsistência entre a regra comunicada ao usuário e o comportamento real. Não é falha de segurança (fica mais
restritivo do que o anunciado), mas confunde o usuário e quebra testes escritos a partir da especificação (o
projeto de terceiros P2 gera senhas de 8 caracteres para o caso válido). Sugestões: anunciar 9 como mínimo
quando a força exigida for `strong`, ou ajustar o padrão de `min_password_length` para 9.

### Testes

UT-08 (`unitario/T2AddEmployee/PasswordPolicyTest.php::testUT08SenhaDe8CaracteresQueCumpreAsRegrasEhAceita`:
**falha**, como esperado); `integracao/.../CreateLoginApiTest.java::it10_senhaDe8Caracteres` (422) e
`sistema/.../AddEmployeeValidationE2ETest.java::st10_senhaDe8Caracteres` (mensagem "could be guessable"), ambos
falhando e em quarentena (`defeito-conhecido`). Nos três níveis, a senha de 9 caracteres passa.
