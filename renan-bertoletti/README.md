# Renan Bertoletti: jornada Claim (reivindicação de despesa)

Contribuição individual ao Trabalho 2 de Verificação e Validação: testes do OrangeHRM na jornada **Claim**, em que
o funcionário submete uma reivindicação de despesa e o supervisor ou o administrador aprova, rejeita ou paga.

| Issue | Entrega | Onde |
|---|---|---|
| #5 | Análise crítica dos testes existentes (código original e terceiros) | [`analise/claim-testes-existentes.md`](analise/claim-testes-existentes.md) |
| #6 | Plano de testes: jornada, máquina de estados, casos UT/IT/ST | [`plano/plano-de-testes-claim.md`](plano/plano-de-testes-claim.md) |
| #7 | Implementação e execução dos testes | este README e as pastas abaixo |

## Estrutura

| Pasta | Conteúdo | Nível | Ferramenta | Roda contra |
|---|---|---|---|---|
| [`ambiente/`](ambiente/) | `docker-compose.yml` com MariaDB e PHP 8.3 | – | Docker | – |
| `unitario/` | UT-01 a UT-07 | Unitário | PHPUnit 9.6 | clone do OrangeHRM (sem banco) |
| `integracao/` | IT-01 a IT-26 | Integração | PHPUnit 9.6 + fixtures YAML | clone do OrangeHRM + banco de teste |
| `sistema/` | ST-01 a ST-12 | Sistema | Java 17+, Selenium WebDriver, JUnit 5 (Maven) | servidor de demonstração |
| [`plano/evidencias/`](plano/evidencias/) | conferência dos estados no servidor (#6) | – | Node 18+ | servidor de demonstração |

## Pré-requisitos

- **Docker Desktop** (Compose v2) aberto, para os testes PHPUnit.
- **Git**, para clonar o OrangeHRM.
- **Java 17+** e **Maven 3.8+**, para os testes de sistema; Google Chrome instalado (o Selenium Manager baixa o
  driver sozinho).

Versão do sistema sob teste: `orangehrm/orangehrm`, branch `main`, commit
`56e23b3b09e7af29317a0943523b825843fff527` (a mesma da análise e do plano).

## 1. Preparar o ambiente PHPUnit (uma vez)

O ambiente reaproveita a imagem PHP e o script de preparação de Mateus Neubarth
([`../mateus-neubarth/docs/ambiente.md`](../mateus-neubarth/docs/ambiente.md)), com outro nome de projeto
(`t2-claim`) e outra porta (3308), para não colidir com o ambiente dele.

### 1.1 Clonar o OrangeHRM no commit analisado

O clone fica **fora** do repositório da turma. O caminho padrão do compose é ao lado dele (`../../../orangehrm`,
como fez Mateus); no Windows, se o repositório estiver dentro do OneDrive, prefira uma pasta fora dele, porque o
`composer install` cria dezenas de milhares de arquivos que o OneDrive tentaria sincronizar.

Só o commit analisado é baixado (`--depth 1`), e sem conversão de fim de linha para CRLF (`core.autocrlf false`),
porque os arquivos são usados dentro de um contêiner Linux:

```powershell
mkdir C:\Users\<usuario>\orangehrm
cd C:\Users\<usuario>\orangehrm
git init
git config core.autocrlf false
git remote add origin https://github.com/orangehrm/orangehrm.git
git fetch --depth 1 origin 56e23b3b09e7af29317a0943523b825843fff527
git checkout FETCH_HEAD
git log -1 --format=%H     # deve mostrar 56e23b3b09e7af29317a0943523b825843fff527
```

### 1.2 Subir os contêineres e preparar o clone

`OHRM_SRC` diz ao compose onde está o clone; defina-o em cada terminal novo (dispensável se o clone estiver no
caminho padrão):

```powershell
cd <caminho>\literate-lamp\renan-bertoletti\ambiente
$env:OHRM_SRC = "C:\Users\<usuario>\orangehrm"
docker compose up -d --build db phpunit
docker compose exec phpunit bash -c "tr -d '\r' < /scripts/preparar-phpunit.sh | bash"
```

O `tr -d '\r'` remove o CR que o Git para Windows (`core.autocrlf true`) acrescenta ao script ao fazer checkout
do repositório da turma; sem ele, o `bash` do contêiner falha na primeira linha.

A preparação (`composer install`, instalação no banco `orangehrm_dev` e criação do banco de testes) leva alguns
minutos na primeira vez e termina com `>> pronto.`

O compose monta `unitario/` e `integracao/` dentro do plugin Claim do clone
(`src/plugins/orangehrmClaimPlugin/test/T2ClaimUnitario` e `T2ClaimIntegracao`), em modo somente leitura; nada
é copiado para o clone.

## 2. Rodar os testes

> Comandos completados à medida que cada nível é implementado.

Os comandos PHPUnit são executados a partir de `renan-bertoletti/ambiente` (com `$env:OHRM_SRC` definido, se o
clone não estiver no caminho padrão).

Os testes que expõem defeitos ficam em **quarentena** (`@group defeito-conhecido`): fora da execução padrão, que
deve passar, e rodados à parte, onde se espera que falhem.

### 2.1 Unitários ([`unitario/`](unitario/README.md))

```powershell
docker compose exec phpunit ./src/vendor/bin/phpunit --group T2ClaimUnitario --exclude-group defeito-conhecido   # padrão: 34, devem passar
docker compose exec phpunit ./src/vendor/bin/phpunit --group defeito-conhecido --filter T2ClaimUnitario          # quarentena: 1, falha (R2)
```

### 2.2 Integração ([`integracao/`](integracao/README.md))

```powershell
docker compose exec phpunit ./src/vendor/bin/phpunit --group T2ClaimIntegracao --exclude-group defeito-conhecido  # padrão: 109, devem passar
docker compose exec phpunit ./src/vendor/bin/phpunit --group defeito-conhecido --filter T2ClaimIntegracao         # quarentena: 1, falha (R2)
```

Todos os testes em quarentena dos dois níveis (2, ambos expõem R2):

```powershell
docker compose exec phpunit ./src/vendor/bin/phpunit --group defeito-conhecido --filter T2Claim
```

## Uso de IA

Código, configuração e redação inicial feitos com Claude Code (Claude Opus 5.5, Anthropic); execução dos testes,
revisão e registro dos resultados feitos pelo autor.
