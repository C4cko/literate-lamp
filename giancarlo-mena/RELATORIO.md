# Relatório – Teste da jornada "Recrutador contrata um candidato" no OrangeHRM

**Autor:** Giancarlo Mena · **Disciplina:** Verificação e Validação de Software · **Trabalho 2**

Este é o relatório unificado pedido no enunciado. Cada seção resume o conteúdo e aponta o documento com
os detalhes.

| Seção do enunciado | Documento completo | Issue |
|---|---|---|
| Análise dos testes existentes | [analise/01-analise-critica.md](analise/01-analise-critica.md) | #32 |
| Objetivos (jornada de usuário) | [docs/01-jornada.md](docs/01-jornada.md) | #33 |
| Casos de teste (plano) | [docs/02-plano-de-testes.md](docs/02-plano-de-testes.md) | #34 |
| Implementação dos testes | [testes/](testes/) | #35, #36, #37 |
| Resultado dos testes | [docs/03-relatorio-execucao.md](docs/03-relatorio-execucao.md) | #38 |

## 1. Introdução

**Sistema testado.** O OrangeHRM Starter é um sistema web de código aberto para gestão de recursos humanos,
com módulos de cadastro de pessoal (PIM), férias, ponto e recrutamento. Ele usa back-end em PHP 8
(Symfony e Doctrine), front-end em Vue 3 e uma API REST em `/web/index.php/api/v2`.

- Código-fonte: <https://github.com/orangehrm/orangehrm> (analisado no commit `56e23b3`).
- Servidor de testes: <https://opensource-demo.orangehrmlive.com> (usuário `Admin`, senha `admin123`).
- Descrição do sistema: <https://www.orangehrm.com>.

**Como compilar e rodar os testes.** Os testes estão em Java 21 com Maven, na pasta `testes/`.

Pré-requisitos: JDK 21 e Maven 3.9 ou mais novo. Os comandos abaixo valem para Windows, Linux e macOS:

```
cd testes
mvn test                 # unitários: não precisa de servidor
mvn test -Pintegracao    # API REST do servidor de demonstração
mvn test -Psistema       # interface com Playwright (baixa o Chromium na 1ª vez)
```

Detalhes, variáveis de ambiente e outro servidor: [testes/README.md](testes/README.md).

**Testes já existentes.** O código original tem testes PHPUnit para o módulo Recruitment, que na prática são
de integração (endpoint, serviço e banco). Não há testes de sistema (Cypress) para esse módulo. A análise
crítica encontrou estas lacunas:
- quase nenhuma transição proibida da máquina de estados é testada;
- a contratação não verifica se o funcionário foi criado;
- faltam valores-limite;
- há nomes de casos copiados e colados.

Também foram analisados dois materiais de terceiros.

## 2. Objetivos

O objetivo é verificar a jornada **"Recrutador contrata um candidato"**. Nela, a recrutadora (papel Admin)
cadastra um candidato em uma vaga e o conduz pelas etapas do processo seletivo até a contratação, quando o
candidato vira funcionário no PIM.

> **Como** recrutadora, **quero** cadastrar um candidato e registrar cada etapa do processo seletivo,
> **para que** a contratação fique documentada e o novo funcionário já apareça no cadastro de pessoal.

A jornada tem 7 critérios de aceitação e uma máquina de estados com 9 status e 15 transições válidas,
extraída do script de instalação do sistema:

*Application Initiated → Shortlisted → Interview Scheduled → Interview Passed → Job Offered → Hired*

Os caminhos alternativos são rejeição, entrevista reprovada, oferta recusada e segunda entrevista.

## 3. Casos de teste

| Nível | Casos | Técnicas | Ferramentas |
|---|---|---|---|
| Unitário | U01–U34 | transição de estados (15 válidas e 57 inválidas), partição de equivalência, valor-limite, tabela de decisão | JUnit 5 |
| Integração | I01–I12 | transição de estados (29 pares proibidos), valor-limite, partição, efeito colateral (funcionário no PIM) | JUnit 5 + `java.net.http.HttpClient` |
| Sistema | S01–S06 | caso de uso (jornada completa), tabela de decisão, partição, valor-limite | JUnit 5 + Playwright |
| Hipóteses de defeito | H1–H5 | derivadas da leitura do código | JUnit 5 (integração) |

Como o OrangeHRM é escrito em PHP, os testes unitários em Java testam um **modelo executável da
especificação** (máquina de estados e regras dos formulários). Esse modelo serve de **oráculo** para os
testes de integração e de sistema, que comparam o comportamento real com o que o modelo diz que deveria
acontecer.

## 4. Resultado dos testes

| Nível | Execuções | Passaram | Falharam |
|---|---|---|---|
| Unitário | 184 | 184 | 0 |
| Integração | 51 | 46 | 5 (defeitos do OrangeHRM) |
| Sistema (2ª execução) | 6 | 6 | 0 |

Na 1ª execução, os 6 testes de sistema falharam por um defeito no próprio código de teste. A busca de
rótulos usava uma sintaxe de expressão regular que só existe no Java, e o Playwright roda a expressão no
navegador. O defeito foi corrigido e os testes rodaram de novo. Todas as saídas estão em
[resultados/](resultados/).

**Defeitos encontrados no OrangeHRM:**

| Id | Defeito | Gravidade |
|---|---|---|
| D1 | É possível aprovar a entrevista de **outro candidato** (`PUT .../candidates/92/interviews/13/pass` com a entrevista do candidato 93 responde 200) | Alta |
| D2 | Cadastrar candidato com e-mail vazio gera **erro 500** em vez de 422 | Média |
| D3 | Primeiro nome vazio (`""`) é aceito | Média |
| D4 | Telefone com 31 caracteres é aceito (a tela limita a 25; o banco, a 30) | Baixa |
| D5 | E-mail com 101 caracteres é aceito (a tela limita a 50; o banco, a 100) | Baixa |

A jornada principal funciona como especificado. Os defeitos estão na validação da API, que é mais fraca que
a da tela.

## 5. Fontes

- OrangeHRM, código-fonte: <https://github.com/orangehrm/orangehrm>
- Casos de teste de Recruitment (marinaincodeland): <https://marinaincodeland.github.io/OrangeHRM_TestCases/html/recruitment-functionality.html>
- OrangeHRM_UIAutomationFrameWork (shishurajpandey): <https://github.com/shishurajpandey/OrangeHRM_UIAutomationFrameWork>
- O trabalho do colega Mateus Neubarth (pasta `mateus-neubarth/`) também autentica na API usando o token
  CSRF da página de login. O cliente deste trabalho (`TokenCsrf`, `OrangeHrmApi`) foi escrito a partir do
  código do OrangeHRM (`LoginController`, `Prop::getValue`, template `vue.html.twig`), com outra
  implementação (`java.net.http.HttpClient` em vez de REST Assured). Registro a semelhança da abordagem.

## 6. Uso de IA

Este trabalho usou o **Claude** (Anthropic, modelo Claude Opus 5.5) como assistente de programação e de
redação: análise do código do OrangeHRM, escrita dos testes e dos documentos. Os testes foram executados
pelo autor, e os resultados vêm das saídas reais do Maven. Todo o conteúdo foi revisado pelo autor.
