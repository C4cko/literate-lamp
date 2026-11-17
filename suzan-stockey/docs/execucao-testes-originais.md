# Registro de preparação e execução dos testes originais

## Ambiente preparado em 17/11/2026

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

A saída efetiva está em [inicializacao-docker.txt](../resultados/originais/inicializacao-docker.txt). A falha ocorreu na infraestrutura, antes de instalar as dependências e iniciar o PHPUnit. **Nenhum teste original foi executado nesta tentativa; não há contagem de aprovados ou reprovados.** A análise estática dos métodos permanece distinta desse registro de execução.

## Reprodução

Com o motor Linux ativo, os comandos de [ambiente-local.md](ambiente-local.md) preparam o banco e executam o filtro dos testes originais. O script grava automaticamente versões, saída textual, JUnit XML e código de saída em `resultados/originais/`.

## Uso de IA

ChatGPT/Codex (OpenAI) preparou os arquivos, validou a configuração e a sintaxe dos scripts e realizou as tentativas de inicialização a pedido de Suzan Stockey Pereira. Os resultados acima correspondem às saídas reais dessas operações.
