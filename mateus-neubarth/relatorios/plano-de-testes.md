# Relatório do Plano de Testes: PIM › Add Employee

> Jornada: **Admin cadastra um funcionário** no OrangeHRM. Testes em três níveis: unitário (PHPUnit),
> integração (Java + REST Assured) e sistema (Java + Playwright).
> Todos os arquivos citados estão na pasta `mateus-neubarth/` do repositório da turma (`masmangan/literate-lamp`).
> Issues: análise dos testes existentes (#2, #4), jornada e casos de teste (#9), implementação por nível (#10
> unitário, #14 integração, #18 sistema) e defeitos (#22). Resultados: Issue #53, "Relatório da Execução dos Testes".

## 1. Introdução

### 1.1 O sistema

O **OrangeHRM Starter** é um sistema de código aberto de gestão de recursos humanos (cadastro de funcionários,
férias, ponto, recrutamento, desempenho). O back-end é em PHP sobre componentes do Symfony e o Doctrine ORM. O
front-end é Vue 3, e as telas conversam com o servidor por uma API REST (`/web/index.php/api/v2/...`). Cada
módulo é um plugin em `src/plugins/orangehrm{Nome}Plugin/`. A jornada escolhida fica no módulo **PIM**
(*Personal Information Management*).

| Item | Onde |
|---|---|
| Código-fonte | https://github.com/orangehrm/orangehrm (analisado no commit `56e23b3b`, 29/06/2026, versão 5.9) |
| Servidor de demonstração | https://opensource-demo.orangehrmlive.com (Admin / admin123) |
| Descrição do sistema | https://www.orangehrm.com |
| Imagem Docker oficial usada nos testes | `orangehrm/orangehrm:5.9` |

### 1.2 Como compilar e rodar os testes

Tudo roda em contêineres. A máquina precisa de **Docker** (Compose v2), **Java 17+** e **Maven 3.8+**. O passo a
passo completo, com problemas encontrados e soluções, está em `docs/ambiente.md`.

```bash
# 1. Sistema sob teste: MariaDB + OrangeHRM 5.9 em http://localhost:8200 (Admin / Admin@OHRM123)
cd mateus-neubarth/ambiente
docker compose up -d db app
docker compose exec app php installer/cli_install.php

# 2. Testes unitários (PHPUnit), com o clone de orangehrm/orangehrm em ../../../orangehrm (ou OHRM_SRC=...)
docker compose up -d phpunit
docker compose exec phpunit bash /scripts/preparar-phpunit.sh       # uma vez: composer + banco de testes
docker compose exec phpunit ./src/vendor/bin/phpunit --group T2AddEmployee --exclude-group defeito-conhecido --testdox

# 3. Testes de integração (API REST v2)
cd ../integracao && mvn test

# 4. Testes de sistema (navegador)
cd ../sistema && mvn test                    # -Dheadless=false para ver o navegador
```

Os testes que expõem defeitos conhecidos ficam em quarentena, marcados como `defeito-conhecido`. A execução
padrão roda todo o resto, e eles rodam à parte: `mvn test -Pdefeitos` e `phpunit --group defeito-conhecido`.

Os testes de integração e de sistema também rodam contra o servidor de demonstração:
`-Dohrm.baseUrl=https://opensource-demo.orangehrmlive.com/web/index.php -Dohrm.password=admin123`.

## 2. Objetivos

### 2.1 Objetivo dos testes

Verificar se o cadastro de funcionário segue as regras do sistema, no fluxo principal, nos fluxos alternativos e
nas exceções, nos três níveis de teste. Também investigar os riscos encontrados na leitura do código-fonte (R1–R4).

### 2.2 Jornada de usuário

Descrição completa, com diagrama do fluxo, em `docs/jornada.md`.

**História de usuário.** *Como* administrador de RH, *quero* cadastrar um novo funcionário no módulo PIM, com foto
e credenciais de acesso opcionais, *para que* ele passe a constar no quadro de pessoal e, se for o caso, consiga
entrar no sistema.

**Ator e pré-condição:** usuário com papel Admin, autenticado.

| ID | Critério de aceitação (Dado / Quando / Então) |
|---|---|
| CA1 | Dado o Admin autenticado, quando informa First e Last Name válidos e salva, então o funcionário é criado, aparece "Successfully Saved" e abre a tela *Personal Details*. |
| CA2 | Dado o formulário aberto, quando não altera o Employee Id, então é usado o próximo ID sugerido (4 dígitos). |
| CA3 | Dado um Employee Id já usado, quando salva, então aparece "Employee Id already exists" e nada é gravado. |
| CA4 | Dada uma foto GIF/JPG/PNG de até 1 MB, quando salva, então a foto fica no perfil. |
| CA5 | Dado *Create Login Details* ligado com usuário e senhas válidos, quando salva, então é criado também um usuário ESS vinculado. |
| CA6 | Dado qualquer campo inválido, quando tenta salvar, então o erro aparece no campo e nada é gravado. |

**Fluxo principal:** PIM › Add Employee → nomes → Save → `POST /api/v2/pim/employees` → *Personal Details*.

| Fluxos alternativos | Exceções |
|---|---|
| A1 com foto · A2 com login (2ª requisição `POST /api/v2/admin/users`) · A3 com Employee Id manual | E1 nome vazio · E2 ID duplicado · E3 foto inválida · E4 username existente · E5 senhas diferentes · E6 senha fora da política · E7 campo acima do tamanho · E8 username curto · E9 usuário sem permissão |

**Regras confirmadas no código** (não documentadas na interface):

| Campo | Regra |
|---|---|
| First / Middle / Last Name | First e Last obrigatórios; máx. 30 caracteres cada |
| Employee Id | opcional, pré-preenchido; máx. 10; único entre funcionários não expurgados |
| Foto | gif, jpg, jpeg, jfif, png; até 1.048.576 bytes; nome do arquivo até 100 caracteres |
| Username | 5 a 40 caracteres; único |
| Password | até 64; política padrão: mín. 8, maiúscula, minúscula, número, especial, sem espaços, força *strong* |
| Confirm Password | igual à senha |

**Riscos levantados na leitura do código:** R1, criação não atômica de funcionário e usuário; R2, contador de ID
não incrementado no cadastro com foto; R3, limite de 1 MB só no cliente; R4, unicidade do ID ignora expurgados.

### 2.3 Base: análise dos testes existentes

O plano foi desenhado a partir de duas análises críticas, feitas com a grade da disciplina (nível, técnica,
cobertura, isolamento, oráculo, legibilidade, lacunas):

- **Testes do código original** (`analise/codigo-original.md`): bons testes de API em PHPUnit, mas com oráculo que
  compara só a resposta (efeitos no banco nunca são conferidos), sem análise de valor-limite e sem nenhum teste
  de sistema do PIM.
- **Testes de terceiros** (`analise/terceiros.md`): três projetos (pytest/Selenium, TestNG/Selenium e Playwright
  gerado por IA). Todos são só de sistema e nenhum aplica técnicas de projeto de forma sistemática. Um deles tem
  oráculo tautológico, outro tem oráculo desatualizado.

## 3. Casos de teste

Projeto completo, com classes de equivalência, tabela de decisão e matriz de rastreabilidade, em
`docs/casos-de-teste.md`.

### 3.1 Níveis, ferramentas e técnicas

| Nível | Alvo | Ferramenta | Técnicas |
|---|---|---|---|
| Unitário | classes PHP do PIM e serviços usados (`EmployeeAPI`, `IDGeneratorService`, `PasswordStrengthService`) | PHPUnit 9.6 com a infraestrutura de testes do OrangeHRM | particionamento em classes de equivalência, valor-limite, teste guiado por risco, mocks |
| Integração | API REST v2 (`/pim/employees`, `/admin/users`) | Java 17, JUnit 5, REST Assured | equivalência, valor-limite (testes parametrizados), tabela de decisão |
| Sistema | a jornada pela interface | Java 17, JUnit 5, Playwright for Java, Page Objects | casos de uso (fluxos e exceções), valor-limite, tabela de decisão |

**Critérios.**
- **Oráculo:** as regras confirmadas no código e as mensagens oficiais dos arquivos de tradução do OrangeHRM.
- **Entrada:** ambiente no ar e política de senha padrão.
- **Saída:** todos os casos de prioridade alta executados e cada defeito registrado como Issue.
- **Independência:** dados únicos por execução e limpeza de tudo o que o teste cria.

### 3.2 Classes de equivalência e valores-limite (resumo)

| Campo | Válidas | Inválidas | Limites |
|---|---|---|---|
| First / Last Name | 1–30 | vazio, > 30 | 0, 1, 30, 31 |
| Employee Id | vazio, 1–10 único | > 10, duplicado | 10, 11 |
| Foto | gif/jpg/jpeg/jfif/png ≤ 1 MB | outro tipo, > 1 MB, nome > 100 | 1 MB − 1, 1 MB, 1 MB + 1 |
| Username | 5–40 único | < 5, > 40, existente | 4, 5, 40, 41 |
| Password | 8–64 dentro da política | < 8, sem maiúscula, sem minúscula, sem número, sem especial, com espaço, > 64, previsível | 7, 8, 9, 64, 65 |

### 3.3 Tabela de decisão (foto × login)

| Regra | Foto | Login | Dados | Esperado |
|---|---|---|---|---|
| R-1 | não | não | válidos | funcionário criado |
| R-2 | sim | não | válidos | funcionário + foto |
| R-3 | não | sim | válidos | funcionário + usuário ESS |
| R-4 | sim | sim | válidos | funcionário + foto + usuário |
| R-5 | – | sim | usuário recusado no servidor | nada deveria ser criado (verifica R1) |
| R-6 | – | – | funcionário inválido | nada criado, erro no campo |

### 3.4 Casos planejados

**Unitários (UT), PHPUnit**

| ID | Objetivo | Técnica |
|---|---|---|
| UT-01 | nomes: 30 caracteres aceitos, 31 recusados | valor-limite |
| UT-02 | First/Last obrigatórios; Middle opcional | equivalência |
| UT-03 | Employee Id: 10 aceito, 11 recusado, duplicado recusado, ID de expurgado reutilizável | valor-limite, equivalência |
| UT-04 | foto: tipo, extensão e nome de 100/101 caracteres | equivalência, valor-limite |
| UT-05 | cadastro **com foto** deve avançar o contador de ID (com controle sem foto) | guiado por risco (R2) |
| UT-06 | usuário ESS não cria funcionário e nada é gravado | equivalência |
| UT-07 | ID sugerido com 4 dígitos (0001, 0999→1000, 9999→10000); `incrementId` grava atual + 1 | valor-limite |
| UT-08 | política de senha: uma senha por classe inválida, lista completa de mensagens; limites 7/8/9/64/65 | equivalência, valor-limite |

**Integração (IT), REST Assured**

| ID | Objetivo |
|---|---|
| IT-01 | cadastro mínimo (200) e leitura pelo GET |
| IT-02 | sem First/Last Name → 422 no campo; total de funcionários não muda |
| IT-03 | nomes com 30 (200) e 31 (422) caracteres |
| IT-04 | Employee Id duplicado (422); 10 e 11 caracteres |
| IT-05 | foto PNG gravada e devolvida pelo endpoint da foto |
| IT-06 | pdf, bmp, svg e extensão errada → 422 |
| IT-07 | foto com 1 MB − 1, 1 MB (200) e 1 MB + 1 (422); tamanho declarado falso (422) |
| IT-08 | funcionário + usuário ESS vinculados, com status escolhido |
| IT-09 | username com 4/5/40/41 caracteres e já existente |
| IT-10 | senhas por classe de equivalência (422); 9 e 64 caracteres aceitas; 8 caracteres deveria ser aceita |
| IT-11 | 2ª requisição (usuário) recusada: o que acontece com o funcionário (R1) |
| IT-12 | Employee Id sugerido depois de cadastrar com foto (R2), com controle sem foto |

**Sistema (ST), Playwright**

| ID | Cenário |
|---|---|
| ST-01 | cadastro mínimo: toast, *Personal Details* e nome exibido |
| ST-02 | cadastro com foto |
| ST-03 | cadastro com login; usuário aparece em User Management |
| ST-04 | foto + login com usuário desativado |
| ST-05 | First/Last vazio → "Required" |
| ST-06 | Employee Id existente → "Employee Id already exists" |
| ST-07 | foto pdf ou acima de 1 MB recusada; foto de exatamente 1 MB aceita |
| ST-08 | username existente → "Username already exists" |
| ST-09 | senhas diferentes → "Passwords do not match" |
| ST-10 | senha fora da política → mensagem da política; senha de 8 caracteres deveria ser aceita |
| ST-11 | limites: First Name 31, Employee Id 11, Username 4 e 41 |
| ST-12 | Employee Id sugerido depois de cadastrar com foto (R2), com controle sem foto |
| ST-13 | Cancel volta à lista sem gravar |

### 3.5 Rastreabilidade (resumo)

| Item da jornada | Unitário | Integração | Sistema |
|---|---|---|---|
| Fluxo principal / CA1 | – | IT-01 | ST-01 |
| CA2 (ID sugerido) e R2 | UT-05, UT-07 | IT-12 | ST-12 |
| A1 / CA4 (foto) | UT-04 | IT-05 | ST-02 |
| A2 / CA5 (login) | – | IT-08 | ST-03, ST-04 |
| E1 | UT-02 | IT-02 | ST-05 |
| E2 / A3 | UT-03 | IT-04 | ST-06 |
| E3 e R3 | UT-04 | IT-06, IT-07 | ST-07 |
| E4 | – | IT-09 | ST-08 |
| E5 | – | – (só no cliente) | ST-09 |
| E6 | UT-08 | IT-10 | ST-10 |
| E7 / E8 | UT-01, UT-03 | IT-03, IT-04, IT-09 | ST-11 |
| E9 | UT-06 | – | – |
| R1 | – | IT-11 | – |

## 4. Uso de IA e fontes

- **IA:** Claude Code (Anthropic, modelo Claude Opus 5.5) foi usado para:
  - levantar as regras no código-fonte;
  - escrever a primeira versão da jornada, dos casos e deste relatório;
  - ler e analisar os testes existentes.

  Todo o material foi revisado pelo autor. O registro detalhado (data e finalidade) está no diário do trabalho.
- **Fontes:**
  - OrangeHRM, https://github.com/orangehrm/orangehrm (commit `56e23b3b`); arquivos citados em `docs/jornada.md`.
  - Projetos de terceiros citados em `analise/terceiros.md`.
  - PHPUnit (https://phpunit.de), REST Assured (https://rest-assured.io), Playwright for Java (https://playwright.dev/java).
  - Material da disciplina (técnicas de projeto de casos de teste).
