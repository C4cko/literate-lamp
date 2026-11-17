# Mateus Neubarth: testes da jornada PIM › Add Employee (OrangeHRM)

Contribuição individual no trabalho de Verificação e Validação: análise crítica dos testes existentes e testes
unitários, de integração e de sistema da jornada **Admin cadastra um funcionário** do OrangeHRM 5.9.

## Comece por aqui

| Entrega | Onde |
|---|---|
| Relatório do Plano de Testes (introdução, objetivos, casos de teste) | [`relatorios/plano-de-testes.md`](relatorios/plano-de-testes.md) e Issue #52 |
| Relatório da Execução dos Testes (resultados e defeitos) | [`relatorios/relatorio-execucao.md`](relatorios/relatorio-execucao.md) e Issue #53 |
| Defeitos encontrados (D1, D2) | [`docs/defeitos.md`](docs/defeitos.md) e Issue #22 |

## Jornada

O administrador de RH acessa **PIM › Add Employee**, informa nome (e, opcionalmente, Employee Id, foto e dados de
login) e salva. O funcionário é criado e, se pedido, também um usuário do sistema.

## Conteúdo da pasta

| Pasta | Conteúdo | Issue |
|---|---|---|
| `analise/` | análise crítica dos testes do código original e de três projetos de terceiros | #2, #4 |
| `docs/` | jornada de usuário, projeto dos casos de teste, ambiente e defeitos | #9, #10, #22 |
| `ambiente/` | `docker compose` com MariaDB, OrangeHRM 5.9 e PHPUnit | #10 |
| `unitario/` | testes PHPUnit (UT-01..08), executados dentro do plugin PIM do OrangeHRM | #10 |
| `integracao/` | testes da API REST v2 em Java (JUnit 5 + REST Assured), IT-01..12 | #14 |
| `sistema/` | testes pela interface em Java (JUnit 5 + Playwright), ST-01..13 | #18 |
| `relatorios/` | os dois relatórios do enunciado | #52, #53 |

Os testes rodam localmente com o ambiente Docker de `ambiente/`; o passo a passo está em
[`docs/ambiente.md`](docs/ambiente.md).

## Resultados por nível

| Nível | Testes | Execução padrão | Quarentena (`defeito-conhecido`) |
|---|---|---|---|
| Unitário (PHPUnit) | 49 | 47 passam | 2 falham: UT-05 com foto (D1), UT-08 com 8 caracteres (D2) |
| Integração (REST Assured) | 41 | 39 passam | 2 falham: IT-12 com foto (D1), IT-10 com 8 caracteres (D2) |
| Sistema (Playwright) | 23 | 21 passam | 2 falham: ST-12 com foto (D1), ST-10 com 8 caracteres (D2) |
| **Total** | **113** | **107 passam** | **6 falham: 2 defeitos, detectados nos três níveis** |

Defeitos: **D1**, o cadastro com foto não avança o Employee Id sugerido; **D2**, uma senha de 8 caracteres
nunca é aceita, embora o mínimo anunciado seja 8 (inconsistência de especificação).

## Uso de IA

Claude Code (Anthropic, modelo Claude Opus 5.5) foi usado como assistente de programação e de redação. Os
commits feitos com ele levam a linha `Co-Authored-By: Claude`. Todo o conteúdo foi revisado pelo autor. A
finalidade de cada uso está nas seções "Uso de IA" dos documentos e dos relatórios.
