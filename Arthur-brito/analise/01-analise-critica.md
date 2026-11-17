# Análise crítica dos testes já existentes

Versão lida: OrangeHRM OS 5.9, commit [`56e23b3b09e7af29317a0943523b825843fff527`](https://github.com/orangehrm/orangehrm/commit/56e23b3b09e7af29317a0943523b825843fff527) do repositório [orangehrm/orangehrm](https://github.com/orangehrm/orangehrm). A interface do servidor de demonstração, em 17/11/2026, identifica a mesma linha: OrangeHRM OS 5.9.

O recorte é o plugin `orangehrmBuzzPlugin`, porque a jornada desta contribuição é publicar no Buzz, comentar e apagar. Os critérios são os trabalhados na disciplina: o caso de teste tem identificador, pré-condição, entrada, oráculo e independência; a escolha das entradas usa partição de equivalência, valor limite, tabela de decisão ou transição de estados; o nível (unitário, integração, sistema) corresponde ao que de fato é executado.

## 1. O que o código original automatiza

O `phpunit.xml` da raiz organiza uma suíte por plugin. A suíte `Buzz` aponta para `src/plugins/orangehrmBuzzPlugin/test`. Dentro dela há testes de API, controlador, DAO, entidade e serviço, além de fixtures YAML. Isso é organização boa: o nome da suíte localiza o módulo, e os YAML separam dados de código.

O tipo desses testes, porém, não é o que o nome da pasta sugere à primeira vista.

- `BuzzPostAPITest` e `BuzzCommentAPITest` estendem `EndpointIntegrationTestCase`. Sobem fixtures, um kernel com serviços simulados e um relógio falso, e comparam a resposta com o YAML. São testes de integração do endpoint com o DAO e o banco de teste, não testes unitários da regra isolada.
- `BuzzPostTest` persiste uma entidade com um funcionário da fixture e confere nome, tamanho do texto e datas. É um teste de integração com o mapeamento, e cobre um único caminho feliz.
- `BuzzServiceTest` só verifica se `getBuzzDao()` e `getBuzzLikeDao()` devolvem instâncias das classes esperadas. Não há comportamento.

Não há, no commit lido, teste de sistema da tela do Buzz nem suíte de interface. A jornada do usuário fica implícita nos YAML.

## 2. O que esses testes acertam

A suíte de comentário é o melhor trecho do plugin, e vale como referência de valor limite. Em `BuzzCommentAPITestCases.yaml` a criação é exercitada com 65530 caracteres (aceito) e 65531 (recusado), inclusive com caractere multibyte e com emoji de 4 bytes. O oráculo deixa de ser “texto grande” e passa a ser a fronteira anunciada por `BuzzPostAPI::PARAM_RULE_TEXT_MAX_LENGTH` (65530). Há também permissão: o ESS não apaga comentário alheio; identificador inexistente produz `RecordNotFoundException`.

A validação de vídeo (`BuzzVideoURLValidationAPIAPITestCases.yaml`) separa classes de URL que o código realmente trata de forma diferente: `youtu.be`, `youtube.com/watch`, `youtube.com/shorts`, `youtube.com/v`, `vimeo.com`, `player.vimeo.com`, `dailymotion.com/video`, `dai.ly`, URL vazia e URL com `###` no lugar do identificador. YouTube, Vimeo e Dailymotion são três classes válidas; o domínio estranho e o identificador corrompido são classes inválidas. O oráculo inclui o `embeddedURL`, não apenas o booleano `valid`.

A criação de post (`BuzzPostAPITestCases.yaml`) cobre os três tipos (`text`, `photo`, `video`), a ausência de texto no post de texto, vídeo sem legenda e mais de cinco fotos. A regra das cinco fotos está no corpo de `BuzzPostAPI::setBuzzPhotos()`, não só na declaração do `ParamRule`.

## 3. Falhas de qualidade

**O oráculo de comentário vazio está invertido em relação à regra de negócio.** `NotBlankStringType` recusa uma string que seja só whitespace, com o regex `/^\s+$/`. A string vazia não casa com esse regex, porque o `+` exige ao menos um caractere. `Required` (via `IsEmptyTrait`) recusa `null` e `''`, mas `BuzzCommentAPI::getValidationRuleForCreate()` aplica `STRING_TYPE` e `STR_LENGTH` e não aplica `REQUIRED`. O YAML oficial transforma isso em comportamento esperado: o caso `Update Comment without any text` envia `text: ''` e espera sucesso, com o comentário gravado. A suíte, aqui, congela o defeito. A postagem de texto não tem o mesmo furo, porque `getTextValidationRuleForTextPost()` acrescenta `REQUIRED`.

**A edição do comentário não usa a mesma regra de comprimento da criação.** A criação chama `Rules::STR_LENGTH`. A edição chama `Rules::LENGTH` (`BuzzCommentAPI::getValidationRuleForUpdate()`). As duas rejeitam 65531 nos YAML, então o efeito visível coincide nos casos gravados, mas a suíte não explica por que as classes diferem e não amarra as duas ao mesmo oráculo. Uma mudança no Respect Validation e uma mudança no `StrLength` do OrangeHRM podem divergir sem que o outro lado quebre.

**Valor limite da postagem de texto não aparece no YAML de post.** O texto “grande” de `Create Video Post With Text In Buzz - Large Text` é um parágrafo de lorem ipsum, longe de 65530. O limite está testado no comentário e não na publicação, embora a constante seja a mesma. Partição sem a fronteira deixa passar um off-by-one só num dos dois endpoints.

**`BuzzVideoURLValidationAPI` documenta um parâmetro e lê outro.** O bloco OpenAPI descreve o parâmetro de caminho `link`. A constante `PARAMETER_VIDEO_LINK` vale `'url'`, lida da query, e há um `TODO - length validation` no lugar do limite de tamanho. A suíte YAML usa `url` e passa; ela não percebe que o contrato publicado aponta para `link`. Também não há caso de URL muito longa, que é exatamente o TODO.

**Há código de teste que não testa.** `BuzzServiceTest` não tem oráculo de negócio. `BuzzPostAPITest::dataProviderForTestGetAll()` tem tipo de retorno `array`, não devolve array e chama `getValidationRuleForGetAll()` no corpo. Nenhum `@dataProvider` o referencia. `getAll()` e `delete()` do `BuzzPostAPI` lançam não implementado, e os testes só afirmam essa exceção. A listagem real está em `BuzzFeedAPI` e a exclusão real está em `BuzzShareAPI::delete()`. Quem lê a suíte do post conclui que “apagar não existe”; a interface apaga pelo share.

**Os testes de endpoint não são repetíveis fora do harness.** Dependem de `DataGroupPermission.yaml`, de funcionários numerados (Kayla Abbey, empNumber 1, e assim por diante) e de ids fixos de share. Isso é aceitável num banco de teste isolado. Não descreve a jornada de quem usa o servidor de demonstração, nem protege a tela Vue.

## 4. Material de terceiros

O material público encontrado trata o Buzz como um cenário de ponta, quando trata. No quadro de casos de [Javier Meléndez](https://javimele.com/orangehrm.html) (consultado em 17/11/2026) há dois itens:

- HRM-14, Buzz, “Create a post”, positivo, funcional, automatizável.
- HRM-15, Buzz, “Like a post”, positivo, funcional, automatizável.

O restante do mesmo quadro (login, PIM, vaga, manutenção) separa pelo menos um caso negativo por cenário. O Buzz não tem caso negativo, não tem comentário, não tem apagar, não tem limite de tamanho e não diz qual oráculo confirma que o post foi persistido e não apenas que o botão foi clicado. O rótulo “funcional” não distingue unitário, integração e sistema. Serve como mapa de telas. Não serve como projeto de teste da publicação.

Tutoriais de Selenium sobre o demo, no mesmo gênero, concentram login, PIM e Admin. Onde o Buzz aparece, o fluxo é “escrever uma frase e ver a frase”. Não há partição do texto, não há os três tipos de post e não há limpeza do dado criado num servidor compartilhado. O dado de um aluno permanece no feed dos outros.

Não copiei esses casos. A leitura serve para marcar o que eles não cobrem e que esta contribuição cobre: texto vazio, texto só com espaços, fronteira 65530/65531, tipo fora do domínio, URL de vídeo inválida, comentário e exclusão, nos três níveis.

## 5. Consequência para esta contribuição

A suíte original é forte na integração do endpoint de comentário e na tabela de formatos de vídeo, e é fraca como descrição da jornada e como guarda do comentário vazio. Os testes desta pasta não substituem essa suíte. Eles acrescentam:

- unitário das classes originais de URL, que hoje só são alcançadas por dentro do endpoint;
- unitário da tabela de decisão e dos limites de texto, com a regra do código e a regra da jornada lado a lado;
- integração contra o servidor de demonstração, no fluxo publicar → comentar → apagar;
- sistema pela tela Buzz Newsfeed.

O defeito D1 (comentário vazio aceito), o defeito D2 (contrato `link` contra a query `url`, resposta 501) e o defeito D3 (vídeo sem link responde 500) estão no relatório de execução. A tela do demo, no mesmo dia, não mostra Required quando o texto está vazio ou só tem espaços: o clique não cria card. A API, para o mesmo texto, responde 422. A interface esconde a validação que o endpoint já faz.
