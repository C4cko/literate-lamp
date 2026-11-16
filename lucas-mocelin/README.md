# Trabalho 2 – Teste do OrangeHRM · Jornada "Autenticação (Login)"

Contribuição individual para o relatório unificado da turma.

| Item | Onde está |
|---|---|
| Introdução, compilação e execução | este README |
| Análise crítica dos testes existentes | [`docs/01-analise-critica.md`](docs/01-analise-critica.md) |
| Objetivos, jornada de usuário e casos de teste (**Issue: Plano de Testes**) | [`docs/02-plano-de-testes.md`](docs/02-plano-de-testes.md) |
| Resultado dos testes (**Issue: Execução dos Testes**) | [`docs/03-relatorio-execucao.md`](docs/03-relatorio-execucao.md) |
| Textos prontos das Issues e sequência de commits | [`docs/issues.md`](docs/issues.md) |
| Testes unitários e de integração (PHPUnit) | [`testes/php/`](testes/php) |
| Testes de sistema (Playwright) | [`testes/sistema/`](testes/sistema) |
| Evidências de execução | [`resultados/`](resultados) |

## 1. Introdução

### 1.1 O sistema

O **OrangeHRM Starter** é um sistema de gestão de recursos humanos de código aberto (GPL v3).
Cobre cadastro de funcionários (PIM), férias e ausências (Leave), ponto (Time/Attendance),
recrutamento, avaliação de desempenho, despesas (Claim), rede social interna (Buzz),
entre outros. Site do produto: <https://www.orangehrm.com>.

Arquitetura (versão analisada: **5.9**, commit `56e23b3`):

* **Back-end** em PHP 8 sobre componentes Symfony (HttpKernel, DI, Session) e Doctrine ORM,
  organizado em *plugins* (`src/plugins/orangehrm*Plugin`), cada um com Controller, Service,
  Dao, Api, entidades e pasta `test/`.
* **Front-end** em Vue 3 com a biblioteca de componentes própria **OXD** (`src/client`),
  compilado para `web/dist`.
* **Banco** MySQL/MariaDB.

A jornada escolhida (login) está no plugin `orangehrmAuthenticationPlugin`, com apoio de
`orangehrmAdminPlugin` (UserService/UserDao) e `orangehrmCorePlugin` (ConfigService).

### 1.2 Onde obter

* Código-fonte: <https://github.com/orangehrm/orangehrm>
* Servidor de demonstração: <https://opensource-demo.orangehrmlive.com/web/index.php/auth/login>
  (usuário `Admin`, senha `admin123`)

### 1.3 Como compilar e instalar (ambiente local)

O script [`scripts/ambiente-local.sh`](scripts/ambiente-local.sh) automatiza tudo em Ubuntu 24.04.
Resumo dos passos:

```bash
git clone https://github.com/orangehrm/orangehrm.git && cd orangehrm
(cd src && composer install)                # dependências PHP
(cd devTools/core && composer install)      # ferramentas de desenvolvimento
(cd src/client && yarn install && yarn build)   # front-end -> web/dist
php installer/cli_install.php               # instala (config em installer/cli_install_config.yaml)
php devTools/core/console.php i:create-test-db -p root --dump-options=--ssl=0   # banco do PHPUnit
php -S 127.0.0.1:8080 -t .                  # servidor: http://127.0.0.1:8080/web/index.php
```

Requisitos: PHP 8.x com extensões `pdo_mysql gd intl zip xml mbstring curl ldap`,
Composer 2, Node 18+ com Yarn, MariaDB 10.x/MySQL 8. O OrangeHRM também oferece imagem Docker
(`orangehrm/orangehrm`).

### 1.4 Como rodar os testes

**Testes originais do projeto**

```bash
php src/vendor/bin/phpunit --testsuite Authentication     # PHPUnit (54 testes do plugin)
cd src/client && yarn test:unit                            # Jest (front-end)
cd src/test/functional && yarn install && yarn test        # Cypress (E2E)
```

**Testes desta jornada – unitários e de integração (PHPUnit)**

```bash
scripts/instalar-testes-php.sh /caminho/orangehrm        # copia testes/php para o plugin
cd /caminho/orangehrm
php src/vendor/bin/phpunit src/plugins/orangehrmAuthenticationPlugin/test/JornadaLogin --testdox
# só unitários:   ... --group Unit       só integração:  ... --group Integration
```

**Testes desta jornada – sistema (Playwright)**

```bash
cd testes/sistema
npm install
npx playwright install chromium
npm run test:demo                       # contra o servidor de demonstração público
# ou contra a instalação local (habilita também o ST-17, que lê o e-mail):
ORANGEHRM_URL=http://127.0.0.1:8080/web/index.php ADMIN_PASS='Ohrm@1423' \
MAILBOX_DIR=/tmp/orangehrm-mail npx playwright test
npx playwright show-report              # relatório HTML
```

**Análise de mutação** (compara a suíte original com a da jornada):

```bash
scripts/mutacao.sh /caminho/orangehrm
```

## 2. Resumo dos resultados

| Nível | Ferramenta | Casos | Resultado |
|---|---|---|---|
| Unitário | PHPUnit 9.6 | 32 | 32 passaram |
| Integração | PHPUnit 9.6 + MariaDB | 15 | 15 passaram |
| Sistema | Playwright + Chromium | 19 | 19 passaram |
| **Total** | | **66** | **66 passaram, 0 defeitos funcionais** |

Mutação: a suíte da jornada detectou **9 de 9** defeitos injetados; a suíte original do plugin
detectou **3 de 9**. Foram registradas 4 observações de segurança/usabilidade
(detalhes em [`docs/03-relatorio-execucao.md`](docs/03-relatorio-execucao.md)).


## Uso de IA

Programação e redação assistidas pelo Claude (Anthropic, Claude Opus 5.5), com revisão do autor.
