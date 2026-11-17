# Plano de testes — Buzz: publicar, comentar e apagar

Responsável: Arthur Brito. Sistema: OrangeHRM OS 5.9, commit `56e23b3`. Servidor: <https://opensource-demo.orangehrmlive.com/>. Jornada: [`01-jornada.md`](01-jornada.md).

## Objetivo

Verificar a jornada em três níveis, com técnicas diferentes em cada um:

| Nível | Onde roda | Técnica dominante | Ferramenta |
| --- | --- | --- | --- |
| Unitário | classes de URL originais e regras extraídas do código | partição de equivalência, valor limite, tabela de decisão | PHPUnit 9.6 |
| Integração | API v2 do servidor de demonstração, com sessão do login web | transição de estados e a mesma tabela, agora no endpoint | Node.js, `node:test` |
| Sistema | tela Buzz Newsfeed | caso de uso do fluxo principal e dos alternativos A1 e A5 | Playwright, chamado pelo `node:test` |

Critério de sucesso do caminho feliz: o texto publicado aparece no feed, o comentário fica associado a esse share, a exclusão do comentário e depois da publicação os remove, e uma consulta seguinte não os encontra.

Critério de sucesso dos alternativos de erro: a API responde 422 com a chave do campo inválido. Na tela, o clique inválido não cria card com aquele texto. A interface do demo não mostra Required nem “Invalid video url”; o oráculo de sistema é a contagem de cards `.orangehrm-buzz`, não uma mensagem.

## Ambiente e como executar

Não há PHP nem Maven nesta máquina. Os unitários rodam em `php:8.3-cli` (Docker) sobre as classes baixadas do commit, com `TextHelperTrait` substituído só na ligação ao contêiner. Integração e sistema usam Node 24. Detalhes de comando estão no [`README.md`](../README.md).

Credenciais: as públicas do demo, `Admin` / `admin123`. Não há segredo próprio no repositório.

Cada teste que cria dado apaga o share ao final. Textos começam com `AB-`.

## Casos unitários

Oráculo das regras de texto: `testes/unitario/RegrasDeTexto.php`, copiando as condições de `BuzzPostAPI`, `BuzzCommentAPI`, `NotBlankStringType` e `IsEmptyTrait`. Oráculo das URLs: a própria classe `BuzzEmbeddedURL` do commit.

### Partições e limites do texto da postagem

| ID | Classe ou limite | Entrada | Esperado |
| --- | --- | --- | --- |
| UT-T01 | nulo | `null` | recusa, motivo `text` |
| UT-T02 | vazio, abaixo do mínimo | `''` | recusa |
| UT-T03 | limite inferior válido | `'a'` | aceita |
| UT-T04 | string que seria vazia em `empty()` do PHP | `'0'` | aceita |
| UT-T05 | só whitespace | espaços; espaço e tab | recusa |
| UT-T06 | whitespace nas pontas, miolo visível | `'  reunião  '` | aceita |
| UT-T07 | emoji, um caractere | `'👍'` | aceita |
| UT-T08 | limite superior | 65530 `'a'` | aceita |
| UT-T09 | um acima do limite | 65531 `'a'` | recusa |
| UT-T10 | limite em caractere multibyte | 65530 `'á'` | aceita (conta caractere, não byte) |

### Comentário: código contra a jornada

| ID | Entrada | Regra do código | Regra da jornada |
| --- | --- | --- | --- |
| UT-C01 | `''` | aceita | recusa |
| UT-C02 | `' '` | recusa | recusa |
| UT-C03 | `null` | recusa | recusa |

UT-C01 é o desenho do defeito D1. A integração observa se o servidor segue o código.

### Tabela de decisão da criação

Colunas: tipo, texto, link presente, quantidade de fotos. A validade interna da URL não entra nesta tabela; ela está nos UT-V.

| ID | tipo | texto | link | fotos | Esperado |
| --- | --- | --- | --- | --- | --- |
| UT-D01 | text | válido | não | 0 | aceita |
| UT-D02 | text | vazio | não | 0 | recusa |
| UT-D03 | text | nulo | não | 0 | recusa |
| UT-D04 | text | só espaços | não | 0 | recusa |
| UT-D05 | text | 65530 | não | 0 | aceita |
| UT-D06 | text | 65531 | não | 0 | recusa |
| UT-D07 | text | `0` | não | 0 | aceita |
| UT-D08 | video | nulo | sim | 0 | aceita |
| UT-D09 | video | nulo | não | 0 | recusa |
| UT-D10 | video | 65531 | sim | 0 | recusa |
| UT-D11 | photo | nulo | não | 1 | aceita |
| UT-D12 | photo | válido | não | 5 | aceita |
| UT-D13 | photo | nulo | não | 0 | recusa |
| UT-D14 | photo | nulo | não | 6 | recusa |
| UT-D15 | audio | válido | não | 0 | recusa |

### Partições de URL (classe original)

| ID | Classe | Exemplo | Esperado |
| --- | --- | --- | --- |
| UT-V01 | youtu.be | `https://youtu.be/JGkxRVEzUz0?list=...` | embed `.../embed/JGkxRVEzUz0` |
| UT-V02 | watch com tempo | `youtube.com/watch?v=4dDP_1lGbYs&t=4s` | embed sem o `t` |
| UT-V03 | shorts | `youtube.com/shorts/Fsa34Mo439qI0` | embed do id |
| UT-V04 | embed já pronto | `youtube.com/embed/jNQXAC9IVRw` | a própria URL |
| UT-V05 | http legado `/v/` | `http://www.youtube.com/v/-wtIMTCHWuI?...` | embed https |
| UT-V06 | vimeo numérico | `vimeo.com/732689681` | `player.vimeo.com/video/732689681` |
| UT-V07 | vimeo já embedado | a URL do player | a própria URL |
| UT-V08 | dailymotion longo | `dailymotion.com/video/x8fi3pm?...` | embed `x8fi3pm` |
| UT-V09 | dai.ly | `https://dai.ly/x2rnn2s` | embed `x2rnn2s` |
| UT-V10 | vazia | `''` | inválida |
| UT-V11 | outro site | `https://example.com/video/123` | inválida, embed nulo |
| UT-V12 | youtube sem id | `youtube.com/watch?v=` | inválida |
| UT-V13 | id corrompido | `youtu.be/###/...`, vimeo e dai.ly análogos | inválida |
| UT-V14 | a palavra youtube num outro host | `example.com/?q=youtube` | inválida |

Os exemplos válidos são os mesmos formatos do YAML oficial, para o unitário falar a mesma língua da suíte de integração do projeto. Os inválidos UT-V12 e UT-V14 não estão nesse YAML.

## Casos de integração

Pré-condição comum: login web com o token publicado em `:token` na página `/auth/login`, depois cookie de sessão nas chamadas `/api/v2`.

| ID | Técnica | Passos e oráculo |
| --- | --- | --- |
| IT-01 | fumaça da sessão | `GET /api/v2/buzz/feed?limit=1` devolve 200 e `data` é lista |
| IT-02 | estados S0→S1→S2→S1→S0 | Posta texto `AB-jornada-...`. O feed mostra o share com `numOfComments = 0`. Comenta. O GET de comentários contém o texto. Apaga o comentário. O id some. Apaga o share. O feed não tem mais o texto |
| IT-03 | partição inválida | `''`, `' '` e espaço+tab em `POST /buzz/posts` devolvem 422 e `invalidParamKeys = ['text']` |
| IT-04 | valor de fronteira disfarçado de vazio | texto `'0'` é gravado e o feed devolve esse texto; o share é apagado |
| IT-05 | valor limite | 65530 caracteres: 200 e o feed devolve comprimento 65530. 65531: 422 em `text` |
| IT-06 | tabela de decisão no endpoint | `type=audio` → 422 em `type`. `photo` sem arquivo → 422. `video` com `https://youtu.be/jNQXAC9IVRw` → 200 e o share é apagado. `video` sem link fica em D3, fora desta suíte, porque o servidor responde 500 |
| IT-07 | partição de URL no endpoint de validação | `watch?v=jNQXAC9IVRw` → `valid true` e o embed esperado. `watch?v=` → `valid false`. `example.com` → `valid false` |
| IT-08 | partição do comentário | espaços → 422 em `text`. `'👍'` → 200. O share é apagado |

## Casos de sistema

Pré-condição: login pela tela e abertura de `/buzz/viewBuzz`.

| ID | Fluxo | Passos e oráculo |
| --- | --- | --- |
| ST-01 | principal | Preenche “What's on your mind?”, Post, e espera HTTP 200 de `POST /api/v2/buzz/posts`. O texto aparece num `p.orangehrm-buzz-post-body-text`. Se em 12 s a lista não pintar o item, o teste recarrega o Buzz uma vez: a API já gravou. Ícone de comentário, “Write your comment...”, Enter, o card mostra o comentário. O menu usado é o primeiro `.orangehrm-buzz-post-header-config` (o comentário tem outro menu de três pontos). Delete Post, Yes Delete. Nenhum parágrafo do feed continua com aquele texto |
| ST-02 | A1 vazio | Espera o feed carregar, conta os cards, aciona Post sem texto. A quantidade de `.orangehrm-buzz` não muda |
| ST-03 | A1 espaços | Preenche só espaços e aciona Post. A quantidade de cards não muda |
| ST-04 | A5 URL inválida | Share Video, cola `https://example.com/nao-e-video`, escreve uma legenda `AB-video-...` e aciona Post. Nenhum card contém a legenda |

## O que ficou de fora, de propósito

- Suíte oficial de fixtures não é reexecutada aqui. Ela exige o banco e o kernel do OrangeHRM; a análise critica esse material em vez de duplicá-lo.
- Anexo de foto na tela. A fronteira 0/1/5/6 está na tabela unitária e na suíte original; repetir upload no demo compartilhado não acrescenta oráculo novo.
- Desempenho, segurança e acessibilidade. O enunciado pede níveis e técnicas de projeto de casos, não um ensaio de carga.

## Defeitos previstos antes da execução

- **D1.** Comentário com `text: ''` é gravado na criação e aceito na edição. Oráculo da jornada: 422. Observado em 17/11/2026: HTTP 200 nos dois casos.
- **D2.** O OpenAPI de `BuzzVideoURLValidationAPI` nomeia o parâmetro `link`. O servidor lê a query `url`. Chamar `?link=` com uma URL válida do YouTube devolve HTTP 501 `Not Implemented`, não 200.
- **D3.** `POST /api/v2/buzz/posts` com `type: video` e sem `link` devolve HTTP 500 `Unexpected Error Occurred`. Oráculo da jornada: 422, como as outras entradas inválidas da tabela.

Os testes desses dois oráculos estão em `testes/integracao/defeitos.test.js` e são executados à parte, porque o resultado esperado da disciplina é registrá-los como falha, não escondê-los no meio dos casos que passam.
