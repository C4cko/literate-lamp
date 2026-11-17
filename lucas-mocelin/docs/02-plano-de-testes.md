# 2. Plano de Testes – Jornada "Autenticação (Login)"

> Documento da **Issue "Plano de Testes"**. Sistema sob teste: OrangeHRM 5.9 (commit `56e23b3`).

## 2.1 Objetivos

1. Verificar se um usuário legítimo consegue **entrar, usar e sair** do OrangeHRM com segurança,
   e se usuários não autorizados (credencial errada, conta desabilitada, funcionário desligado,
   usuário excluído) são barrados.
2. Exercitar a jornada nos **três níveis**: unitário (regras isoladas), integração (serviços +
   ORM + banco + sessão) e sistema (navegador real).
3. Aplicar técnicas de projeto de casos de teste: **classes de equivalência**, **valor-limite**,
   **tabela de decisão**, **transição de estados**, **casos de uso** e teste **baseado em riscos**.
4. Medir a **eficácia** dos testes por análise de mutação e compará-la com a suíte original.

Fora do escopo: login por LDAP/OpenID/OAuth, acesso administrativo elevado
(`admin/verify`), aplicativo móvel, desempenho e carga.

## 2.2 Jornada de usuário

### Persona

**Maria Souza**, 34 anos, analista de RH. Usa o OrangeHRM todos os dias para consultar
funcionários e aprovar férias. Acessa pelo navegador do notebook da empresa e às vezes esquece
a senha depois das férias.

### História de usuário

> **Como** funcionária da empresa, **quero** entrar no OrangeHRM com meu usuário e minha senha
> **para** acessar minhas funções de RH com segurança. **Se eu esquecer a senha, quero** recuperá-la
> sozinha por e-mail.

### Critérios de aceitação (formato Dado/Quando/Então)

| ID | Critério |
|---|---|
| CA-1 | **Dado** que tenho conta ativa, **quando** informo usuário e senha corretos, **então** vejo o Dashboard com meu nome no menu. |
| CA-2 | **Dado** que erro a senha ou o usuário, **quando** clico em *Login*, **então** vejo "Invalid credentials" e continuo na tela de login, sem saber qual dos dois errei. |
| CA-3 | **Dado** que deixo campos em branco, **quando** clico em *Login*, **então** cada campo vazio mostra "Required". |
| CA-4 | **Dado** que minha conta está desabilitada, fui desligada ou não tenho funcionário vinculado, **quando** informo a senha correta, **então** o acesso é negado. |
| CA-5 | **Dado** que estou autenticada, **quando** clico em *Logout*, **então** volto ao login e não consigo mais acessar páginas ou API sem entrar de novo. |
| CA-6 | **Dado** que não estou autenticada, **quando** abro uma URL interna, **então** sou levada ao login. |
| CA-7 | **Dado** que esqueci a senha, **quando** informo meu usuário em *Forgot your password?*, **então** recebo um link por e-mail, defino uma nova senha e passo a entrar só com ela. |
| CA-8 | **Dado** que a política "senha forte obrigatória" está ligada e minha senha é fraca, **quando** faço login, **então** sou obrigada a trocá-la. |

### Mapa da jornada

| Etapa | 1. Abrir o sistema | 2. Informar credenciais | 3. Ser validada | 4. Usar a sessão | 5. Sair | Alternativo: Esqueci a senha |
|---|---|---|---|---|---|---|
| Ação | acessa a URL | digita usuário/senha, *Login* ou Enter | — | navega pelos módulos | menu → *Logout* | link → e-mail → nova senha |
| Tela/rota | `/auth/login` | formulário OXD | `POST /auth/validate` | `/dashboard/index`, `/pim/...` | `/auth/logout` | `/auth/requestPasswordResetCode` → `/auth/resetPassword/resetCode/{code}` |
| Componentes | `LoginController` | `Login.vue` (validação no cliente) | `ValidateController` → `AuthProviderChain` → `LocalAuthProvider` → `AuthenticationService` → `UserService`/`UserDao` → banco; `PasswordStrengthService` | `AuthenticationSubscriber` (bloqueia acesso anônimo), sessão | `LogoutController` | `ResetPasswordService`, e-mail |
| Riscos | página fora do ar | erro de digitação, senha fraca | acesso indevido, injeção de SQL, CSRF, enumeração de usuários, força bruta | sessão roubada ou que não expira | sessão não encerrada | enumeração, link reutilizável |

### Caso de uso UC-01 "Autenticar-se"

* **Ator**: usuário do OrangeHRM (Admin ou ESS).
* **Pré-condição**: o usuário possui conta.
* **Fluxo principal**: 1) abre a tela de login; 2) informa usuário e senha; 3) o sistema valida
  as credenciais e o estado da conta; 4) o sistema cria a sessão e exibe o Dashboard.
* **Fluxos alternativos**: A1 campo vazio → "Required"; A2 credencial inválida → "Invalid credentials";
  A3 conta desabilitada, funcionário desligado ou sem funcionário → acesso negado com mensagem própria;
  A4 senha fraca com política ligada → redireciona para troca obrigatória; A5 esqueci a senha → UC-02.
* **Pós-condição**: sessão autenticada com id, papel e número do funcionário do usuário.

## 2.3 Estratégia

| Nível | O que é testado | Ferramenta | Ambiente | Técnicas |
|---|---|---|---|---|
| Unitário | `AuthenticationService::setCredentialsForUser`, `PasswordStrengthService::isValidPassword`, `PasswordStrengthValidation` isolados por dublês (sem banco) | PHPUnit 9.6 | PHP 8.3 | Tabela de decisão, classes de equivalência, valor-limite |
| Integração | `LocalAuthProvider` + `AuthenticationService` + `UserService` + `UserDao` + Doctrine + MariaDB + `ConfigService` + sessão Symfony, sem dublês nessas camadas | PHPUnit + fixture YAML | MariaDB 10.11 (banco `test_orangehrm_mysql`) | Classes de equivalência, transição de estados, robustez |
| Sistema | jornada completa no navegador | Playwright 1.x + Chromium, Page Object | instalação local (PHP built-in server + SMTP falso) **e** servidor de demonstração | Casos de uso, transição de estados, baseado em riscos (OWASP ASVS V2/V3) |

**Critério de entrada**: ambiente instalado e suíte original `Authentication` passando.
**Critério de saída**: 100 % dos casos planejados executados; todo defeito encontrado registrado
como Issue; escore de mutação da suíte da jornada ≥ suíte original.

**Massa de dados de integração** (`testes/php/fixtures/JornadaLogin.yml`):

| Usuário | Papel | Estado | Senha |
|---|---|---|---|
| ana | Admin | ativa | `Ana@Segura#2026` |
| bruno | ESS | **desabilitado** | `Bruno@Segura#2026` |
| carla | ESS | funcionária **desligada** | `Carla@Segura#2026` |
| diego | ESS | **sem funcionário** | `Diego@Segura#2026` |
| eva | ESS | **excluída** | `Eva@Segura#2026` |
| fabio | ESS | ativo, **senha fraca** | `fabio123` |
| gil | ESS | ativo, **hash MD5 legado** | `Gil@Legado#2020` |

## 2.4 Casos de teste – nível unitário

### 4.1 Tabela de decisão – `AuthenticationService::setCredentialsForUser`

Condições na ordem em que o código as avalia: C1 usuário encontrado; C2 não excluído;
C3 conta ativa; C4 tem funcionário; C5 funcionário não desligado.

| Regra / Caso | C1 | C2 | C3 | C4 | C5 | Ação esperada |
|---|---|---|---|---|---|---|
| UT-AS-01 | N | – | – | – | – | retorna `false` |
| UT-AS-02 | S | N | – | – | – | retorna `false` |
| UT-AS-03 | S | S | N | – | – | exceção `USER_DISABLED` ("Account disabled") |
| UT-AS-04 | S | S | S | N | – | exceção `EMPLOYEE_NOT_ASSIGNED` |
| UT-AS-05 | S | S | S | S | N | exceção `EMPLOYEE_TERMINATED` |
| UT-AS-06 | S | S | S | S | S | retorna `true` e grava atributos na sessão 1 vez |
| UT-AS-07 | S | S | N | N | – | prevalece `USER_DISABLED` (precedência das regras) |

Arquivo: `testes/php/Unit/AuthenticationServiceDecisionTableTest.php`

### 4.2 Classes de equivalência e valores-limite – `PasswordStrengthService::isValidPassword`

Política: comprimento 8..64, ≥1 maiúscula, ≥1 minúscula, ≥1 número, ≥1 especial, sem espaços,
força mínima *strong* (3).

| Caso | Entrada | Classe / limite | Esperado |
|---|---|---|---|
| UT-PW-01 | 7 caracteres | min − 1 | inválida |
| UT-PW-02 | 8 caracteres | min | válida |
| UT-PW-03 | 9 caracteres | min + 1 | válida |
| UT-PW-04 | 63 caracteres | max − 1 | válida |
| UT-PW-05 | 64 caracteres | max | válida |
| UT-PW-06 | 65 caracteres | max + 1 | inválida |
| UT-PW-07 | "" | vazia | inválida |
| UT-PW-08 | `Senha@Forte1` | classe válida | válida |
| UT-PW-09 | `senha@forte1` | sem maiúscula | inválida |
| UT-PW-10 | `SENHA@FORTE1` | sem minúscula | inválida |
| UT-PW-11 | `Senha@Forte` | sem número | inválida |
| UT-PW-12 | `SenhaForte12` | sem especial | inválida |
| UT-PW-13 | `Senha @Forte1` | com espaço | inválida |
| UT-PW-14 | `SenhaçãoFor1` | acento não conta como especial | inválida |
| UT-PW-15 | força 2 | limite − 1 | inválida |
| UT-PW-16 | força 3 | limite | válida |
| UT-PW-17 | força 4 | limite + 1 | válida |
| UT-PW-18 | `Senha @Forte1`, espaços permitidos | configuração alternativa | válida |
| UT-PW-19 | mínimos configurados com −1 | configuração inválida tratada como 0 | válida |
| UT-PW-20 | força 0, mínimo *veryWeak* | configuração mais permissiva | válida |

Arquivo: `testes/php/Unit/PasswordPolicyBoundaryTest.php`

### 4.3 Classes de equivalência – `PasswordStrengthValidation::checkPasswordStrength`

| Caso | Entrada | Esperado |
|---|---|---|
| UT-SV-01 | "" | `VERY_WEAK` (0) |
| UT-SV-02 | `password` | `VERY_WEAK` (0) |
| UT-SV-03 | `admin123` (senha da demo) | < `STRONG` |
| UT-SV-04 | `Tq9#vL2!pX7$wZ4&mR8` | `STRONGEST` (4) |
| UT-SV-05 | `null` | `VERY_WEAK`, sem erro |

Arquivo: `testes/php/Unit/PasswordStrengthValidationTest.php`

## 2.5 Casos de teste – nível de integração

Arquivo: `testes/php/Integration/LoginIntegrationTest.php`

| Caso | Pré-condição / Entrada | Resultado esperado | CA |
|---|---|---|---|
| IT-01 | ana / senha correta | `true`; sessão com `user_id=1`, papel `Admin`, `emp_number=1` | CA-1 |
| IT-02 | ana / senha errada | `false`; sessão sem usuário | CA-2 |
| IT-03 | usuário inexistente | `false` | CA-2 |
| IT-04 | ana / senha em minúsculas | `false` (senha diferencia maiúsculas) | CA-2 |
| IT-05 | `ANA` / senha correta | `true` (usuário não diferencia maiúsculas) | CA-1 |
| IT-06 | bruno (desabilitado) | exceção `USER_DISABLED`; sessão vazia | CA-4 |
| IT-07 | carla (desligada) | exceção `EMPLOYEE_TERMINATED` | CA-4 |
| IT-08 | diego (sem funcionário) | exceção `EMPLOYEE_NOT_ASSIGNED` | CA-4 |
| IT-09 | eva (excluída) | `false` (tratada como inexistente) | CA-4 |
| IT-10 | gil (hash MD5) | `true`; hash regravado em bcrypt; 2º login funciona | CA-1 |
| IT-11 | política **on** + fabio (senha fraca) | `PasswordEnforceException`, redireciona para `changeWeakPassword/resetCode/…`, 1 registro novo em `EnforcePasswordRequest` | CA-8 |
| IT-12 | política **on** + ana (senha forte) | `true` | CA-8 |
| IT-13 | política **off** + fabio | `true` | CA-8 |
| IT-14 | `' OR '1'='1` e `ana' -- ` | `false` (sem injeção de SQL) | risco |
| IT-15 | usuário/senha vazios | `false` | CA-3 |

## 2.6 Casos de teste – nível de sistema

Arquivos: `testes/sistema/tests/*.spec.js` (Page Object em `pages/LoginPage.js`).

| Caso | Passos | Resultado esperado | CA |
|---|---|---|---|
| ST-01 | login com Admin válido | URL `/dashboard/index`, título "Dashboard", nome no menu | CA-1 |
| ST-02 | senha incorreta | alerta "Invalid credentials", permanece em `/auth/login` | CA-2 |
| ST-03 | usuário inexistente | **mesma** mensagem do ST-02 | CA-2 |
| ST-04 | *Login* com campos vazios | "Required" nos dois campos | CA-3 |
| ST-05 | só usuário | "Required" só na senha | CA-3 |
| ST-06 | só senha | "Required" só no usuário | CA-3 |
| ST-07 | inspecionar campo senha | `type="password"` | — |
| ST-08 | preencher e pressionar Enter | Dashboard | CA-1 |
| ST-09 | usuário com espaços (" Admin ") | "Invalid credentials" (sem *trim*) | CA-2 |
| ST-10 | usuário em minúsculas ("admin") | Dashboard | CA-1 |
| ST-11 | abrir `/pim/viewEmployeeList` sem sessão | redireciona para `/auth/login` | CA-6 |
| ST-12 | login → lista de funcionários → logout → Voltar → chamar a API | volta ao login; API responde **401** | CA-5 |
| ST-13 | login → recarregar a página | continua no Dashboard | CA-1 |
| ST-14 | *Forgot your password?* → *Cancel* | abre a tela "Reset Password" e volta ao login | CA-7 |
| ST-15 | recuperação com usuário vazio | "Required" | CA-7 |
| ST-16 | recuperação com usuário inexistente | **mesma** tela de sucesso (sem enumeração) | CA-7 |
| ST-17 | recuperação ponta a ponta: pedir link → ler e-mail → definir nova senha → reabrir o link → login antigo → login novo → restaurar senha | link chega por e-mail; usuário vem preenchido e somente leitura; link vale **uma vez**; senha antiga recusada; nova aceita | CA-7 |
| ST-18 | `POST /auth/validate` sem token CSRF | 302 para login; sessão continua anônima | risco |
| ST-19 | 10 senhas erradas seguidas e depois a correta | **observação**: documenta que não há bloqueio de conta | risco |

ST-17 só roda na instalação local (precisa ler a caixa de e-mail; variável `MAILBOX_DIR`).

## 2.7 Rastreabilidade (critério → casos)

| Critério | Unitário | Integração | Sistema |
|---|---|---|---|
| CA-1 | UT-AS-06 | IT-01, 05, 10 | ST-01, 08, 10, 13 |
| CA-2 | UT-AS-01 | IT-02, 03, 04 | ST-02, 03, 09 |
| CA-3 | — | IT-15 | ST-04, 05, 06 |
| CA-4 | UT-AS-02..05, 07 | IT-06..09 | — (exige preparar contas; coberto na integração) |
| CA-5 | — | — | ST-12 |
| CA-6 | — | — | ST-11 |
| CA-7 | — | — | ST-14..17 |
| CA-8 | UT-PW-01..20, UT-SV-01..05 | IT-11, 12, 13 | — |
| Riscos de segurança | — | IT-14 | ST-03, 16, 18, 19 |

**Total planejado: 66 casos** (32 unitários, 15 de integração, 19 de sistema).

---
- Uso de IA: escolha da jornada, leitura do código-fonte, projeto inicial dos casos de teste e redação
  feitos com Claude (Anthropic, Claude Opus 5.5), revisados pelo autor.
