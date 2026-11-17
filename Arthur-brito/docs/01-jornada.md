# Jornada: publicar no Buzz, comentar e apagar

## História

Como colaborador autenticado no OrangeHRM, quero publicar um texto no Buzz, comentar essa publicação e depois apagá-la, para avisar o time e retirar o aviso quando ele deixar de valer.

Ator principal: colaborador com sessão válida e acesso ao Buzz. No servidor de demonstração a conta pública `Admin` / `admin123` está ligada ao empregado de empNumber 7. O nome exibido no feed é o cadastro desse empregado e muda quando alguém edita o PIM compartilhado; a jornada não depende do nome. O fluxo também não depende de ser administrador: a API aceita o mesmo corpo para o ESS, como já mostram os YAML oficiais. A conta do demo é a que a turma pode repetir.

## Pré-condições

- OrangeHRM OS 5.9 no ar.
- Usuário e senha válidos.
- O módulo Buzz habilitado. No demo, o menu lateral tem o item Buzz e a rota `/web/index.php/buzz/viewBuzz`.

## Fluxo principal

1. O colaborador autentica-se.
2. Abre Buzz Newsfeed.
3. Escreve um texto na caixa “What's on your mind?” e aciona Post. O sistema grava um post do tipo `text` e um share do tipo post, e o texto aparece no topo do feed. Estado da publicação: **publicada, sem o comentário desta jornada**.
4. Aciona o ícone de comentário, escreve um comentário e confirma. O texto do comentário passa a fazer parte da publicação. Estado: **publicada, com comentário**.
5. Remove o comentário. Estado: **publicada, sem aquele comentário**.
6. Abre o menu da publicação (três pontos) e escolhe Delete Post, confirmando. Estado: **inexistente**. O feed deixa de mostrar o texto.

## Fluxos alternativos

- **A1. Texto vazio ou só com espaços no passo 3.** A API recusa a publicação (`422`, chave `text`). Na tela, o botão Post continua habilitado e não aparece a mensagem Required: o clique é ignorado e nenhum card novo entra no feed. O estado continua inexistente. O oráculo da interface é a ausência do card, não um texto de erro.
- **A2. Texto com um caractere, inclusive o caractere `0`.** A publicação é aceita. `0` não é vazio.
- **A3. Texto na fronteira.** 65530 caracteres são aceitos. 65531 são recusados. A contagem é em caracteres, a mesma de `StrLength` / `mb_strlen`.
- **A4. Tipo que não é text, photo nem video.** A API recusa a chave `type`.
- **A5. Vídeo.** O colaborador escolhe Share Video e informa uma URL. YouTube, Vimeo e Dailymotion válidos produzem um embed. URL de outro site, URL vazia e identificador vazio não produzem embed e não criam publicação. Sem link, a jornada exige recusa com 422. A API atual responde 500 (`Unexpected Error Occurred`). É o defeito D3.
- **A6. Foto.** De 1 a 5 imagens, a publicação é aceita. Zero ou seis, não. Este fluxo fica na tabela de decisão e na suíte original; a tela desta contribuição não anexa arquivo.
- **A7. Comentário vazio.** A jornada exige recusa. A API atual grava o comentário. É o defeito D1. Comentário só com espaços é recusado.
- **A8. Comentário apagado ou publicação apagada.** A confirmação de exclusão devolve o share ou o comentário removido, e uma consulta seguinte não os encontra.

## Pós-condições

No caminho feliz, não resta post nem comentário com o texto usado no teste. Cada teste de integração e de sistema apaga o que criou. O prefixo `AB-` identifica o dado desta contribuição no feed compartilhado.

## Máquina de estados

Estados visíveis no feed:

| Estado | Significado |
| --- | --- |
| S0 | Não existe publicação com o texto |
| S1 | Publicação existe, sem o comentário da jornada |
| S2 | Publicação existe e exibe o comentário |

Transições do caminho feliz: S0 → S1 (post válido), S1 → S2 (comentário válido), S2 → S1 (apaga o comentário), S1 → S0 (apaga a publicação). Texto inválido em S0 permanece em S0. O caso D1 fura a transição: comentário vazio também leva a um estado em que o comentário existe.

## Rastreio

A história acima é a origem dos casos UT (regras e URLs), IT (API, incluindo a sequência S0–S2–S1–S0) e ST (a mesma sequência pela tela). Os identificadores estão no plano de testes.
