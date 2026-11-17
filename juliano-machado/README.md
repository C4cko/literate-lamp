# Juliano Machado – Trabalho 2: Teste do OrangeHRM (Buzz)

Jornada escolhida: **funcionário publica uma novidade no Buzz, comenta, curte e depois exclui o post**,
incluindo descurtir, curtir comentários e a aba Most Liked Posts.

| Issue | Assunto | Documento |
|---|---|---|
| #48 | Análise dos testes existentes e de terceiros | [docs/01-analise.md](docs/01-analise.md) |
| #49 | Plano de testes (jornada e casos) | [docs/02-plano-de-testes.md](docs/02-plano-de-testes.md) |
| #50 | Implementação dos testes | `testes/` e `sistema/` |
| #51 | Relatório da execução | [docs/03-relatorio-execucao.md](docs/03-relatorio-execucao.md) |
| #58 | Ampliação: curtidas (descurtir, curtir comentário, Most Liked) | plano, testes e relatório |

## Introdução

O OrangeHRM é um sistema de RH de código aberto (PHP/Symfony no servidor e Vue no front-end). O Buzz é a rede
social interna: os funcionários publicam posts e os colegas comentam e curtem.

- Site: <https://www.orangehrm.com>
- Código-fonte: <https://github.com/orangehrm/orangehrm> (analisado no commit `56e23b3`, versão 5.9)
- Servidor usado nos testes: <https://opensource-demo.orangehrmlive.com> (usuário `Admin`, senha `admin123`)

Todos os testes rodam contra o servidor de demonstração, então não é preciso instalar o OrangeHRM.

## Estrutura

| Pasta | Conteúdo |
|---|---|
| `docs/` | análise, plano e relatório de execução |
| `testes/` | projeto Maven (Java 21, JUnit 5): testes unitários e de integração |
| `sistema/` | testes de sistema: gravações do Chrome Recorder (`gravacoes/*.json`) e o script que as reproduz |
| `resultados/` | saída das execuções |

## Como rodar

Siglas dos casos: **TU** = teste unitário, **TI** = teste de integração, **TS** = teste de sistema.

Pré-requisitos: Java 21 ou mais novo, Maven, Node 20 ou mais novo, Google Chrome e internet (os testes TI e TS
usam o servidor de demonstração). Nenhuma extensão de navegador é necessária.

### 1. Testes unitários (TU-01 a TU-07) e de integração (TI-01 a TI-09, TI-11 a TI-13)

Pelo terminal:

```
cd testes
mvn test
```

Resultado esperado: `Tests run: 24, Failures: 0` e `BUILD SUCCESS`.

Pelo IntelliJ IDEA:

1. *File → Open* e escolher a pasta `testes` (a que tem o `pom.xml`). Abrir como projeto Maven.
2. Se pedir, em *File → Project Structure → Project* escolher um JDK 21 ou mais novo.
3. Abrir a aba **Maven** (lado direito) → `buzz-testes` → *Lifecycle* → dois cliques em **test**.
   Isso roda igual ao terminal.
4. Para rodar uma classe ou um caso: abrir o arquivo em `src/test/java/buzz/` e clicar no ▶ ao lado da
   classe ou do método. A árvore de resultados mostra a sigla de cada caso (ex.: "TI-07 curtir...").

Observação: pelo ▶ do IntelliJ o TI-10 também roda e fica vermelho (é o teste do defeito, ver abaixo). Para
excluí-lo: *Edit Configurations → Test kind: Tags →* `!defeito`.

| Classe | Casos |
|---|---|
| `RegraTextoTest` | TU-01, TU-02, TU-03 |
| `FeedTest` | TU-04, TU-05, TU-07 |
| `TokenCsrfTest` | TU-06 |
| `BuzzApiTest` | TI-01 a TI-13 |

### 2. Testes do defeito D1 (TI-10 e TS-05)

```
cd testes
mvn test -Dgroups=defeito -DexcluirTags=none
```

Esse teste **falha de propósito** enquanto o defeito existir: `expected: <422> but was: <200>`.

O mesmo defeito visto na tela (TS-05), também falha de propósito:

```
cd sistema
npm run defeito                          # ou: HEADLESS=false LENTO=200 npm run defeito
```

A captura `resultados/ts05-comentario-vazio-pela-api-falha.png` mostra o comentário em branco que aparece no post.

### 3. Testes de sistema (TS-01 a TS-04)

```
cd sistema
npm install                          # só na primeira vez
npm test                             # roda as 4 gravações sem abrir janela
HEADLESS=false LENTO=200 npm test    # abre o Chrome e mostra os cliques devagar
node executar.mjs ts01-jornada-completa.json   # roda uma gravação só
```

Resultado esperado: `4 de 4 gravações passaram`. As capturas de tela ficam em `resultados/ts0X-*.png`.
Enquanto o Chrome estiver aberto pelo teste, não clique na janela.

`LENTO` é a pausa em milissegundos entre as ações. O script usa o Chrome instalado no macOS; em outro sistema,
informe o caminho com a variável `CHROME`.

| Gravação | Caso |
|---|---|
| `gravacoes/ts01-jornada-completa.json` | TS-01 |
| `gravacoes/ts02-comentario-vazio.json` | TS-02 |
| `gravacoes/ts03-cancelar-exclusao.json` | TS-03 |
| `gravacoes/ts04-curtidas.json` | TS-04 |
| `gravacoes/defeito/ts05-comentario-vazio-pela-api.json` | TS-05 (defeito D1, roda com `npm run defeito`) |

### 4. Reproduzir uma gravação direto no Chrome (sem terminal)

1. Abrir <https://opensource-demo.orangehrmlive.com> **sem estar logado** (a gravação começa pelo login).
2. Abrir o DevTools (`Cmd + Option + I` no Mac, `F12` no Windows) → ⋮ → *More tools* → **Recorder**.
3. Clicar em **Import recording** e escolher um arquivo de `sistema/gravacoes/`.
4. Ao lado de **Replay**, escolher a velocidade (*Slow* para acompanhar) e clicar em **Replay**.

Pelo Recorder o texto do post aparece como `JM-TS01-{{ID}}`, porque quem troca o `{{ID}}` por um número
único é o `executar.mjs`. A própria gravação exclui o post no final.

### 5. Gravação original e ajustes

A jornada foi gravada no Chrome Recorder e exportada em JSON:
[`sistema/gravacoes/original/ts-gravacao.json`](sistema/gravacoes/original/ts-gravacao.json). Ela não é executada
pelo script, porque não serve como teste do jeito que sai do Recorder:

| Na gravação original | Problema | Nas gravações `ts0X` |
|---|---|---|
| comentário, curtida e menu pelo seletor `div.oxd-grid-1 > div:nth-of-type(1)` (o primeiro post do feed) | na demo compartilhada o primeiro post pode ser de outra pessoa | XPath que acha o card pelo texto do post |
| texto fixo ("Post") | não dá para saber qual post é desta execução | texto `JM-TS0X-{{ID}}`, com número único |
| nenhum `waitForElement` | repete os cliques, mas não verifica nada | verificações de "1 Comment", "1 Like", "0 Likes", post removido etc. |
| não registrou a ida ao menu Buzz | depende da página em que o login termina | clique no link do Buzz |

Por isso as gravações `ts01` a `ts05` foram ajustadas à mão a partir desse formato.

## Resultado

28 execuções nos três níveis, todas passando. Encontrei 1 defeito (**D1**: a API aceita comentário vazio),
confirmado na API pelo TI-10 e na tela pelo TS-05, que ficam separados da execução normal. Detalhes no
[relatório de execução](docs/03-relatorio-execucao.md).
