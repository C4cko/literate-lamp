# Relatório da Execução dos Testes — #29

**Autora:** Suzan Stockey Pereira (@SuzanStockey). **Execução:** 17/11/2026 (horário de Brasília).
**Jornada:** [#25](https://github.com/masmangan/literate-lamp/issues/25). **Plano:** [#27](https://github.com/masmangan/literate-lamp/issues/27). **Implementação:** [#28](https://github.com/masmangan/literate-lamp/issues/28). **Resultados:** [#29](https://github.com/masmangan/literate-lamp/issues/29).

## Ambiente e código executado

Testes próprios no commit `b806d71b6af4913bf31281cf15eb83436e0c024b`. Fonte OrangeHRM `56e23b3b09e7af29317a0943523b825843fff527`, sem alteração do código do produto. Ambiente: Windows 11 (build 26200), PHP 7.4.33, Composer 2.8.12, PHPUnit 9.6.16, MariaDB 10.11.11, Node.js 24.12.0, Playwright 1.58.2 e Chrome 157.0.8103.79. [Registro das versões](../resultados/execucao-29/ambiente.txt).

A autora autorizou preparar apenas as dependências de execução. PHP/Composer e MariaDB foram usados em distribuições portáteis temporárias; o banco escutou somente em `127.0.0.1:33317`. Não foi instalado serviço do Windows, criado workflow, contêiner ou servidor web. O instalador CLI, a geração de proxies Doctrine e `i:create-test-db` do OrangeHRM prepararam o schema. Os lockfiles originais foram respeitados, sem atualizar dependências.

A integração usou `test_suzan_t2_execucao_dev`, criado para a execução. Ao final, a consulta de conferência encontrou zero funcionários `T2-Suzan-%` após os rollbacks. O processo MariaDB temporário foi encerrado e sua ausência foi conferida.

Sistema: demo fornecida no enunciado, exibindo **OrangeHRM OS 5.9**, com sessão administrativa e funcionário sintético dedicado `T2Suzan Execucao29` (Employee Id `SZT229`, empNumber `171` na reexecução final; `274` na execução anterior). A preparação e os casos não usam dados pessoais reais. A versão da demo não assegura igualdade com o commit do clone. A suíte verifica estado vazio e limpa o contato próprio ao final de cada caso.

## Comandos

Com `OHRM_ROOT` apontando ao clone original e PHP portátil disponível como `php` (nesta execução foi usado o caminho absoluto do executável):

```powershell
$t2Tests = (Resolve-Path 'suzan-stockey/testes/php').Path
php "$env:OHRM_ROOT/src/vendor/bin/phpunit" --no-configuration --bootstrap "$t2Tests/bootstrap-unitario.php" --testdox --log-junit unitarios.xml "$t2Tests/unitarios"
php "$env:OHRM_ROOT/src/vendor/bin/phpunit" --no-configuration --bootstrap "$t2Tests/bootstrap-integracao.php" --testdox --log-junit integracao.xml "$t2Tests/integracao"
```

Na raiz do repositório, depois de configurar as variáveis da demo e do funcionário conforme o [README](../testes/README.md):

```powershell
$env:OHRM_BROWSER_CHANNEL = 'chrome'
$env:PLAYWRIGHT_JUNIT_OUTPUT_FILE = 'caminho/absoluto/sistema.xml'
npx --prefix suzan-stockey/testes/sistema playwright test --config=suzan-stockey/testes/sistema/playwright.config.js --reporter=list,junit --max-failures=1
```

A execução completa foi sequencial, com um worker e sem retries automáticos. A evidência preserva a saída de cada variante, os JUnit XML e os códigos de saída. Não foi considerado aprovado um caso apenas porque o comando terminou.

## Resultados finais

| Nível | Executados | Aprovados | Falhas/erros finais | Asserções PHP | Evidência |
|---|---:|---:|---:|---:|---|
| Unitário | 23 | 23 | 0 | 30 | [JUnit](../resultados/execucao-29/unitarios.xml) e [saída](../resultados/execucao-29/unitarios.txt) |
| Integração | 2 | 2 | 0 | 19 | [JUnit](../resultados/execucao-29/integracao.xml) e [saída](../resultados/execucao-29/integracao.txt) |
| Sistema | 29 | 29 | 0 | Não contabilizadas | [JUnit](../resultados/execucao-29/sistema.xml) e [saída](../resultados/execucao-29/sistema.txt) |
| **Total próprio** | **54** | **54** | **0** | **49 nos testes PHP** | [Matriz por variante](../resultados/execucao-29/matriz-resultados.csv) |

Todos os casos previstos foram executados e aprovados na rodada final, sem casos ignorados. Os três comandos finais retornaram zero. As ocorrências anteriores são descritas separadamente abaixo, sem serem apagadas ou contadas como aprovação.


## Matriz caso → resultado → evidência

| Caso | Variantes executadas | Aprovadas | Evidência |
|---|---:|---:|---|
| UT01 | 3 | 3 | [unitarios.xml](../resultados/execucao-29/unitarios.xml) |
| UT02 | 4 | 4 | [unitarios.xml](../resultados/execucao-29/unitarios.xml) |
| UT03 | 15 | 15 | [unitarios.xml](../resultados/execucao-29/unitarios.xml) |
| UT04 | 1 | 1 | [unitarios.xml](../resultados/execucao-29/unitarios.xml) |
| IT01 | 1 | 1 | [integracao.xml](../resultados/execucao-29/integracao.xml) |
| IT02 | 1 | 1 | [integracao.xml](../resultados/execucao-29/integracao.xml) |
| ST01 | 1 | 1 | [sistema.xml](../resultados/execucao-29/sistema.xml) |
| ST02 | 2 | 2 | [sistema.xml](../resultados/execucao-29/sistema.xml) |
| ST03 | 4 | 4 | [sistema.xml](../resultados/execucao-29/sistema.xml) |
| ST04 | 4 | 4 | [sistema.xml](../resultados/execucao-29/sistema.xml) |
| ST05 | 3 | 3 | [sistema.xml](../resultados/execucao-29/sistema.xml) |
| ST06 | 15 | 15 | [sistema.xml](../resultados/execucao-29/sistema.xml) |

A [matriz detalhada em CSV](../resultados/execucao-29/matriz-resultados.csv) identifica cada execução concreta, esperado, obtido, situação, tempo e evidência. Os resultados por variante foram extraídos dos JUnit e relacionados às asserções e aos oráculos do plano. As 12 famílias não são contadas como se fossem apenas 12 execuções.

## Histórico e investigação das reexecuções

1. **ST01, execução inicial:** o nome acessível de Add contém o ícone, portanto a busca exata por `Add` não encontrou o botão. Não houve falha comprovada do produto. O seletor foi ajustado para o final do nome; a confirmação de exclusão recebeu o mesmo ajuste. [Log](../resultados/execucao-29/historico/sistema-inicial.txt) e [captura](../resultados/execucao-29/historico/sistema-inicial.png).
2. **Interrupção e retomada:** uma execução foi interrompida depois de gravar o contato, antes da limpeza. A retomada recusou começar por encontrar um contato no funcionário dedicado, preservando os dados. O registro `T2-Suzan-7caecaf5`, criado pela própria execução, foi removido pela interface. [Log da proteção](../resultados/execucao-29/historico/sistema-retomada.txt). Essa falha de pré-condição não conta como resultado funcional negativo do OrangeHRM.
3. **Preparação de integração:** foi necessário gerar os proxies Doctrine previstos pelo projeto antes de concluir as fixtures oficiais. Uma tentativa dentro do sandbox não conseguiu abrir o socket do banco e o bootstrap oficial terminou antes do PHPUnit, inclusive com código zero; como não houve testes nem JUnit, não foi declarada aprovação. A execução foi retomada com acesso ao banco local autorizado.
4. **IT02, preparação da massa:** a primeira execução efetiva teve IT01 aprovado e IT02 com erro de chave estrangeira. A massa pressupunha um número de funcionário calculado antes de o banco atribuir `AUTO_INCREMENT`; rollback não reinicia esse contador. O teste passou a usar o identificador real após flush. Não foi alterada a regra de negócio ou o esperado de atualização. [Saída anterior](../resultados/execucao-29/historico/integracao-automacao.txt) e [JUnit anterior](../resultados/execucao-29/historico/integracao-automacao.xml).
5. **Perda de ambiente na demo:** após 13 casos de sistema aprovados, a sessão expirou e o funcionário sintético deixou de aparecer na busca. ST05 com sinais não chegou a uma conclusão funcional e ST06 não encontrou sua pré-condição. A execução foi interrompida; o log parcial e as capturas foram preservados. A causa exata da perda de dados da demo não é controlada por esta contribuição; não foi atribuída ao produto como defeito da jornada. A massa foi recriada (novo empNumber 171), seguida de reexecução completa. [Log parcial](../resultados/execucao-29/historico/sistema-demo-reset.txt), [sessão expirada](../resultados/execucao-29/historico/demo-sessao-expirada.png) e [funcionário ausente](../resultados/execucao-29/historico/demo-funcionario-ausente.png).
6. **Reexecução:** ST01 passou isoladamente, incluindo limpeza; os dois testes de integração passaram juntos. A execução final completa foi registrada no commit corrigido. O timeout de 120 s inclui preparação e navegação na demo e não é uma espera fixa; Chrome já instalado foi selecionado por variável de ambiente.

A implementação inicial foi integrada no PR #71. As correções desta execução estão no commit `b806d71`, incluído na branch do relatório da #29 após aquele merge. O relatório não apaga as ocorrências anteriores nem as apresenta como defeitos do produto.

## Defeitos, caracterização e limites

Todos os 54 testes próprios passaram na execução final. Nenhum defeito do produto foi confirmado pelos casos executados; as falhas anteriores foram de automação, preparação ou perda de ambiente da demo. Isso não prova ausência geral de defeitos. Os cenários de sinais e limites foram avaliados conforme os oráculos do plano, sem mudar o esperado para produzir aprovação.

Na interface, ST04 verifica a rejeição de todos os telefones vazios e a aceitação de cada telefone isolado. A hipótese estática sobre essa exigência em toda a API não é conclusão desta execução, pois ficou fora do plano revisado. A aceitação de `+++++++++` por UT02/ST05 é caracterização de comportamento previsto pelo código e observado na exploração; não foi criada uma regra de obrigatoriedade de dígitos para classificá-la como defeito.

A integração demonstra gravação e leitura no banco com contexto ORM limpo dentro da transação, não teste de HTTP nem durabilidade após reinício do servidor. Os testes pela interface demonstram o comportamento da instância acessada na data. Os resultados não provam ausência geral de defeitos em outros fluxos.

Os resultados originais de 16 testes/56 asserções pertencem à [linha de base da #26](execucao-testes-originais.md) e não entram nos totais dos testes próprios. O [relatório unificado](../RELATORIO.md) reúne introdução, objetivos, casos e resultados conforme o enunciado.

## Uso de IA

Execução automatizada, diagnóstico dos problemas de preparação/seletores e redação assistidos por ChatGPT/Codex (OpenAI), a pedido de Suzan Stockey Pereira. A exploração manual anterior e a revisão do escopo são da autora. Não foram inventados resultados, alterados oráculos para esconder falhas ou atribuídos resultados de terceiros a esta execução.
