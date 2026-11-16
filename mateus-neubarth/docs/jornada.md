# Jornada de usuário: Admin cadastra um funcionário (PIM › Add Employee)

> Versão analisada: OrangeHRM 5.x, repositório `orangehrm/orangehrm`, commit
> `56e23b3b09e7af29317a0943523b825843fff527` (29/06/2026, "OHRM5X-2669: Update Dockerfile for OrangeHRM 5.9").
> Servidor de demonstração: https://opensource-demo.orangehrmlive.com (Admin / admin123).

## 1. História de usuário

**Como** administrador de RH,
**quero** cadastrar um novo funcionário no módulo PIM, com foto e credenciais de acesso opcionais,
**para que** ele passe a constar no quadro de pessoal e, se for o caso, consiga entrar no sistema.

### Critérios de aceitação (Given / When / Then)

| ID | Critério |
|----|----------|
| CA1 | **Dado** que estou autenticado como Admin, **quando** informo First Name e Last Name válidos e clico em *Save*, **então** o funcionário é criado, vejo o aviso "Successfully Saved" e sou levado à tela *Personal Details* dele. |
| CA2 | **Dado** o formulário aberto, **quando** não altero o campo Employee Id, **então** é usado o próximo ID sugerido pelo sistema (4 dígitos, ex.: `0042`). |
| CA3 | **Dado** que informo um Employee Id já usado por outro funcionário, **quando** saio do campo ou salvo, **então** vejo "Employee Id already exists" e nada é gravado. |
| CA4 | **Dado** que anexo uma foto GIF/JPG/PNG de até 1 MB, **quando** salvo, **então** a foto aparece no perfil do funcionário. |
| CA5 | **Dado** que ativo *Create Login Details* e preencho usuário e senhas válidos, **quando** salvo, **então** é criado também um usuário do sistema (perfil ESS) vinculado ao funcionário, com o status escolhido. |
| CA6 | **Dado** qualquer campo inválido, **quando** tento salvar, **então** a mensagem de erro aparece junto ao campo e nenhum registro é criado. |

## 2. Atores e pré-condições

- **Ator principal:** usuário com papel *Admin* (permissão `EMPLOYEE_ACTION_ADD` no fluxo de funcionário).
- **Pré-condições:** usuário autenticado; acesso ao menu PIM.
- **Pós-condição de sucesso:** registro em `hs_hr_employee` (e `hs_hr_emp_picture` / `ohrm_user`, conforme o fluxo); evento `EmployeeAddedEvent` disparado; contador de ID (`hs_hr_unique_id`) incrementado.

## 3. Campos e regras (confirmados no código)

| Campo | Obrigatório | Regra | Onde está a regra |
|---|---|---|---|
| First Name | Sim | texto, máx. **30** caracteres | `EmployeeService::FIRST_NAME_MAX_LENGTH`; `EmployeeAPI::getCommonBodyValidationRules`; `SaveEmployee.vue` |
| Middle Name | Não | texto, máx. **30** | `EmployeeService::MIDDLE_NAME_MAX_LENGTH` |
| Last Name | Sim | texto, máx. **30** | `EmployeeService::LAST_NAME_MAX_LENGTH` |
| Employee Id | Não (pré-preenchido) | máx. **10**, **único** entre funcionários não expurgados (`purgedAt = null`) | `EmployeeService::EMPLOYEE_ID_MAX_LENGTH`; regra `ENTITY_UNIQUE_PROPERTY` |
| Foto | Não | tipos `gif, jpeg, jpg, jfif, png` (MIME `image/gif`, `image/jpeg`, `image/jpg`, `image/pjpeg`, `image/png`, `image/x-png`); **≤ 1 MB** (só no front-end); nome do arquivo ≤ **100** | `EmpPicture::ALLOWED_IMAGE_*`; `EmployeeAPI::PARAM_RULE_EMP_PICTURE_FILE_NAME_MAX_LENGTH`; `maxFileSize(1024*1024)` em `SaveEmployee.vue` |
| Create Login Details | Não | liga a criação do usuário | `SaveEmployee.vue` |
| Username | Sim, se login | **5 a 40** caracteres, **único** entre usuários não excluídos | `UserService::USERNAME_MIN/MAX_LENGTH`; `UserAPI` |
| Status | Sim, se login | Enabled / Disabled | `UserAPI` (`BOOL_TYPE`) |
| Password | Sim, se login* | máx. **64**; política padrão: mín. **8** caracteres, ≥1 maiúscula, ≥1 minúscula, ≥1 número, ≥1 caractere especial, sem espaços, força mínima *strong* (zxcvbn score ≥ 3). | `Rules\Password`; `PasswordStrengthService::checkPasswordPolicies`; valores padrão em `installer/Migration/V5_4_0/Migration.php` |
| Confirm Password | Sim, se login | igual à senha ("Passwords do not match") | `PasswordInput.vue` |

\* A senha deixa de ser obrigatória quando a autenticação por rede social está ativa (`isPasswordRequired`).

> A política de senha é **configurável** pelo Admin (chaves `auth.password_policy.*` em `ConfigService`). Os testes usam os valores
> padrão; antes de executar no servidor de demonstração, conferir se não foram alterados.

> Em andamento: fluxos, exceções, riscos e diagrama entram no próximo commit.

## Fontes

- Código-fonte OrangeHRM: https://github.com/orangehrm/orangehrm (commit indicado acima). Arquivos:
  `src/plugins/orangehrmPimPlugin/Api/EmployeeAPI.php`, `Service/EmployeeService.php`, `entity/EmpPicture.php`,
  `Controller/SaveEmployeeController.php`; `src/plugins/orangehrmAdminPlugin/Api/UserAPI.php`,
  `Service/UserService.php`; `src/plugins/orangehrmCorePlugin/Service/IDGeneratorService.php`,
  `Api/V2/Validator/Rules/Password.php`; `src/plugins/orangehrmAuthenticationPlugin/Service/PasswordStrengthService.php`;
  `installer/Migration/V5_4_0/Migration.php`; `src/client/src/orangehrmPimPlugin/pages/employee/SaveEmployee.vue`;
  `src/client/src/core/components/inputs/PasswordInput.vue`.
- Uso de IA: levantamento das regras no código e redação inicial feitos com Claude Code (Claude Opus 5.5),
  revisados pelo autor. Detalhes no diário do trabalho.
