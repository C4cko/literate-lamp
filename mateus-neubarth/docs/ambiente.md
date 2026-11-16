# Ambiente de testes e como rodar os testes

> Issue #E. Tudo roda em contêineres: a máquina precisa só de **Docker** (com Compose v2), **Java 17+** e
> **Maven 3.8+**. Testado em Linux com Docker 29.9, Java 21 e Maven 3.8.7.

## 1. Visão geral

| Serviço (`ambiente/docker-compose.yml`) | Imagem | Papel | Porta no host |
|---|---|---|---|
| `db` | `mariadb:10.11` | bancos `orangehrm_app` (sistema sob teste), `orangehrm_dev` e `test_orangehrm_dev` (PHPUnit) | 3307 |
| `app` | `orangehrm/orangehrm:5.9` (imagem oficial) | **sistema sob teste** dos testes de integração e de sistema | 8200 → http://localhost:8200 |
| `phpunit` | construída de `ambiente/phpunit/Dockerfile` (PHP 8.3 CLI + extensões + Composer + PCOV) | roda o PHPUnit sobre o clone do código-fonte | – |

Por que não usar o ambiente oficial de desenvolvimento (`orangehrm-os-dev-environment`)? Ele exige editar o
`/etc/hosts` e ocupar as portas 80/443 da máquina. Aqui as mesmas etapas do CI oficial
(`.github/workflows/test.yml`) foram reproduzidas num compose pequeno e isolado.

A versão 5.9 da imagem corresponde ao commit analisado (`56e23b3b`, "Update Dockerfile for OrangeHRM 5.9").

## 2. Subir o sistema sob teste (uma vez)

```bash
cd mateus-neubarth/ambiente
docker compose up -d db app
docker compose exec app php installer/cli_install.php   # instalação não interativa (config/cli_install_app.yaml)
```

Acesse http://localhost:8200 e entre com **Admin / Admin@OHRM123**.

> O instalador avisa que não conseguiu apagar `cli_install_config.yaml`: é esperado, porque o arquivo é montado
> como somente leitura. Ele só tem credenciais do ambiente local.

## 3. Testes unitários (PHPUnit)

O serviço `phpunit` monta o clone de `orangehrm/orangehrm` em `/app`. Por padrão o clone fica três níveis acima
de `ambiente/` (`../../../orangehrm`, ao lado do clone do repositório da turma); para outro caminho, use
`OHRM_SRC=/caminho docker compose ...`. O clone deve estar no commit `56e23b3b`.

```bash
# uma vez: dependências, instalação no banco orangehrm_dev e banco de testes
docker compose up -d phpunit
docker compose exec phpunit bash /scripts/preparar-phpunit.sh

# rodar
docker compose exec phpunit ./src/vendor/bin/phpunit --testsuite Pim
docker compose exec phpunit ./src/vendor/bin/phpunit src/plugins/orangehrmPimPlugin/test/Api/EmployeeAPITest.php
docker compose exec phpunit ./src/vendor/bin/phpunit --filter EmployeeAPITest --testdox

```

> As seções sobre os testes de integração e de sistema entram junto com esses testes.

## 4. Desligar e recomeçar do zero

```bash
docker compose down          # para os contêineres (mantém os bancos)
docker compose down -v       # apaga também os bancos; repetir as seções 2 e 3 depois
```

## 5. Problemas encontrados ao montar o ambiente

| Problema | Causa | Solução |
|---|---|---|
| Suíte do PIM falha na 2ª execução (`CustomFieldAPITest::testDelete`) | testes originais dependentes de ordem/estado (ver `analise/codigo-original.md`) | recriar o banco de testes antes de cada execução completa: `docker compose exec phpunit php devTools/core/console.php i:create-test-db -p root --dump-options=--ssl=0` |
| Depois de `docker compose up`, a aplicação responde 500 e pede reinstalação | o contêiner `app` foi recriado e `/var/www/html` ficava num volume anônimo (perdia `lib/confs/Conf.php`) | volume nomeado `app-html`; se acontecer, apagar o banco `orangehrm_app` e rodar o instalador de novo |

## 6. Execução dos testes originais (12/11/2026)

Clone `orangehrm/orangehrm` no commit `56e23b3b`, PHP 8.3.x, MariaDB 10.11, PHPUnit 9.6.

| Execução | Comando | Resultado | Tempo |
|---|---|---|---|
| Suíte completa do PIM | `phpunit --testsuite Pim` | **674 testes, 2046 asserções, todos passaram**; 53 marcados como *risky* | 4 min 47 s |
| Testes da jornada em outros plugins (`IDGeneratorServiceTest`, `UserAPITest`, `PasswordStrengthServiceTest`, `PasswordStrengthValidationAPITest`) | `phpunit --filter '/(...)/' src/plugins` | **55 testes, 139 asserções, todos passaram** | 11 s |

Os 53 *risky* são todos "This test printed output": avisos de depreciação do PHP 8.3 (criação de propriedade
dinâmica nas classes de teste, ex. `EmploymentContractDaoTest::$fixture`) impressos durante o teste. Nenhum
deles pertence aos testes da jornada; o problema é de compatibilidade das classes de teste com o PHP 8.2+, não
do código de produção.

### Cobertura das classes da jornada

Testes relacionados à jornada (`EmployeeAPITest`, `EmployeeServiceTest`, `EmployeePictureServiceTest`,
`EmployeePictureAPITest`, `IDGeneratorServiceTest`, `UserAPITest`, `PasswordStrengthServiceTest`,
`PasswordStrengthValidationAPITest`: 154 testes), com PCOV e `--coverage-text`:

| Classe | Métodos | Linhas |
|---|---|---|
| `Pim\Api\EmployeeAPI` | 93,33% (14/15) | **99,66%** (289/290) |
| `Core\Service\IDGeneratorService` | 100% (5/5) | **100%** (25/25) |
| `Pim\Service\EmployeePictureService` | 100% (9/9) | 100% (43/43) |
| `Admin\Api\UserAPI` | 84,62% (11/13) | 86,79% (138/159) |
| `Authentication\Service\PasswordStrengthService` | 61,54% (16/26) | 85,81% (127/148) |
| `Pim\Service\EmployeeService` | 68,00% (17/25) | 69,23% (45/65) |

**Leitura crítica:** `EmployeeAPI::create()` e `IDGeneratorService` estão praticamente 100% cobertos, mas
nenhum teste verifica se o contador de ID avança no cadastro com foto (risco R2): as linhas são *executadas*,
mas o resultado não é *conferido*. Cobertura de código mede o que roda, não o que é verificado; é a qualidade
do oráculo (ver `analise/codigo-original.md`, seção 2) que decide se um defeito é detectado.

Comando usado:

```bash
docker compose exec phpunit ./src/vendor/bin/phpunit --coverage-text --colors=never \
  --filter '/(EmployeeAPITest|EmployeeServiceTest|EmployeePictureServiceTest|EmployeePictureAPITest|IDGeneratorServiceTest|UserAPITest|PasswordStrengthServiceTest|PasswordStrengthValidationAPITest)/' src/plugins
```
