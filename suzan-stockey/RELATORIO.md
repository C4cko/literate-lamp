# T2 — Relatório unificado: contatos de emergência no OrangeHRM

**Contribuição individual:** Suzan Stockey Pereira (@SuzanStockey). **Data:** 17/11/2026.
**Coordenação:** [jornada #25](https://github.com/masmangan/literate-lamp/issues/25), [análise #26](https://github.com/masmangan/literate-lamp/issues/26), [plano #27](https://github.com/masmangan/literate-lamp/issues/27), [implementação #28](https://github.com/masmangan/literate-lamp/issues/28) e [execução #29](https://github.com/masmangan/literate-lamp/issues/29).

## 1. Introdução

O OrangeHRM é um sistema de informação para gestão de recursos humanos. A contribuição testa o cadastro e a atualização de contatos de emergência de funcionários, no módulo PIM. A descrição do produto está em [orangehrm.com](https://www.orangehrm.com) e o código em [orangehrm/orangehrm](https://github.com/orangehrm/orangehrm).

A análise e os testes PHP usam a revisão `56e23b3b09e7af29317a0943523b825843fff527`. Os testes de sistema utilizam o [servidor indicado no enunciado](https://opensource-demo.orangehrmlive.com/web/index.php/dashboard/index), que exibe OrangeHRM OS 5.9. O commit implantado nessa demo não é publicado; portanto, a igualdade com a revisão do clone não é presumida.

### Preparação e execução

Não há compilação da aplicação PHP. As dependências são instaladas a partir de `src/composer.lock`; o PHPUnit carrega diretamente as classes originais. Para a integração, foram usados o instalador CLI, a geração de proxies Doctrine e a criação do banco de testes fornecidos pelo OrangeHRM. O banco temporário desta execução foi encerrado após os testes. Os testes de sistema usam a demo, sem compilar nem hospedar uma nova interface.

Os comandos completos, pré-condições, dependências e a matriz caso → código estão no [README dos testes](testes/README.md). Resumo, a partir da raiz do repositório, com `OHRM_ROOT` apontando para o clone preparado:

```powershell
$t2Tests = (Resolve-Path 'suzan-stockey/testes/php').Path
php "$env:OHRM_ROOT/src/vendor/bin/phpunit" --no-configuration --bootstrap "$t2Tests/bootstrap-unitario.php" --testdox "$t2Tests/unitarios"
php "$env:OHRM_ROOT/src/vendor/bin/phpunit" --no-configuration --bootstrap "$t2Tests/bootstrap-integracao.php" --testdox "$t2Tests/integracao"
cd suzan-stockey/testes/sistema
npm ci
npm test
```

Para sistema, configurar as variáveis da instância, sessão e funcionário sintético descritas no README. Pode-se usar Chrome já instalado com `OHRM_BROWSER_CHANNEL=chrome` ou Chromium do Playwright. Nenhuma credencial pessoal ou arquivo de infraestrutura compartilhada integra esta entrega.

## 2. Objetivo e jornada selecionada

**Persona:** responsável de RH que mantém um contato indicado pelo funcionário para situações urgentes.

**História:** como responsável de RH, quero cadastrar e atualizar um contato de emergência para manter disponíveis os dados necessários em situações urgentes.

**Pré-condições:** sessão autenticada e funcionário sintético acessível. Login e criação do funcionário são preparação, não jornadas adicionais.

**Fluxo principal:** abrir PIM → Employee List, selecionar o funcionário, acessar Emergency Contacts → Add, informar nome/parentesco e pelo menos um telefone, salvar e conferir após recarregar. Em seguida, editar o telefone do mesmo contato, salvar e confirmar a atualização após nova consulta.

**Alternativas:** usar residencial, celular ou comercial; cancelar criação ou edição. **Exceções:** obrigatório vazio ou só com espaços, ausência dos três telefones, caracteres proibidos e comprimento excedido.

O objetivo dos testes é verificar os valores e o vínculo gravados, atualização sem duplicação, preservação ao cancelar e mensagens das validações. Anexos, outras abas, autorização de outros usuários, desempenho e mudanças no produto estão fora do recorte. A exclusão é usada somente para limpar a massa própria.

## 3. Análise crítica dos testes existentes e de terceiros

Foram analisados três arquivos originais: seis métodos de serviço, nove de DAO e um de entidade. O serviço usa DAO mockado e comprova delegação, mas sua preparação depende do banco e os campos retornados vêm do próprio objeto programado no mock. Não comprova persistência. O DAO e o teste da entidade exercitam ORM/banco, sendo classificados pelo comportamento como integração, independentemente do nome da pasta.

Os testes originais verificam cadastro, consultas, exclusão, sequência e vínculo ao funcionário. As principais lacunas nos arquivos lidos são entradas inválidas, limites dos campos, atualização seguida de leitura em contexto limpo e cancelamento pela interface. Algumas consultas podem reutilizar objetos já presentes no contexto ORM. Os testes próprios complementam essas fronteiras: validadores reais em unidade, leitura após limpar o EntityManager na integração e recarga da página no sistema.

A avaliação de terceiros utilizou o projeto [ezethufokazi/OrangeHRM-Testing](https://github.com/ezethufokazi/OrangeHRM-Testing/tree/b34cb03920c4f3c1ecae4826472459b66b13730c), nas planilhas de casos e resultados, aba *My Info - Emergency Contacts*. A estrutura de passos e resultados ajuda a descrever a jornada, mas os quatro cenários avaliados não cobrem os limites e formatos desta contribuição. Seus resultados são declarações dos autores para outro contexto, não execução nossa nem comprovação da versão atual. Nenhum teste ou resultado foi copiado como produção própria.

A leitura UI/API mostrou diferenças entre regras de presença e regras de formato. O plano revisado não trata a exigência conjunta de telefone da interface como requisito confirmado de toda a API. `+++++++++` é uma caracterização da aceitação atual, não um defeito confirmado pela simples ausência de dígitos.

Detalhamento e fontes fixadas: [análise original](docs/analise-testes-originais.md), [validações e terceiros](docs/analise-validacoes-e-terceiros.md) e [exploração manual de Suzan](docs/exploracao-contatos-emergencia.md).

## 4. Projeto dos casos de teste

O [Plano de Testes](docs/plano-de-testes.md) define critérios CA01–CA07 e a rastreabilidade completa. A implementação segue o plano revisado e usa classes de equivalência, valores-limite, tabela de decisão e o cenário de cadastro/atualização.

| Casos | Técnica e verificação | Nível | Variantes |
|---|---|---|---:|
| UT01 | Obrigatórios vazio/espaços/texto; combinação de validadores originais | Unitário | 3 |
| UT02 | Número, letras, arroba e somente sinais no validador Phone original | Unitário | 4 |
| UT03 | Nome/parentesco 99/100/101 e telefones 29/30/31 na classe de comprimento original | Unitário | 15 |
| UT04 | Delegação do serviço com argumento e retorno corretos; DAO mockado | Unitário | 1 |
| IT01 | Cadastro, consulta em contexto ORM limpo, campos e vínculo | Integração | 1 |
| IT02 | Atualização com leitura em contexto limpo, mesmo identificador e sem duplicação | Integração | 1 |
| ST01 | Cadastro e atualização com conferência após recarregar | Sistema | 1 |
| ST02 | Cancelar criação e edição | Sistema | 2 |
| ST03 | Obrigatórios vazios e com espaços | Sistema | 4 |
| ST04 | Nenhum telefone e cada um dos três isoladamente | Sistema | 4 |
| ST05 | Letras, arroba e sinais no telefone | Sistema | 3 |
| ST06 | Limites de comprimento dos cinco campos na interface | Sistema | 15 |

São 12 grupos com 54 execuções parametrizadas; essa quantidade é uma escolha do plano, não um mínimo imposto pelo professor. Um mock não é apresentado como evidência de banco; os testes de integração não são apresentados como HTTP; a interface é exercitada por ações reais, sem respostas simuladas.

A integração cria um funcionário próprio em transação e desfaz os dados com rollback. A suíte de sistema usa um funcionário sintético dedicado, verifica a ausência de contatos antes de iniciar cada caso e remove o único contato gerado no final. Os dados não são pessoais reais.

## 5. Resultado dos testes

| Nível | Executados | Aprovados | Falhas/erros finais | Asserções PHP | Evidência |
|---|---:|---:|---:|---:|---|
| Unitário | 23 | 23 | 0 | 30 | [JUnit](resultados/execucao-29/unitarios.xml) e [saída](resultados/execucao-29/unitarios.txt) |
| Integração | 2 | 2 | 0 | 19 | [JUnit](resultados/execucao-29/integracao.xml) e [saída](resultados/execucao-29/integracao.txt) |
| Sistema | 29 | 29 | 0 | Não contabilizadas | [JUnit](resultados/execucao-29/sistema.xml) e [saída](resultados/execucao-29/sistema.txt) |
| **Total próprio** | **54** | **54** | **0** | **49 nos testes PHP** | [Matriz por variante](resultados/execucao-29/matriz-resultados.csv) |

Todos os casos previstos foram executados e aprovados na rodada final, sem casos ignorados. Os três comandos finais retornaram zero. As ocorrências anteriores são descritas no relatório detalhado da execução, sem serem apagadas ou contadas como aprovação.

Nenhum defeito do produto foi confirmado nesses casos. As ocorrências iniciais de seletores, preparação da massa e perda de ambiente foram investigadas e estão documentadas no histórico. Todos os casos do plano possuem resultado final e evidência. A aceitação de sinais sem dígitos foi caracterizada conforme a regra atual, sem ser apresentada automaticamente como defeito.

A linha de base original, executada anteriormente, teve 16 testes e 56 asserções aprovados; está documentada em [execucao-testes-originais.md](docs/execucao-testes-originais.md). Esses 16 não são somados às 54 execuções próprias nem usados para substituir seus resultados.

O [Relatório da Execução](docs/execucao-testes-proprios.md) apresenta ambiente, comandos, matriz por caso, evidências, correções da automação e limitações. Os arquivos textuais e JUnit estão em [resultados/execucao-29](resultados/execucao-29/).

## 6. Contribuição e organização da entrega

As atividades foram precedidas pelas Issues #25–#29, atribuídas a @SuzanStockey. Análise e plano foram integrados pelos PRs [#30](https://github.com/masmangan/literate-lamp/pull/30) e [#69](https://github.com/masmangan/literate-lamp/pull/69). A implementação inicial foi integrada no [PR #71](https://github.com/masmangan/literate-lamp/pull/71), com commits incrementais. As correções motivadas pela execução real estão no commit `b806d71`, incluído com os resultados da #29. Este relatório reúne as seções solicitadas pelo enunciado e aponta os documentos detalhados e as evidências da contribuição individual no repositório compartilhado.

## 7. Fontes e uso de IA

- Enunciado **Trabalho 2: Teste do Orange Human Resource Management**, fornecido no Moodle e reproduzido pela autora para conferência do escopo.
- [Código original OrangeHRM, revisão analisada](https://github.com/orangehrm/orangehrm/tree/56e23b3b09e7af29317a0943523b825843fff527), incluindo serviço/DAO, testes originais, validadores e componentes de contatos de emergência. Links por arquivo nos documentos de análise.
- [Material de terceiros, revisão fixada](https://github.com/ezethufokazi/OrangeHRM-Testing/tree/b34cb03920c4f3c1ecae4826472459b66b13730c), citado e discutido acima.
- [Descrição oficial do produto](https://www.orangehrm.com) e [servidor de testes do enunciado](https://opensource-demo.orangehrmlive.com/web/index.php/dashboard/index).

Suzan Stockey Pereira revisou o escopo e realizou a exploração manual registrada na #26. Programação, execução automatizada, investigação e redação tiveram assistência do ChatGPT/Codex (OpenAI), a pedido da autora. Os resultados relatados decorrem das execuções e arquivos vinculados; uso de IA e fontes são declarados para distinguir a contribuição própria do material consultado.
