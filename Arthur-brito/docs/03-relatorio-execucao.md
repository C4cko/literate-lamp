# Relatório da execução — Buzz

Data: 17/11/2026. Responsável: Arthur Brito. Servidor: <https://opensource-demo.orangehrmlive.com/>. Código lido: OrangeHRM OS 5.9, commit `56e23b3b09e7af29317a0943523b825843fff527`. As saídas brutas estão em `resultados/`.

## Unitário

PHPUnit 9.6.22, imagem `php:8.3-cli`. Comando equivalente ao de `executar-unitario.ps1`.

Resultado: **46 testes, 79 asserções, todos passaram** (00:00.031, 24,89 MB). Código de saída 0. Arquivo: `resultados/unitario.txt`.

As classes originais de URL (`BuzzEmbeddedURL` e os parsers de YouTube, Vimeo e Dailymotion) aceitam os formatos do YAML oficial e recusam URL vazia, id vazio, id `###`, outro site e um host que só contém a palavra “youtube”. A classe local `RegrasDeTexto` reproduz o limite 65530, o furo do comentário vazio e a tabela de decisão dos tipos text, photo, video e audio.

## Integração da jornada

`node --test testes/integracao/jornada-api.test.js`, sessão `Admin` no demo.

Resultado: **8 testes, 8 passaram**, cerca de 17,5 s. Código de saída 0. Arquivo: `resultados/integracao.txt`.

| ID | Tempo | Resultado |
| --- | --- | --- |
| IT-01 feed autenticado | 1,0 s | 200, `data` é lista |
| IT-02 publicar, comentar, apagar comentário, apagar post | 4,1 s | estados S0→S1→S2→S1→S0 |
| IT-03 texto vazio e só espaços | 1,2 s | 422, chave `text` |
| IT-04 texto `0` | 1,2 s | gravado e depois apagado |
| IT-05 65530 e 65531 | 3,2 s | 200 com comprimento 65530; 65531 devolve 422 |
| IT-06 tipos | 2,1 s | audio 422 em `type`; photo sem arquivo 422; vídeo com link do YouTube 200 |
| IT-07 validação com o parâmetro `url` | 1,1 s | watch válido com embed; `watch?v=` e example.com inválidos |
| IT-08 comentário | 2,0 s | só espaços 422; `👍` gravado |

O comentário criado no IT-02 só devolve o texto em `GET .../comments/{id}?model=detailed`. O modelo `default` omite o campo.

## Defeitos

`node --test testes/integracao/defeitos.test.js`. O oráculo é a jornada. **4 testes, 0 passaram**, cerca de 10,9 s. Código de saída 1, de propósito. Arquivo: `resultados/defeitos.txt`.

| ID | Oráculo da jornada | Observado em 17/11/2026 |
| --- | --- | --- |
| D1 criação | `POST` de comentário com `text: ""` responde 422 | HTTP 200. O corpo gravou o comentário (id 13 no share 29) |
| D1 edição | `PUT` com `text: ""` responde 422 | HTTP 200 (comentário 14, share 30) |
| D3 | `POST /api/v2/buzz/posts` com `type: "video"` e sem `link` responde 422 | HTTP 500 `{"error":{"status":"500","message":"Unexpected Error Occurred"}}` |
| D2 | `GET /api/v2/buzz/validation/links?link=` com `https://youtu.be/jNQXAC9IVRw` responde 200 e `valid: true`, como descreve o OpenAPI | HTTP 501 `{"error":{"status":"501","message":"Not Implemented"}}` |

A mesma URL do YouTube, consultada com o nome que o código lê (`url`), passa no IT-07. D2 é o contrato publicado, não a regra de embed.

Causa ligada ao código do commit, sem depurador no processo do demo: `BuzzCommentAPI::getValidationRuleForCreate()` não aplica `Rules::REQUIRED`, e `NotBlankStringType` não casa com a string vazia. O vídeo sem `link` não vira 422; a montagem do post dispara o 500. O OpenAPI de `BuzzVideoURLValidationAPI` nomeia `link`; a constante `PARAMETER_VIDEO_LINK` vale `url`.

Os shares dos casos D1 foram apagados no `finally` de cada teste.

## Sistema

`node --test --test-timeout=120000 testes/sistema/jornada-buzz.test.js`, Chromium do Playwright, tela `/buzz/viewBuzz`.

Resultado: **4 testes, 4 passaram**, cerca de 97 s. Código de saída 0. Arquivo: `resultados/sistema.txt`.

| ID | Tempo | O que a tela fez |
| --- | --- | --- |
| ST-01 | 14,0 s | Post devolveu 200, o texto apareceu, o comentário entrou no card, Delete Post removeu o parágrafo |
| ST-02 | 21,7 s | Post sem texto não aumentou a quantidade de cards `.orangehrm-buzz` |
| ST-03 | 24,8 s | Post só com espaços também não aumentou a quantidade |
| ST-04 | 21,6 s | Share Video com `https://example.com/nao-e-video` e uma legenda `AB-video-...` não criou card com essa legenda |

Nesta execução o texto do ST-01 apareceu sem recarregar a página. Numa execução anterior do mesmo dia, o `POST` já tinha respondido 200 e o texto continuou ausente por 45 s. O caso espera 12 s e, se o feed não pintar o item, recarrega o Buzz uma vez. A API e a lista da tela não andam sempre juntas no demo compartilhado.

A tela não mostra “Required” nem “Invalid video url” nesses cliques. O botão Post segue habilitado. O oráculo é a ausência do card. A API, para o texto vazio, responde 422 (IT-03). A interface esconde uma validação que o endpoint já faz.

## Limpeza

Depois da suíte de sistema, `limpar.mjs` apagou dois posts `AB-st-` que tinham ficado de tentativas anteriores (shares 18 e 17, HTTP 200). O ST-01 que passou já tinha apagado o próprio post pela tela.

## Leitura do resultado

A jornada feliz passa nos três níveis. As fronteiras de texto da postagem, o caractere `0`, os tipos fora do domínio e as URLs de vídeo com o parâmetro `url` passam na API. Três defeitos ficam de pé: comentário vazio aceito (D1), parâmetro documentado `link` respondendo 501 (D2) e vídeo sem link respondendo 500 (D3).
