# Relatório unificado — testes do Buzz no OrangeHRM

Arthur Brito. Verificação e Validação. Execução em 17/11/2026.

Programação e redação assistidas pelo Cursor, modelo Grok 4.7 (xAI). A ferramenta leu o código, redigiu este relatório e os documentos ligados, implementou os testes e executou as suítes. O autor da entrega revisa o texto e os oráculos.

## Introdução

O OrangeHRM é um sistema de informação para gestão de pessoas: cadastro de empregados, licenças, ponto, recrutamento e o mural Buzz, entre outros módulos. O produto está descrito em <https://www.orangehrm.com>. O código-fonte aberto está em <https://github.com/orangehrm/orangehrm>. A versão sob teste é a OrangeHRM OS 5.9, commit [`56e23b3b09e7af29317a0943523b825843fff527`](https://github.com/orangehrm/orangehrm/commit/56e23b3b09e7af29317a0943523b825843fff527). O servidor usado pela turma é <https://opensource-demo.orangehrmlive.com/>. Em 17/11/2026 o rodapé da interface identificava OrangeHRM OS 5.9. A conta pública é `Admin` / `admin123`, ligada ao empregado de empNumber 7. O nome exibido no feed é o cadastro desse empregado no PIM compartilhado e mudou durante o dia; a jornada não depende do nome.

O Buzz é o plugin `orangehrmBuzzPlugin`. Publicar, comentar e apagar passam pela API v2 (`/web/index.php/api/v2/buzz/...`) e pela tela `/web/index.php/buzz/viewBuzz`. O projeto original traz PHP 8, Symfony, Doctrine e uma interface Vue. O `phpunit.xml` da raiz tem uma suíte `Buzz` apontando para `src/plugins/orangehrmBuzzPlugin/test`.

Esta máquina não tem PHP nem Maven. Os testes unitários não compilam o OrangeHRM inteiro. O script `executar-unitario.ps1` baixa as classes de URL desse commit para `testes/unitario/sut/`, baixa o PHPUnit 9.6.22 se ele ainda não estiver na pasta e roda `php:8.3-cli` no Docker. O `TextHelperTrait` original exige o contêiner do Symfony; `TextHelperTraitStub.php` devolve um `TextHelperService` direto, só para essa ligação. Integração e sistema usam Node.js contra o demo: `npm run test:integracao`, `npm run test:defeitos` e `npm run test:sistema`, a partir da pasta `Arthur-brito`, depois de `npm install` e de `npx playwright install chromium`. O passo a passo está no `README.md`.

A leitura da suíte original e de material de terceiros está em `analise/01-analise-critica.md`. Em resumo: os YAML de comentário e de URL de vídeo separam fronteiras e classes de verdade; o YAML de post não chega no limite 65530; o caso oficial de comentário sem texto espera sucesso e congela o furo de `NotBlankStringType` com string vazia; `BuzzServiceTest` não tem oráculo de negócio; a exclusão que a tela usa está em `BuzzShareAPI`, enquanto `BuzzPostAPI::delete()` lança não implementado. No quadro de [Javier Meléndez](https://javimele.com/orangehrm.html), consultado em 17/11/2026, os casos HRM-14 e HRM-15 cobrem criar e curtir um post, os dois só no caminho positivo. Não copiei esses casos.

## Objetivos

O objetivo é verificar uma jornada, nos três níveis, com as técnicas da disciplina.

**História.** Como colaborador autenticado, quero publicar um texto no Buzz, comentar essa publicação e apagá-la, para avisar o time e retirar o aviso quando ele deixar de valer.

**Fluxo principal.** Autenticar, abrir Buzz Newsfeed, escrever na caixa “What's on your mind?” e acionar Post (estado: publicada, sem o comentário desta jornada), comentar (estado: publicada, com o comentário), apagar o comentário e, em seguida, apagar a publicação pelo menu Delete Post (estado: inexistente).

**Fluxos alternativos usados no projeto dos casos.** Texto vazio ou só com espaços permanece inexistente. O caractere `0` é publicação válida. 65530 caracteres são aceitos e 65531 são recusados. Tipo fora de text, photo e video é recusado. Vídeo exige link de YouTube, Vimeo ou Dailymotion; outro site não vira embed. Foto aceita de 1 a 5 imagens. Comentário vazio deve ser recusado. A descrição completa, com pré-condições, pós-condições e a máquina S0 / S1 / S2, está em `docs/01-jornada.md`.

## Casos de teste

O desenho completo, com entradas e oráculos, está em `docs/02-plano-de-testes.md`. Abaixo fica o que cada nível decide.

### Unitário — partição, limite e tabela de decisão

Arquivos: `testes/unitario/RegrasDeTextoTest.php` e `testes/unitario/VideoUrlTest.php`. As URLs são as classes do commit. O texto é uma especificação local que copia as condições de `BuzzPostAPI`, `BuzzCommentAPI`, `NotBlankStringType` e `IsEmptyTrait`, para o caso rodar sem o kernel.

- UT-T01 a UT-T10: nulo, vazio, um caractere, `0`, só whitespace, whitespace nas pontas, emoji, 65530, 65531 e 65530 caracteres `á` (contagem por caractere).
- UT-C01 a UT-C03: comentário `''` passa na regra do código e falha na regra da jornada; espaço e nulo falham nas duas.
- UT-D01 a UT-D15: tabela tipo × texto × link presente × quantidade de fotos. Áudio é recusado. Vídeo sem link é recusado. Foto com 0 ou 6 imagens é recusada. Foto com 1 ou 5 e texto válido é aceita.
- UT-V01 a UT-V14: formatos `youtu.be`, watch, shorts, embed, `/v/`, Vimeo, player, Dailymotion e `dai.ly` produzem o embed esperado. Vazio, example.com, `watch?v=`, id `###` e `example.com/?q=youtube` são inválidos. URL de outro site devolve embed nulo. Os inválidos UT-V12 e UT-V14 não estão no YAML oficial.

### Integração — a mesma decisão no endpoint, mais os estados

Arquivo: `testes/integracao/jornada-api.test.js`. Login pelo token publicado em `:token` na página `/auth/login` e cookie na API v2.

- IT-01: o feed autenticado responde 200.
- IT-02: S0→S1→S2→S1→S0, com o texto do comentário lido em `model=detailed`.
- IT-03 e IT-04: partição inválida (`''`, espaço, espaço e tab) e o valor `0`.
- IT-05: limite 65530 / 65531 na postagem, fronteira que o YAML de post não cobre.
- IT-06: audio, photo sem arquivo e vídeo com link. Vídeo sem link saiu desta suíte e foi para D3, porque o status real é 500.
- IT-07: validação de URL pelo parâmetro `url`.
- IT-08: comentário só com espaços recusado; emoji gravado.

Cada caso que cria share apaga o share ao final. O prefixo do texto é `AB-`.

### Sistema — caso de uso na tela

Arquivo: `testes/sistema/jornada-buzz.test.js`. O cartão do feed é o elemento com a classe `orangehrm-buzz`. O texto fica em `p.orangehrm-buzz-post-body-text`, fora do cabeçalho `.orangehrm-buzz-post`.

- ST-01: fluxo principal até o comentário visível e a exclusão da publicação.
- ST-02 e ST-03: Post vazio e Post só com espaços não mudam a quantidade de cards, depois que o feed já carregou.
- ST-04: Share Video com `https://example.com/nao-e-video` não cria card com a legenda usada.

### Fora do escopo

A suíte oficial de fixtures, o upload de foto na tela e ensaios de desempenho, segurança e acessibilidade. A fronteira de 0, 1, 5 e 6 fotos fica na tabela unitária e na suíte original.

## Resultado dos testes

| Suíte | Resultado | Saída |
| --- | --- | --- |
| Unitário | 46 passaram, 79 asserções | `resultados/unitario.txt` |
| Integração da jornada | 8 passaram | `resultados/integracao.txt` |
| Sistema | 4 passaram | `resultados/sistema.txt` |
| Defeitos, oráculo da jornada | 4 falharam, como previsto | `resultados/defeitos.txt` |

Os quatro caminhos felizes e as recusas de postagem vazia, de tamanho e de tipo passam. Três defeitos permanecem no demo em 17/11/2026:

1. **D1.** Criar ou editar comentário com texto vazio responde HTTP 200 e grava o comentário. A jornada pede 422. O YAML oficial do projeto espera esse sucesso.
2. **D2.** `GET /api/v2/buzz/validation/links?link=` com uma URL válida do YouTube responde HTTP 501 `Not Implemented`. O OpenAPI chama o parâmetro de `link`. O código lê `url`, e com `url` o IT-07 obtém `valid: true` e o embed.
3. **D3.** `POST /api/v2/buzz/posts` com `type: "video"` e sem `link` responde HTTP 500 `Unexpected Error Occurred`. A jornada pede 422, no mesmo formato das outras entradas inválidas.

Observação da tela, separada desses três: o clique com texto vazio, só com espaços ou com URL que não é vídeo não exibe mensagem. O card novo não aparece. A API já recusa o texto vazio com 422. Numa tentativa anterior do ST-01, o `POST` respondeu 200 e o texto demorou mais de 45 s para surgir na lista; a execução registrada em `resultados/sistema.txt` mostrou o texto sem recarregar. O caso espera 12 s e recarrega o Buzz uma vez se a lista não pintar.

O detalhe dos corpos HTTP e dos tempos está em `docs/03-relatorio-execucao.md`.

## Fontes

- OrangeHRM, commit `56e23b3b09e7af29317a0943523b825843fff527`, em particular `BuzzPostAPI`, `BuzzCommentAPI`, `BuzzShareAPI`, `BuzzVideoURLValidationAPI`, `NotBlankStringType`, `Required`, `IsEmptyTrait`, `StrLength` e os YAML de teste do plugin Buzz. <https://github.com/orangehrm/orangehrm>
- Servidor de demonstração. <https://opensource-demo.orangehrmlive.com/>
- Descrição do produto. <https://www.orangehrm.com>
- Javier Meléndez, quadro de casos HRM-14 e HRM-15. <https://javimele.com/orangehrm.html>
- PHPUnit 9.6.22. <https://phar.phpunit.de/phpunit-9.6.22.phar>
- Playwright 1.49.1 e o executor `node:test` do Node.js.
- Enunciado da disciplina, adaptado por Marco Mangan a partir de enunciado de Júlio Machado e revisado com ChatGPT. Essa revisão é do enunciado.
