# Plano de Testes e Casos de Teste: PIM › Add Employee

> Base: [jornada.md](jornada.md). Versão do código analisada: commit `56e23b3b` de `orangehrm/orangehrm`.

## 1. Plano de Testes

### 1.1 Objetivo
Verificar se o cadastro de funcionário (fluxo principal, A1–A3 e exceções E1–E9) segue as regras da
jornada, e investigar os riscos R1–R4 levantados na leitura do código.

### 1.2 Escopo
- **Dentro:** formulário *Add Employee*; `POST /api/v2/pim/employees`; `POST /api/v2/admin/users`
  na criação do login; regras de validação, política de senha e geração de Employee Id.
- **Fora:** edição de *Personal Details*, importação CSV, relatórios e outros módulos.

### 1.3 Níveis, ferramentas e ambiente

| Nível | Alvo | Ferramenta | Ambiente |
|---|---|---|---|
| Unitário | classes PHP do `orangehrmPimPlugin` (e serviços que ele usa) | **PHPUnit** (infra do próprio OrangeHRM: `TestCase`, `EndpointTestCase`, fixtures YAML) | OrangeHRM local via Docker |
| Integração | API REST v2 | **Java 17 + JUnit 5 + REST Assured** (Maven) | OrangeHRM local via Docker (principal) e servidor de demonstração (verificação pontual) |
| Sistema (E2E) | jornada pela interface | **Java 17 + JUnit 5 + Playwright for Java**, padrão Page Object | servidor de demonstração e/ou local |

Por que Java nos níveis de integração e sistema: é a linguagem de preferência do autor, e tanto o
REST Assured quanto o Playwright têm suporte oficial a ela.

### 1.4 Técnicas de projeto
- **Particionamento em classes de equivalência (PE)** para cada campo.
- **Análise de valor-limite (AVL)** nos tamanhos (0, 1, máx., máx.+1) e no tamanho da foto (1 MB).
- **Tabela de decisão (TD)** para a combinação foto × login × validade dos dados.
- **Teste baseado em casos de uso (CU)** para o fluxo principal, os alternativos e as exceções.
- **Teste guiado por risco** para R1–R4.

### 1.5 Critérios
- **Entrada:** ambiente de pé, usuário Admin disponível, política de senha nos valores padrão.
- **Saída:** todos os casos de prioridade Alta executados; defeitos registrados como Issue com evidência.
- **Oráculo:** regras confirmadas no código (seção 3 da jornada) e mensagens oficiais de
  `installer/Migration/*/lang-string/*.yaml`.

### 1.6 Isolamento e dados
- Dados únicos a cada execução (sufixo com timestamp em nomes, IDs e usernames), para rodar no
  servidor compartilhado sem colisões.
- Limpeza: os testes de integração e E2E excluem os funcionários e usuários que criarem
  (`DELETE /api/v2/pim/employees`, `DELETE /api/v2/admin/users`).
- Os testes unitários usam as fixtures e o banco de teste do próprio OrangeHRM.

### 1.7 Riscos do próprio plano
- O servidor de demonstração é reiniciado com frequência e é usado por muita gente: a execução
  oficial deve ser feita no ambiente local (Docker).
- Se a política de senha do servidor for alterada, os casos de E6 precisam ser revistos.

## 2. Classes de equivalência e valores-limite

| Campo | Classes válidas | Classes inválidas | Limites testados |
|---|---|---|---|
| First / Last Name | V1: 1–30 caracteres | I1: vazio; I2: > 30; I3: só espaços* | 0, 1, 30, 31 |
| Middle Name | V2: vazio; V3: 1–30 | I4: > 30 | 0, 30, 31 |
| Employee Id | V4: vazio (API); V5: 1–10, único | I5: > 10; I6: já existente | 10, 11 |
| Foto | V6: gif/jpg/jpeg/jfif/png ≤ 1 MB | I7: outro tipo (pdf, bmp, svg); I8: > 1 MB; I9: nome > 100 | 1 MB, 1 MB + 1 byte |
| Username | V7: 5–40, único | I10: < 5; I11: > 40; I12: existente | 4, 5, 40, 41 |
| Password | V8: 8–64, atende à política | I13: < 8; I14: sem maiúscula; I15: sem minúscula; I16: sem número; I17: sem especial; I18: com espaço; I19: > 64; I20: fraca/adivinhável | 7, 8, 64, 65 |
| Confirm Password | V9: igual | I21: diferente | – |

\* Comportamento com nome só de espaços não está definido no código; o caso serve para descobrir o comportamento.

## 3. Tabela de decisão (foto × login)

| Regra | Foto | Login | Dados | Resultado esperado | Casos |
|---|---|---|---|---|---|
| R-1 | não | não | válidos | funcionário criado | ST-01, IT-01 |
| R-2 | sim | não | válidos | funcionário + foto | ST-02, IT-05 |
| R-3 | não | sim | válidos | funcionário + usuário ESS | ST-03, IT-08 |
| R-4 | sim | sim | válidos | funcionário + foto + usuário | ST-04 |
| R-5 | – | sim | usuário inválido no servidor | **nada** deveria ser criado (verifica R1) | IT-11 |
| R-6 | – | – | funcionário inválido | nada criado, erro no campo | ST-05..ST-09 |

## 4. Casos de teste

Prioridade: **A** alta, **M** média, **B** baixa. Rastreabilidade: CA = critério de aceitação; FP/A/E = fluxo da jornada.

### 4.1 Unitários (PHPUnit), em `src/plugins/orangehrmPimPlugin/test`


| ID | Objetivo | Técnica | Rastreia | Prio |
|---|---|---|---|---|
| UT-01 | Regras de criação de `EmployeeAPI`: firstName com 30 aceita, com 31 rejeita | AVL | E7 | A |
| UT-02 | lastName vazio ou ausente rejeitado; middleName ausente aceito | PE | E1 | A |
| UT-03 | employeeId com 10 aceita, com 11 rejeita; ID duplicado rejeitado | AVL/PE | E2, E7 | A |
| UT-04 | empPicture com MIME `application/pdf` rejeitado; `image/png` aceito; nome do arquivo com 101 caracteres rejeitado | PE/AVL | E3 | M |
| UT-05 | `EmployeeAPI::create()` **com foto** deve incrementar o contador de ID | risco | R2, CA2 | A |
| UT-06 | `EmployeeAPI::create()` sem permissão lança `BadRequestException` | PE | E9 | M |
| UT-07 | `IDGeneratorService::getNextID(Employee)` completa com zeros até 4 dígitos (`0001`, `0999`, `1000`, `10000`) | AVL | CA2 | M |
| UT-08 | `PasswordStrengthService::checkPasswordPolicies`: uma senha para cada classe I13–I20 retorna exatamente as mensagens esperadas; senhas válidas nos limites (8 e 64 caracteres) retornam lista vazia | PE/AVL | E6 | A |

### 4.2 Integração (Java + REST Assured), API REST v2


| ID | Requisição | Entrada | Resultado esperado | Rastreia | Prio |
|---|---|---|---|---|---|
| IT-01 | POST employees | first/last válidos | 200; `data.empNumber` > 0; GET devolve os mesmos nomes | FP | A |
| IT-02 | POST employees | lastName ausente | 422; nada criado | E1 | A |
| IT-03 | POST employees | firstName com 31 caracteres | 422 | E7 | A |
| IT-04 | POST employees | employeeId repetido | 422 | E2 | A |
| IT-05 | POST employees | foto PNG em base64 | 200; `GET /pim/employees/{n}/picture` devolve imagem | A1 | M |
| IT-06 | POST employees | foto com tipo `application/pdf` | 422 | E3 | M |
| IT-07 | POST employees | foto PNG de 1.048.575, 1.048.576 e 1.048.577 bytes; tamanho declarado menor que o real | 200, 200, 422, 422 (verifica R3: o limite também é validado no servidor?) | R3, E3 | M |
| IT-08 | POST employees + POST users | username/senha válidos, userRoleId 2 | 200; GET users filtrado por username retorna o usuário com `empNumber` certo | A2 | A |
| IT-09 | POST users | username existente | 422 | E4 | A |
| IT-10 | POST users | senhas das classes I13–I19 | 422 para cada uma | E6 | A |
| IT-11 | sequência do front-end com a 2ª chamada recusada (username existente) | – | verificar se o funcionário **permanece** criado sem usuário (R1) | R1 | A |
| IT-12 | POST employees ×2 (a 1ª com foto), sem employeeId | – | o ID sugerido depois deve ser diferente do usado (R2) | R2 | A |

### 4.3 Sistema / E2E (Playwright for Java)


| ID | Cenário | Passos resumidos | Resultado esperado | Rastreia | Prio |
|---|---|---|---|---|---|
| ST-01 | Cadastro mínimo | login Admin → PIM › Add Employee → First/Last → Save | toast "Successfully Saved"; URL `/pim/viewPersonalDetails/empNumber/…`; nome exibido | FP, CA1 | A |
| ST-02 | Com foto | ST-01 + upload de PNG válido | foto exibida no perfil | A1, CA4 | M |
| ST-03 | Com login | ativa toggle, username/senhas válidos | sucesso; o usuário aparece em Admin › User Management | A2, CA5 | A |
| ST-04 | Com foto e login | combinação de ST-02 e ST-03 | sucesso | R-4 | B |
| ST-05 | Nome vazio | Save sem First Name | "Required" sob o campo; URL não muda | E1 | A |
| ST-06 | ID duplicado | informa o ID de um funcionário existente | "Employee Id already exists" | E2, CA3 | A |
| ST-07 | Foto inválida | upload de `.pdf` / imagem > 1 MB | "File type not allowed" / "Attachment Size Exceeded" | E3 | M |
| ST-08 | Username existente | username `Admin` | "Username already exists" | E4 | A |
| ST-09 | Senhas diferentes | Confirm ≠ Password | "Passwords do not match" | E5 | A |
| ST-10 | Senha fraca | `abc` / `abcdefgh` | mensagem de política | E6 | A |
| ST-11 | Limites | 31 caracteres no First Name; 11 no ID; 4 e 41 no Username | "Should not exceed …" / "Should be at least 5 characters" | E7, E8 | M |
| ST-12 | ID sugerido após cadastro com foto | cadastra com foto sem mexer no ID → abre Add Employee de novo | ID sugerido ≠ ID anterior (R2) | R2 | A |
| ST-13 | Cancelar | preenche e clica em Cancel | volta à lista; nada criado | FP | B |

> Em andamento: as demais seções entram nos próximos commits.

## Fontes
- Jornada e regras: [jornada.md](jornada.md) e os arquivos do OrangeHRM citados nela.
- Técnicas: material da disciplina (particionamento em equivalência, valor-limite, tabela de decisão, casos de uso).
- Ferramentas: PHPUnit (https://phpunit.de), REST Assured (https://rest-assured.io), Playwright for Java (https://playwright.dev/java).
- Uso de IA: projeto inicial dos casos feito com Claude Code (Claude Opus 5.5), revisado pelo autor.
