# Ambiente local — testes originais de contatos de emergência

O ambiente usa um projeto Docker Compose exclusivo (`t2-suzan-emergency-contacts`), MariaDB 10.11 e PHP 7.4, versão mínima declarada pelos projetos Composer da revisão analisada. PHP e banco rodam em contêineres; não é necessário instalar PHP no Windows. O banco não publica portas no host e usa um volume exclusivo.

O propósito é executar os testes PHP originais de serviço, DAO e entidade. Não é um servidor web para os testes de sistema. A configuração se baseia no fluxo do CI oficial (instalação Composer, instalador CLI, criação do banco de testes e PHPUnit) e na organização do ambiente compartilhado de Mateus, adaptada para um projeto isolado e PHP 7.4.

## Preparação

Requer Git, Docker Desktop com motor Linux ativo e Docker Compose v2. O clone de OrangeHRM deve ser exclusivo deste ambiente, pois o instalador gera configurações e o preparo cria o banco de testes.

```powershell
git clone https://github.com/orangehrm/orangehrm.git orangehrm
git -C orangehrm checkout 56e23b3b09e7af29317a0943523b825843fff527
$env:OHRM_SRC = (Resolve-Path orangehrm).Path
cd caminho/para/literate-lamp/suzan-stockey/ambiente
docker compose config --quiet
docker compose up -d --build db phpunit
docker compose exec -T phpunit bash /scripts/preparar.sh
```

Na disposição local usada nesta contribuição, o clone está em `t2/fontes/orangehrm` e o repositório da turma em `t2/literate-lamp`. O caminho relativo padrão do Compose já contempla essa disposição. Para outra disposição, defina `OHRM_SRC` com caminho absoluto antes de executar Compose.

As dependências são instaladas com os lockfiles originais, sem atualização de versões. As credenciais no YAML destinam-se somente ao banco local deste projeto.

## Execução e resultados

```powershell
docker compose exec -T phpunit bash /scripts/executar-originais.sh
```

O filtro seleciona somente `EmpEmergencyContactDaoTest`, `EmpEmergencyContactServiceTest` e `EmpEmergencyContactTest`, dentro dos testes do plugin PIM. Os testes originais não são modificados. O script registra versão/commit, saída textual, JUnit XML e código de saída em `suzan-stockey/resultados/originais/`. O código do PHPUnit é preservado mesmo com o uso de `tee`.

Para parar somente este projeto e preservar o banco:

```powershell
docker compose down
```

## Referências e uso de IA

- [CI oficial da revisão analisada](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/.github/workflows/test.yml).
- [Ambiente de Mateus no repositório compartilhado](../../mateus-neubarth/docs/ambiente.md).
- [Análise dos testes originais](analise-testes-originais.md).

Ambiente e scripts preparados por ChatGPT/Codex (OpenAI), a pedido de Suzan Stockey Pereira. A execução automatizada desta etapa será identificada nos resultados; a exploração anterior pela interface foi executada por Suzan.
