# Registro de preparação e execução dos testes originais

## Resultado no GitHub Actions — 17/11/2026

**16 testes executados, 56 asserções, zero falhas, erros, avisos ou casos ignorados**, conforme relatório JUnit. Código de saída do PHPUnit: 0. A saída textual registra `OK (16 tests, 56 assertions)`, tempo 0,137 s e memória 26 MB.

| Classe original | Testes |
|---|---:|
| EmpEmergencyContactDaoTest | 9 |
| EmpEmergencyContactServiceTest | 6 |
| EmpEmergencyContactTest (entidade) | 1 |

Execução: [GitHub Actions 44174448568](https://github.com/masmangan/literate-lamp/actions/runs/44174448568), job `originais`, concluído com sucesso. Código da contribuição usado pelo job: `a2ca05cb280907f43657d0f2e95def07790cdf5d`. Código oficial: `56e23b3b09e7af29317a0943523b825843fff527`, sem alteração dos três arquivos de teste.

Ambiente efetivo: Ubuntu 24.04, PHP 7.4.33, Composer 2.8.12, PHPUnit 9.6.16 e MariaDB 10.11.19. As versões estão preservadas em [ambiente.txt](../resultados/originais/ambiente.txt). A execução do PHPUnit ocorreu às 20:57 UTC (17:57 de Brasília).

Comando executado na raiz do clone oficial:

```bash
./src/vendor/bin/phpunit --colors=never --testdox \
  --filter '(EmpEmergencyContactDaoTest|EmpEmergencyContactServiceTest|EmpEmergencyContactTest)' \
  --log-junit ../suzan-stockey/resultados/originais/phpunit-junit.xml \
  src/plugins/orangehrmPimPlugin/test
```

Evidências preservadas:

- [Saída testdox](../resultados/originais/phpunit-testdox.txt).
- [JUnit XML](../resultados/originais/phpunit-junit.xml).
- [Código de saída](../resultados/originais/exit-code.txt).
- [Artefato original do job](https://github.com/masmangan/literate-lamp/actions/runs/44174448568/artifacts/14369351590).

Os totais do JUnit foram conferidos contra a saída textual. Os testes passaram nas condições dessa execução; esse resultado não demonstra cobertura das validações e dos fluxos ausentes nos arquivos analisados, discutidos na [análise crítica](analise-testes-originais.md).

## Histórico da preparação local

- Código oficial clonado e selecionado na revisão `56e23b3b09e7af29317a0943523b825843fff527`.
- Projeto Compose exclusivo `t2-suzan-emergency-contacts`, com PHP 7.4 e MariaDB 10.11 configurados.
- Fluxo de instalação e criação do banco de testes baseado no CI oficial.
- Filtro definido para os três arquivos originais analisados: serviço, DAO e entidade de contatos de emergência.
- Scripts de preparação e execução com relatórios textual/JUnit e preservação do código de retorno do PHPUnit.

## Verificações realizadas

| Verificação | Resultado |
|---|---|
| Revisão do clone oficial | `56e23b3b09e7af29317a0943523b825843fff527` |
| `docker compose config --quiet` | Código de saída 0 |
| `bash -n scripts/preparar.sh` | Código de saída 0 |
| `bash -n scripts/executar-originais.sh` | Código de saída 0 |
| `docker compose up -d --build db phpunit` | Código de saída 1: motor Linux do Docker inacessível |

O cliente Docker instalado informou versão 29.8.0 e contexto `desktop-linux`. A inicialização encontrou ausência do canal `dockerDesktopLinuxEngine`. O Docker Desktop foi iniciado pela CLI, mas o motor não disponibilizou a API. A tentativa de iniciar `com.docker.service`, encontrado parado, foi recusada pelo Windows por falta de acesso ao serviço.

A saída efetiva está em [inicializacao-docker.txt](../resultados/originais/inicializacao-docker.txt). A falha ocorreu na infraestrutura, antes de instalar as dependências e iniciar o PHPUnit. Nenhum teste foi executado nessa tentativa local. A execução bem-sucedida foi realizada posteriormente no GitHub Actions, sem depender do Docker da autora.

A primeira configuração de CI também falhou na construção da imagem PHP 7.4/Bullseye por respostas 404 dos repositórios de pacotes Debian. O workflow foi simplificado para PHP diretamente no runner e MariaDB como serviço do job. Os testes originais não foram alterados para resolver falhas de infraestrutura.

## Reprodução

O workflow do PR prepara o banco e executa o filtro automaticamente no GitHub Actions. A documentação de [ambiente-local.md](ambiente-local.md) descreve esse fluxo e a alternativa de reprodução local.

## Uso de IA

ChatGPT/Codex (OpenAI) preparou os arquivos, validou a configuração e a sintaxe dos scripts, configurou a execução no GitHub Actions e conferiu os relatórios a pedido de Suzan Stockey Pereira. O GitHub Actions executou os testes automatizados. Os resultados acima correspondem às saídas reais dessas operações.
