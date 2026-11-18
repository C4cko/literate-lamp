# Ambiente de testes — GitHub Actions

O ambiente usa Ubuntu 24.04, PHP 7.4 e MariaDB 10.11. O PHP roda no executor Linux e o banco é um serviço temporário do job. A execução abrange os testes originais de serviço, DAO e entidade de contatos de emergência.

## Preparação e execução

O workflow [suzan-emergency-contacts.yml](../../.github/workflows/suzan-emergency-contacts.yml) faz checkout da contribuição e do OrangeHRM na revisão `56e23b3b09e7af29317a0943523b825843fff527`, prepara PHP com `shivammathur/setup-php`, instala as dependências dos lockfiles originais com Composer, executa o instalador CLI e cria o banco de testes.

A configuração [cli_install_config.yaml](../ambiente/cli_install_config.yaml) fornece os dados sintéticos de instalação; o workflow adapta o host para `127.0.0.1`. As credenciais se destinam exclusivamente ao banco temporário do job. O token tem somente leitura de conteúdo e não utiliza segredos externos.

O filtro do PHPUnit seleciona somente `EmpEmergencyContactDaoTest`, `EmpEmergencyContactServiceTest` e `EmpEmergencyContactTest`, no plugin PIM. Os testes originais não são modificados. O workflow preserva o código de saída do PHPUnit e gera versões do ambiente, saída textual e JUnit XML.

## Reprodução e evidências

Alterações no workflow ou em `suzan-stockey/ambiente/` disparam a execução no PR. Após o workflow estar disponível na branch padrão, também pode ser iniciado pela ação **Run workflow** na aba Actions.

Os relatórios e logs são publicados como artefato por 30 dias, inclusive quando uma etapa falha. O resumo mostra a saída final do PHPUnit. Os resultados usados na entrega ficam preservados em [resultados/originais](../resultados/originais/), sem depender da retenção dos artefatos. O [registro da execução](execucao-testes-originais.md) apresenta o resultado e o link da execução bem-sucedida.

O nome deste arquivo foi mantido para preservar os links existentes.

## Referências e uso de IA

- [CI oficial da revisão analisada](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/.github/workflows/test.yml).
- [Análise dos testes originais](analise-testes-originais.md).

Configuração e documentação preparadas com auxílio do ChatGPT/Codex (OpenAI), a pedido de Suzan Stockey Pereira. O GitHub Actions executou os testes; a exploração pela interface foi executada por Suzan.
