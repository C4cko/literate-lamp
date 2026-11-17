# Análise dos testes existentes – módulo Buzz

Issue: #48

## 1. Testes no código original

Versão analisada: repositório `orangehrm/orangehrm`, commit `56e23b3` (OrangeHRM OS 5.9).
Os testes do Buzz ficam em `src/plugins/orangehrmBuzzPlugin/test/`.

| Pasta | O que testa | Nível | Qtd. de métodos |
|---|---|---|---|
| `Entity/` | getters e setters das entidades (BuzzPost, BuzzShare, BuzzComment...) | unitário | 7 classes |
| `Dao/` | consultas no banco (feed, curtidas, contadores, aniversários) | integração (PHPUnit + banco com fixtures YAML) | 4 classes |
| `Api/` | endpoints REST v2 (posts, shares, comentários, curtidas, feed) | integração | 9 classes |
| `Controller/` | download de fotos dos posts | integração | 2 classes |

No total são 90 métodos `test...`. Os testes de API usam *data providers* com os casos escritos em YAML
(`fixtures/testcases/*.yaml`). Cada caso informa o usuário logado, o corpo da requisição e o resultado esperado.

**Pontos positivos**

- Os casos de API cobrem entradas válidas e inválidas, e há casos de **valor-limite** no tamanho do texto
  (ex.: "create comment with large text (include multibyte characters, char count = 65,530 [65,614 bytes])").
  O limite de 65530 é contado em caracteres, não em bytes.
- Os oráculos são precisos: comparam o JSON inteiro da resposta, não só o status.
- Os testes são independentes: cada um carrega as próprias fixtures no banco de teste.

**Pontos fracos**

- Não existe nenhum teste de sistema (pela interface) no repositório. A tela do Buzz (Vue) não é testada.
- Os testes de entidade só testam get/set, ou seja, quase não encontram defeitos.
- A partição "texto vazio" do comentário não é testada. Existe o caso *create comment without text*
  (campo ausente), mas não o caso `text: ""`. Ao testar no servidor, essa partição mostrou um defeito (ver D1
  no relatório de execução).
- Para rodar é preciso montar o ambiente PHP + MySQL com as fixtures. Não dá para rodar contra o servidor de
  demonstração.

## 2. Material de terceiros

Procurei projetos públicos de automação do OrangeHRM. A maioria usa a demo pública e cobre Login e PIM:

- [code-pace/playwright-orangeHrm](https://github.com/code-pace/playwright-orangeHrm) – Playwright + TypeScript, UI e API.
- [mohamedatef2020/orangehrm-demo-project](https://awesome.ecosyste.ms/projects/github.com%2Fmohamedatef2020%2Forangehrm-demo-project) – Cypress + Cucumber.
- [tanzinatoma/orangeHRMSelenium](https://github.com/tanzinatoma/orangeHRMSelenium) – Selenium + TestNG (Java).
- [NakulLakhotia/Selenium-Webdriver-OrangeHRM-Website-Usecases](https://www.github.com/NakulLakhotia/Selenium-Webdriver-OrangeHRM-Website-Usecases) – Selenium.

Pela descrição desses projetos, o foco é Login, PIM e Admin. Não encontrei nenhum projeto de terceiros com
testes organizados do Buzz, por isso não tenho como avaliar casos de Buzz feitos por outras pessoas.
Não analisei o código de cada repositório a fundo.

Um cuidado que vale para qualquer teste na demo pública: vários usuários testam ao mesmo tempo, então um
teste que age sobre "o primeiro post do feed" pode mexer no post de outra pessoa. Isso aconteceu comigo na
exploração: um script que clicava no primeiro post acabou agindo no post de outro usuário. Por isso meus
testes procuram o post pelo texto único que eles mesmos criaram e apagam tudo no final.

## 3. O que levei para o meu plano

1. Testar a tela (que não tem teste nenhum) com gravações do Chrome Recorder.
2. Testar a API contra o servidor real, incluindo a partição de texto vazio que ficou de fora.
3. Usar os limites do código (65530) para o valor-limite.
4. Cada teste cria seus dados com um identificador único e apaga no final.
