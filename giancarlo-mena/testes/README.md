# Projeto de testes (Java 21 + Maven)

## Pré-requisitos

- JDK 21 (`java -version` deve mostrar 21)
- Maven 3.9 ou mais novo (`mvn -version`)
- Acesso à internet (servidor de demonstração e download das dependências)

Na primeira execução dos testes de sistema, o Playwright baixa sozinho o navegador Chromium (cerca de 150 MB).

## Como compilar e rodar

Todos os comandos são executados dentro desta pasta (`testes/`).

| O que | Comando | Precisa do servidor? |
|---|---|---|
| Compilar | `mvn -q compile test-compile` | não |
| Testes unitários | `mvn test` | não |
| Testes de integração (API) | `mvn test -Pintegracao` | sim |
| Só as hipóteses de defeito | `mvn test -Pintegracao -Dgrupos.incluidos=hipotese` | sim |
| Testes de sistema (navegador) | `mvn test -Psistema` | sim |
| Sistema com o navegador visível | `mvn test -Psistema -Dnavegador.visivel=true` | sim |

Os relatórios do JUnit ficam em `target/surefire-reports/`. No Linux e no macOS, o script
`../executar-testes.sh` roda os três níveis e copia os relatórios para `../resultados/`. No Windows (cmd),
o equivalente é:

```
mkdir ..\resultados
mvn -B test > ..\resultados\unitario-saida.txt 2>&1
mvn -B test -Pintegracao > ..\resultados\integracao-saida.txt 2>&1
mvn -B test -Psistema > ..\resultados\sistema-saida.txt 2>&1
```

## Ambiente

Por padrão os testes usam o servidor de demonstração
`https://opensource-demo.orangehrmlive.com/web/index.php` com `Admin` / `admin123`.
Para outro servidor (por exemplo, uma instalação local do OrangeHRM em Docker):

```bash
# Linux/macOS (no Windows: set ORANGEHRM_URL=...)
export ORANGEHRM_URL=http://localhost:8200/web/index.php
export ORANGEHRM_USUARIO=Admin
export ORANGEHRM_SENHA=sua-senha
mvn test -Pintegracao
```

Também dá para passar como propriedade: `-Dorangehrm.url=...`, `-Dorangehrm.usuario=...`,
`-Dorangehrm.senha=...`.

## Estrutura

```
src/main/java/br/edu/pucrs/vv/recrutamento/
  modelo/     máquina de estados e regras dos formulários (oráculo dos testes)
  cliente/    cliente HTTP da API v2 (login com token CSRF + chamadas JSON)
src/test/java/br/edu/pucrs/vv/recrutamento/
  modelo/     testes unitários do modelo                (sem tag)
  cliente/    testes unitários do cliente               (sem tag)
  integracao/ testes da API REST                        (tag integracao; hipóteses com tag hipotese)
  sistema/    testes pela interface com Playwright      (tag sistema) + testes unitários FormatoDataTest e TextoExatoTest
```

Cada teste de integração e de sistema cria a própria vaga e candidatos com o sufixo `VV<data><hora><nnn>` e
apaga tudo no final. Se uma execução for interrompida no meio, podem sobrar vagas "Vaga VV..." ou
"Vaga UI..." no servidor; elas podem ser apagadas em Recruitment › Vacancies.
