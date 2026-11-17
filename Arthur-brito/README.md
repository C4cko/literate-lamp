# Arthur Brito — testes do Buzz no OrangeHRM OS 5.9

Contribuição individual do Trabalho 2 de Verificação e Validação. A jornada é publicar um texto no Buzz, comentar e apagar a publicação. O recorte não repete férias, ponto, PIM, recrutamento, reembolso, manutenção nem login, que são jornadas de outros participantes.

## O sistema

OrangeHRM é um sistema de gestão de pessoas. O código está em <https://github.com/orangehrm/orangehrm>. Esta pasta usa o commit `56e23b3b09e7af29317a0943523b825843fff527` (OrangeHRM OS 5.9), o mesmo pin da turma. O servidor de demonstração é <https://opensource-demo.orangehrmlive.com/>. A descrição do produto está em <https://www.orangehrm.com>.

A conta pública do demo é `Admin` / `admin123`. Não há senha própria neste repositório. Os textos criados por estes testes começam com `AB-` e são apagados ao final. Se algum ficar no feed, `node testes/integracao/limpar.mjs` remove os itens do feed cujo texto começa com esse prefixo.

## Como obter o código e rodar

O OrangeHRM completo não é compilado aqui. Não há PHP nem Maven nesta máquina. Os testes unitários baixam só as classes de URL do commit e rodam no Docker. Integração e sistema falam com o demo.

Pré-requisitos: Node.js 20 ou mais novo, Docker em execução para o unitário, e o Chromium do Playwright (`npx playwright install chromium` na primeira vez, a partir desta pasta).

```powershell
cd Arthur-brito
npm install
npm run test:unitario
npm run test:integracao
npm run test:defeitos
npm run test:sistema
```

- `test:unitario` chama `executar-unitario.ps1`. Se `php` não estiver no PATH, o script sobe `php:8.3-cli` e executa o PHPUnit 9.6.22 (`testes/unitario/phpunit.phar`, baixado na primeira execução e ignorado pelo git). As classes do sistema sob teste vão para `testes/unitario/sut/`, também ignorada. O `TextHelperTrait` original pede o contêiner do Symfony; o arquivo `TextHelperTraitStub.php` só substitui essa ligação.
- `test:integracao` e `test:defeitos` usam `node:test` contra a API v2, com a sessão do login web.
- `test:sistema` abre o Chromium do Playwright pela biblioteca, também sob `node:test`. No Windows o `browser.close()` pode não encerrar o processo; o teste encerra `headless_shell.exe`, que é o Chromium do Playwright, não o Chrome da pessoa.

`test:defeitos` termina com código 1. Essa suíte usa o oráculo da jornada, e o servidor atual não o cumpre. O relatório de execução registra essa falha.

Variáveis opcionais: `ORANGEHRM_BASE`, `ORANGEHRM_USER`, `ORANGEHRM_PASSWORD`.

## O que tem nesta pasta

| Caminho | Conteúdo |
| --- | --- |
| `analise/01-analise-critica.md` | Leitura da suíte original do Buzz e do material de terceiros |
| `docs/01-jornada.md` | História, fluxos e estados |
| `docs/02-plano-de-testes.md` | Casos UT, IT e ST |
| `docs/03-relatorio-execucao.md` | O que passou e os defeitos |
| `docs/00-relatorio-unificado.md` | Relatório com introdução, objetivos, casos e resultado |
| `testes/unitario/` | Regras locais e classes originais de URL |
| `testes/integracao/` | API do demo |
| `testes/sistema/` | Tela Buzz Newsfeed |
| `resultados/` | Saída das execuções de 17/11/2026 |

## Uso de IA

Programação e redação assistidas pelo Cursor, modelo Grok 4.7 (xAI). A ferramenta leu o código do commit, redigiu a análise, a jornada, o plano e os relatórios, implementou os testes e executou as suítes. Arthur Brito é o autor da entrega e revisa o texto e os oráculos. O enunciado da disciplina, na nota final, informa que foi adaptado por Marco Mangan a partir de enunciado de Júlio Machado e revisado com ChatGPT. Essa revisão é do enunciado, não deste relatório.

## Issues

Os relatórios de plano e de execução estão nos arquivos acima para serem registrados como Issues atribuídas a Arthur Brito. A tabela abaixo é preenchida quando as issues existirem no repositório.

| Issue | Assunto |
| --- | --- |
| [#55](https://github.com/masmangan/literate-lamp/issues/55) | Plano de testes — Buzz |
| [#56](https://github.com/masmangan/literate-lamp/issues/56) | Execução dos testes — Buzz |

O login `arthurbrt0` não tem permissão para se autoatribuir nestas issues. O título leva o nome do executor.
