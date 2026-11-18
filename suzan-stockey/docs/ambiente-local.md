# Ambiente utilizado nos testes originais

A execução registrada da #26 ocorreu no GitHub Actions, com Ubuntu 24.04, PHP 7.4.33, Composer 2.8.12, PHPUnit 9.6.16 e MariaDB 10.11.19. O PHP rodou no executor Linux e o banco foi um serviço temporário do job.

## Código e preparação

Foi utilizada a revisão oficial `56e23b3b09e7af29317a0943523b825843fff527` do OrangeHRM. A preparação seguiu o CI oficial: instalação das dependências pelos lockfiles com Composer, instalador CLI e criação do banco de testes com `devTools/core/console.php i:create-test-db`.

A massa e as credenciais foram sintéticas e exclusivas do ambiente temporário. Os três arquivos de testes originais não foram modificados.

## Execução e evidências

O comando PHPUnit e o resultado estão no [registro da execução](execucao-testes-originais.md): 16 testes e 56 asserções, sem falhas. As versões, a saída textual, o JUnit XML e o código de saída estão preservados em [resultados/originais](../resultados/originais/).

O [job executado](https://github.com/masmangan/literate-lamp/actions/runs/44174448568) e o commit identificado no registro permitem consultar a configuração usada naquela execução. Este documento descreve o ambiente efetivamente utilizado; a entrega mantém os relatórios e as evidências da #26.

O nome deste arquivo foi mantido para preservar os links existentes.

## Referências e uso de IA

- [CI oficial da revisão analisada](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/.github/workflows/test.yml).
- [Análise dos testes originais](analise-testes-originais.md).

Preparação e documentação com auxílio do ChatGPT/Codex (OpenAI), a pedido de Suzan Stockey Pereira. O GitHub Actions executou os testes; a exploração pela interface foi executada por Suzan.
