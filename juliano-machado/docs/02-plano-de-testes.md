# Plano de testes – jornada no Buzz

Issue: #49

## 1. Jornada de usuário

**História:** Como funcionário, quero publicar uma novidade no Buzz para que os colegas vejam, comentem e curtam.

**Ator:** funcionário logado (nos testes, o usuário `Admin` da demo).

**Pré-condição:** usuário autenticado no OrangeHRM.

**Fluxo principal**

1. O funcionário entra no menu **Buzz**.
2. Escreve o texto em *What's on your mind?* e clica em **Post**.
3. O post aparece no topo do feed com "0 Likes" e "0 Comments".
4. Clica no ícone de comentário, escreve um comentário e tecla Enter.
5. O comentário aparece abaixo do post e o contador vai para "1 Comment".
6. Clica no coração (curtir) e o contador vai para "1 Like".
7. Abre o menu do post (⋯), escolhe **Delete Post** e confirma em **Yes, Delete**.
8. O post sai do feed.

**Fluxos alternativos**

- A1 (passo 2): texto vazio, nada é publicado.
- A2 (passo 4): comentário vazio, nada é enviado.
- A3 (passo 6): curtir de novo um post já curtido, que não pode contar duas vezes.
- A4 (passo 7): clicar em **No, Cancel**, e o post continua no feed.

**Regras (tiradas do código, `BuzzPostAPI` e `BuzzCommentAPI`)**

- R1: o post de texto é obrigatório e tem no máximo 65530 caracteres.
- R2: o tipo do post é `text`, `photo` ou `video`.
- R3: cada usuário curte um post uma vez só.
- R4: o comentário tem no máximo 65530 caracteres. A tela não deixa enviar comentário vazio, então considero
  que ele também é obrigatório.

## 2. Partições e valores-limite do texto (R1)

| Partição | Valores escolhidos | Esperado |
|---|---|---|
| ausente | `null` / campo não enviado | inválido |
| vazio | `""` (0 caracteres) | inválido |
| tamanho válido | 1, 65529, 65530 | válido |
| acima do máximo | 65531 | inválido |
| multibyte | 65530 "ç" ou emoji | válido (conta caractere, não byte) |

## 3. Casos de teste

Identificação dos casos: **TU** = teste unitário, **TI** = teste de integração, **TS** = teste de sistema,
seguido de um número sequencial. A mesma sigla aparece no código (comentário e `@DisplayName`), nas gravações e
no relatório de execução, para ligar cada caso planejado ao teste e ao resultado. Defeitos usam **D** (D1, D2...).

### Unitários (Java 21 + JUnit 5, sem servidor)

| ID | Classe testada | Caso | Técnica |
|---|---|---|---|
| TU-01 | `RegraTexto` | tamanhos 0, 1, 65529, 65530, 65531 | valor-limite |
| TU-02 | `RegraTexto` | texto nulo | particionamento |
| TU-03 | `RegraTexto` | caracteres multibyte contam como 1 | particionamento |
| TU-04 | `Feed` | acha o post pelo texto e lê curtidas/comentários | particionamento |
| TU-05 | `Feed` | texto inexistente, parcial ou post sem texto não é achado | particionamento |
| TU-06 | `TokenCsrf` | extrai o token da página de login / erro sem token | particionamento |

### Integração (Java 21 + JUnit 5 + API REST v2 da demo)

| ID | Caso | Esperado |
|---|---|---|
| TI-01 | criar post de texto | 200, aparece no feed com 0 curtidas e 0 comentários |
| TI-02 | post com texto vazio | 422, parâmetro inválido `text` |
| TI-03 | post com tipo `audio` | 422 |
| TI-04 | post com 65530 caracteres | 200 |
| TI-05 | post com 65531 caracteres | 422 e não aparece no feed |
| TI-06 | comentar | contador de comentários = 1 |
| TI-07 | curtir duas vezes | 1ª 200, 2ª 400 "Already liked", contador = 1 |
| TI-08 | excluir post | 200 e some do feed |
| TI-09 | comentário sem o campo `text` | 422 |
| TI-10 | comentário com `text: ""` | 422 (igual à tela) |

### Sistema (gravações do Chrome Recorder em JSON, reproduzidas com @puppeteer/replay)

| ID | Gravação | Fluxo |
|---|---|---|
| TS-01 | `ts01-jornada-completa.json` | fluxo principal inteiro: login, publicar, comentar, curtir, excluir |
| TS-02 | `ts02-comentario-vazio.json` | A2: Enter com comentário vazio, continua "0 Comments" |
| TS-03 | `ts03-cancelar-exclusao.json` | A4: cancelar a exclusão, o post continua |

O A3 é coberto pelo TI-07 e o A1 pelo TI-02.

## 4. Ambiente e critérios

- Servidor: <https://opensource-demo.orangehrmlive.com> (OrangeHRM OS 5.9), usuário `Admin` / `admin123`.
- O servidor é compartilhado: cada teste cria posts com o prefixo `JM-` e um número único, e apaga no final.
- Critério de aprovação: todos os casos passam. Um caso que falha por defeito do sistema é registrado como
  defeito, e o teste fica com a tag `defeito`, separado da execução normal.
