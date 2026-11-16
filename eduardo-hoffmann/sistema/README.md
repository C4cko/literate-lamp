# Testes de sistema: Time › Attendance (Punch In / Punch Out)

Issue #17. Casos ST-01 a ST-09 de [`../docs/02-casos-de-teste.md`](../docs/02-casos-de-teste.md), em Java 17, JUnit 5
e Selenium, com o padrão Page Object. A jornada roda pela interface; o banco só é usado para preparar e limpar a massa.

## Ambiente

Os testes **gravam** registros de ponto e **mudam a configuração** de Attendance. Por isso rodam na instância LOCAL do
OrangeHRM 5.9 (serviço `app` de `../ambiente/docker-compose.yml`, em http://localhost:8201), nunca no servidor de
demonstração compartilhado.

- Docker (Compose v2), Java 17 ou superior (testado com o JDK 23), Maven 3.9 e Google Chrome.
- Selenium Manager baixa o `chromedriver` compatível na primeira execução (precisa de internet).
- O Chrome usa o fuso horário do sistema, e os testes que semeiam dados usam o mesmo fuso.
- A tela local usa data `yyyy-mm-dd` e hora de 12 h (`hh:mm AM/PM`); os testes digitam nesses formatos.

### Subir a instância local (uma vez)

Em `../ambiente`:

```bash
docker compose up -d db app
docker compose exec app php installer/cli_install.php   # instalação não interativa (config/cli_install_app.yaml)
```

No Git Bash do Windows, prefixe com `MSYS_NO_PATHCONV=1`. O instalador avisa que não conseguiu apagar
`cli_install_config.yaml`: é esperado, o arquivo é montado somente leitura e só tem credenciais do ambiente local.

## Como rodar

Em `sistema/`:

```bash
mvn test                       # navegador sem janela (padrão)
mvn test -Dheadless=false      # com a janela do Chrome visível
mvn test -Dtest='PunchInOutSistemaTest#st05*'    # um caso só
```

Valores padrão, trocáveis com `-D`: `orangehrm.url` (http://localhost:8201), `orangehrm.user` e `orangehrm.password`
(a credencial de `../ambiente/config/cli_install_app.yaml`), e `orangehrm.db.url` (jdbc:mariadb://127.0.0.1:3308/orangehrm_app,
usuário `root`, senha `root`). No Windows sem `JAVA_HOME` definido, aponte-o para o JDK ao chamar o Maven.

## Arquivos

| Arquivo | Papel |
|---|---|
| `Ambiente.java` | URL, credenciais e banco da instância local, com padrões e `-D` |
| `BancoDeTeste.java` | restaura o estado inicial (sem registros e opção desligada), semeia um registro concluído (ST-05) e confere o que a tela gravou |
| `PaginasAttendance.java` | Page Object: login, telas de Punch In/Out, Meus registros e Configuration |
| `PunchInOutSistemaTest.java` | ST-01 a ST-09 (11 métodos) |

## Casos

| Caso | Método | O que verifica |
|---|---|---|
| ST-01 | `st01EntradaLevaParaATelaDePunchOut` | clicar em **In** leva a `/attendance/punchOut` e grava `PUNCHED IN` |
| ST-02 | `st02SaidaAparecemEmMeusRegistros` | **Out** grava `PUNCHED OUT` e o registro aparece em *My Records* |
| ST-03 | `st03NotaDe250CaracteresEhAceita`, `st03NotaDe251CaracteresEhRejeitadaESemGravacao` | limite da nota |
| ST-04 | `st04DataEHoraVaziasMostramRequiredEImpedemOEnvio` | campos obrigatórios (opção ligada) |
| ST-05 | `st05HoraDentroDeRegistroConcluidoMostraOverlappingRecordsFound` | sobreposição |
| ST-06 | `st06NovaEntradaDepoisDaSaidaCriaOutroRegistro` | segundo ciclo cria outro registro |
| ST-07 | `st07EntradaRetroativaGravaOHorarioInformado` | hora retroativa (opção ligada) |
| ST-08 | `st08ComAOpcaoDesligada...`, `st08ComAOpcaoLigada...` | data e hora desabilitadas/habilitadas |
| ST-09 | `st09PunchInComEntradaAbertaRedirecionaParaPunchOut` | redirecionamento pela interface |

## Resultado

Ver o Relatório da Execução dos Testes (comentário final da issue #17).
