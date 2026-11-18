# Implementação dos testes — #28

Testes próprios da jornada de cadastro e atualização de contatos de emergência, conforme o [plano da #27](../docs/plano-de-testes.md). Todos os arquivos desta contribuição ficam em `suzan-stockey/`. Não há workflow, Docker ou novo serviço. Os testes PHP carregam o código original do OrangeHRM; a suíte de sistema interage pela interface, sem substituir chamadas ou respostas do produto.

## Dependências

- Clone OrangeHRM na revisão `56e23b3b09e7af29317a0943523b825843fff527`, com dependências de `src/composer.lock` instaladas, incluindo PHPUnit 9.6. O PHP deve ser compatível com essa revisão (referência da #26: PHP 7.4).
- Para integração: banco de testes do OrangeHRM já preparado pelo suporte oficial. O bootstrap original usa `ENVIRONMENT=test`. Nunca apontar a configuração de testes para produção ou para o banco da demo. A preparação oficial está documentada em `devTools/core/console.php i:create-test-db`; a descrição da execução original está em [ambiente-local.md](../docs/ambiente-local.md).
- Para sistema: Node.js 18 ou superior, Playwright e Chromium; instância acessível em inglês. Usar funcionário sintético próprio e dedicado, sem contatos, com identificador de funcionário e `empNumber` conhecidos.

## Unitários — UT01 a UT04

No PowerShell, a partir da raiz do repositório da turma, defina o caminho do clone original e o diretório dos testes:

```powershell
$env:OHRM_ROOT = (Resolve-Path '../fontes/orangehrm').Path
$t2Tests = (Resolve-Path 'suzan-stockey/testes/php').Path
php "$env:OHRM_ROOT/src/vendor/bin/phpunit" --no-configuration --bootstrap "$t2Tests/bootstrap-unitario.php" --testdox "$t2Tests/unitarios"
```

O bootstrap unitário carrega somente o autoload original. Não prepara banco nem instancia o EntityManager. UT04 usa `Employee` e `EmpEmergencyContact` em memória e DAO mockado. UT03 instancia `Rules::LENGTH`, a mesma classe usada pelo endpoint, com as constantes originais de limite; os valores esperados vêm do plano.

## Integração — IT01 e IT02

Com o mesmo `OHRM_ROOT` e o banco de testes já preparado:

```powershell
php "$env:OHRM_ROOT/src/vendor/bin/phpunit" --no-configuration --bootstrap "$t2Tests/bootstrap-integracao.php" --testdox "$t2Tests/integracao"
```

Os casos usam o bootstrap oficial e serviço/DAO reais. Cada caso abre uma transação, cria um funcionário sintético com número livre, grava seu contato, limpa o EntityManager e consulta novamente. O rollback no `tearDown` desfaz os dados do caso, inclusive quando uma asserção falha. Não há truncamento de tabelas ou alteração de funcionário existente. Executar a suíte sem concorrência no mesmo banco de testes.

Esses testes verificam a persistência por serviço/DAO; não são testes HTTP nem substituem as validações da interface.

## Sistema — ST01 a ST06

```powershell
cd suzan-stockey/testes/sistema
npm ci
npx playwright install chromium
$env:OHRM_URL = 'https://sua-instancia/web/index.php'
$env:OHRM_USER = 'usuario-de-teste'
$env:OHRM_PASSWORD = 'senha-de-teste'
$env:OHRM_EMPLOYEE_ID = 'identificador-do-funcionario-sintetico'
$env:OHRM_EMP_NUMBER = 'numero-interno-do-mesmo-funcionario'
npm run list
npm test
```

Substituir os valores de exemplo pelos dados do ambiente. Não versionar credenciais. `OHRM_EMPLOYEE_ID` é o campo Employee Id usado na busca; `OHRM_EMP_NUMBER` é o número presente na URL do perfil. O teste confere que o perfil aberto corresponde ao número informado.

A preparação faz login, busca o funcionário em Employee List e abre Emergency Contacts. A suíte usa um único worker e não repete automaticamente os casos. Antes de gravar, exige que o funcionário dedicado esteja sem contatos. Cada caso cria no máximo um contato e a limpeza pela interface remove esse registro, inclusive se o produto aceitar uma entrada esperada como inválida. Não usar esse funcionário simultaneamente em outra sessão de testes.

Os seletores usam rótulos visuais e os ícones de edição/exclusão do componente original. As asserções conferem mensagens, conteúdo, quantidade de registros e os valores reabertos após recarregar. Exclusão é apenas limpeza. Resultados e capturas de falhas são gerados em `test-results/` e `playwright-report/`; selecione as evidências pertinentes para o relatório da #29. As saídas geradas e `node_modules` não são arquivos da implementação.

## Matriz caso → código

| Plano | Arquivo / método ou título | Variantes |
|---|---|---:|
| UT01 | `php/unitarios/ValidacoesContatoTest.php` → `testUT01Obrigatorios` | 3 |
| UT02 | Mesmo arquivo → `testUT02FormatoOriginal` | 4 |
| UT03 | Mesmo arquivo → `testUT03LimiteOriginal` | 15 |
| UT04 | `php/unitarios/ServicoContatoTest.php` → `testUT04DelegacaoSemBanco` | 1 |
| IT01 | `php/integracao/PersistenciaContatoTest.php` → `testIT01CadastroConsultaEVinculo` | 1 |
| IT02 | Mesmo arquivo → `testIT02AtualizacaoSemDuplicacao` | 1 |
| ST01 | `sistema/specs/contatos.spec.js` → `ST01 cadastro e atualizacao persistidos` | 1 |
| ST02 | Mesmo arquivo → `ST02 cancelar` criação/edição | 2 |
| ST03 | Mesmo arquivo → `ST03` obrigatório vazio/espaços | 4 |
| ST04 | Mesmo arquivo → `ST04 telefone` nenhum/residencial/celular/comercial | 4 |
| ST05 | Mesmo arquivo → `ST05 formato` letras/arroba/sinais | 3 |
| ST06 | Mesmo arquivo → `ST06` campo/comprimento | 15 |

São 12 grupos do plano, com 54 execuções previstas (23 unitárias, 2 de integração e 29 de sistema). A quantidade descoberta não representa aprovação. Permissões e cobertura ampla da API, mencionadas na redação inicial da Issue, foram retiradas do plano revisado da #27 a pedido da autora e não fazem parte desta implementação.

## Verificações de desenvolvimento e registro

A sintaxe JavaScript foi conferida com `node --check` e o Playwright descobriu os 29 casos de sistema com `npm run list`. Essas verificações não acessam a instância e não demonstram aprovação funcional. A execução consolidada dos três níveis e a classificação dos resultados pertencem à #29. O PHP não está disponível no PATH da estação usada nesta implementação; nenhum resultado de execução PHP é declarado aqui.

Para registrar saídas PHPUnit na execução consolidada, acrescentar `--log-junit caminho/resultado.xml` aos comandos e preservar também a saída textual. Conferir a quantidade de testes executados, asserções, falhas e código de saída; não considerar somente o término do comando como aprovação. Não reutilizar os relatórios da linha de base como resultado destes casos próprios.

## Fontes e uso de IA

Os arquivos originais de serviço/DAO, os testes existentes e os componentes `SaveEmergencyContact.vue`, `EditEmergencyContact.vue` e `EmployeeEmergencyContacts.vue` foram consultados na revisão fixada. O produto e seus testes de referência são disponibilizados pelo projeto OrangeHRM sob GPL-3.0; as regras do produto não foram copiadas para criar uma implementação alternativa no projeto individual.

Código e documentação elaborados com auxílio do ChatGPT/Codex (OpenAI), a pedido de Suzan Stockey Pereira, com base no escopo que ela revisou e na exploração manual que executou. As verificações acima são de desenvolvimento; os resultados funcionais serão os efetivamente obtidos e registrados na #29.
