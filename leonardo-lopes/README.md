# Testes do OrangeHRM — Leonardo Lopes

Contribuição individual para o Trabalho 2 de Verificação e Validação.

A jornada selecionada consiste em acessar a aba "Maintenance", autenticar com a senha administrativa, pesquisar por um empregado e acionar o download de seus dados em formato de arquivo.

## Casos implementados

Caso 1: Busca por um nome existente e faz o download corretamente

Caso 2: Busca por um nome inexistente e espera pela mensagem de erro.

## Ambiente utilizado

- Java 21
- Maven 3.9.10
- Firefox mais recente.
- Selenium e JUnit 5.
- OrangeHRM OS 5.9 no servidor público de demonstração.

Abaixo estão os dois relatórios formatados em padrão profissional (Markdown), prontos para você registrar nas **Issues** do seu repositório ou utilizar na documentação do seu relatório unificado do projeto.

---

# 1. Relatório do Plano de Testes

**[Maintenance] Download de dados do empregado:** Planejamento e Projeto de Casos de Teste — Módulo de Manutenção (OrangeHRM)

**Responsável:** Leonardo Lopes

**Objetivo:** Especificar e planejar os casos de teste de sistema (End-to-End) para a jornada de pesquisa de empregados e download de dados sensíveis/cadastrais no módulo *Maintenance* do sistema OrangeHRM.

---

## 1. Escopo da Jornada Selecionada

A jornada de usuário selecionada abrange o fluxo de auditoria e manutenção de dados de colaboradores. O processo exige autenticação padrão no sistema, navegação para a seção restrita de manutenção, redigitação de senha administrativa de segurança, consulta a um colaborador por meio de autocompletar e download dos registros.

## 2. Abordagem de Teste

 A automação foi projetada em **Java 17**, utilizando **JUnit 5** como framework de testes e **Selenium WebDriver** com esperas explícitas (`WebDriverWait`) para contornar instabilidades de carregamento assíncrono (*loaders*).

## 3. Casos de Teste Projetados

| ID do Caso | Descrição / Objetivo | Pré-condições | Passos da Execução | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CT-SIS-01** | **Pesquisa bem-sucedida e acionamento de download de dados do empregado.** | • Estar autenticado como Admin.<br>

<br>• Possuir acesso ao módulo *Maintenance*. | 1. Acessar `/maintenance/accessEmployeeData`.<br>

<br>2. Confirmar a senha administrativa (`admin123`).<br>

<br>3. Digitar um nome válido (`John William Doe`) no campo *Employee Name*.<br>

<br>4. Selecionar o item correspondente na lista suspensa (*autocomplete*).<br>

<br>5. Clicar em *Search*.<br>

<br>6. Clicar no botão *Download*. | O sistema realiza a busca com sucesso, exibe o registro correspondente e aciona o download dos dados cadastrais do colaborador. |

| **CT-SIS-02** | **Validação de erro ao pesquisar por nome de empregado inexistente.** | • Estar autenticado como Admin.<br> <br>• Possuir acesso ao módulo *Maintenance*. 

|1. Acessar `/maintenance/accessEmployeeData`.<br>

<br>2. Confirmar a senha administrativa (`admin123`).<br>

<br>3. Digitar um nome inválido/inexistente (`Usuario Inexistente XYZ 9999`).<br>

<br>4. Aguardar o retorno de "No Records Found" no componente.<br>

<br>5. Clicar em *Search*. | O sistema bloqueia a submissão e exibe a mensagem de validação de campo obrigatório/inválido (`Invalid`) abaixo do campo *Employee Name*. |

---

---

# 2. Relatório da Execução dos Testes

**[Maintenance] Download de dados do empregado:** Execução e Validação dos Testes Automatizados de Sistema

**Responsável:** Leonardo Lopes

**Data da Última Execução:** 17 de Novembro de 2026

**Ambiente de Execução:** Windows / Linux (CI via GitHub Actions), Java 17, Maven, Selenium WebDriver, Google Chrome Headless/GUI e o servidor público de homologação do OrangeHRM (`[https://opensource-demo.orangehrmlive.com](https://opensource-demo.orangehrmlive.com)`).

---

## 1. Resumo da Execução

A suíte de testes automatizados de sistema implementada na classe `DownloadTest` foi executada com sucesso através do Maven (`mvn test`). Ambos os cenários planejados obtiveram êxito.

## 2. Resultados Detalhados por Caso de Teste

| ID do Caso | Nível | Status | Observações / Comportamento Observado |
| --- | --- | --- | --- |
| **CT-SIS-01** | Sistema (E2E) | **PASSOU** | O Selenium realizou o login, superou a tela de dupla confirmação de senha do módulo de manutenção, interagiu com o componente de *autocomplete* filtrando o texto sem clicar no elemento de carregamento (*Searching*) e acionou o botão de download com sucesso. |
| **CT-SIS-02** | Sistema (E2E) | **PASSOU** | O teste inseriu um nome inexistente, validou a exibição correta do texto *"No Records Found"* pelo componente e confirmou que a interface exibiu o texto de erro esperado (`Invalid`) ao tentar pesquisar sem um ID de funcionário vinculado. |

## 3. Evidências de Execução

* **Logs do Maven / Surefire:** A execução foi finalizada sem exceções de *StaleElement* ou *ElementClickIntercepted*, com o término do ciclo atestado pela mensagem de `BUILD SUCCESS` do Maven.
* **Automação Contínua (CI):** O pipeline configurado via GitHub Actions assegura que os testes rodam de forma automatizada a cada atualização no repositório, garantindo a rastreabilidade e a saúde contínua da entrega.

**Uso de Inteligência Artificial:** Correção de texto para formulação do presente relatório; Revisão e correção de código para implementação de DownloadTest.java.
