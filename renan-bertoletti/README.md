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

Clone o OrangeHRM **fora** do repositório da turma, ao lado dele (é o caminho padrão do compose):

```powershell
cd ..\..            # pasta que contém literate-lamp
git clone https://github.com/orangehrm/orangehrm.git
cd orangehrm
git checkout 56e23b3b09e7af29317a0943523b825843fff527
```

Para outro caminho, defina `OHRM_SRC` antes dos comandos do compose (PowerShell: `$env:OHRM_SRC = "C:\caminho"`).

Suba os contêineres e prepare o clone (dependências, instalação e banco de testes):

```powershell
cd literate-lamp\renan-bertoletti\ambiente
docker compose up -d --build db phpunit
docker compose exec phpunit bash /scripts/preparar-phpunit.sh
```

O compose monta `unitario/` e `integracao/` dentro do plugin Claim do clone
(`src/plugins/orangehrmClaimPlugin/test/T2ClaimUnitario` e `T2ClaimIntegracao`), em modo somente leitura; nada
é copiado para o clone.

## 2. Rodar os testes

> Comandos completados à medida que cada nível é implementado.

## Uso de IA

Código, configuração e redação inicial feitos com Claude Code (Claude Opus 5.5, Anthropic); execução dos testes,
revisão e registro dos resultados feitos pelo autor.
