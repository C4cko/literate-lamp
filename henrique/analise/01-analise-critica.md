
# Análise dos testes existentes — PIM Edit Contact Details

**Issue:** #72  
**Identificador:** PIM-EditContactDetails  
**Jornada:** PIM → Employee List → selecionar funcionário → editar dados de contato  
**Projeto analisado:** OrangeHRM

## 1. Objetivo

Analisar a implementação da funcionalidade de edição dos dados de contato de um funcionário no OrangeHRM, identificando as regras de validação existentes, os testes automatizados relacionados e possíveis lacunas de cobertura.

A análise foi realizada por inspeção estática do código-fonte e dos arquivos de teste disponíveis no repositório oficial do OrangeHRM.

## 2. Jornada selecionada

A jornada analisada corresponde à edição dos dados de contato de um funcionário:

1. Acessar o módulo PIM.
2. Abrir a opção Employee List.
3. Selecionar um funcionário.
4. Acessar a seção Contact Details.
5. Consultar ou alterar os dados de contato.
6. Salvar as alterações.

Os dados envolvidos incluem endereço, telefones e endereços de e-mail.

## 3. Arquivos analisados

| Arquivo | Finalidade |
|---|---|
| `EmployeeContactDetails.vue` | Implementação da interface de edição dos dados de contato. |
| `EmployeeContactDetailsAPI.php` | Implementação da API responsável pela consulta e atualização dos dados. |
| `EmployeeContactDetailsModel.php` | Modelo utilizado pela API. |
| `EmployeeContactDetailsAPITest.php` | Testes automatizados da API. |
| `EmployeeContactDetailsAPITestCases.yaml` | Casos de teste utilizados pelos data providers. |
| `EmployeeContactDetailsAPI.yml` | Dados de teste utilizados como fixtures. |
| `EmployeeService.php` | Definição de constantes e regras relacionadas aos funcionários. |

## 4. Análise da implementação

### 4.1 Interface

O arquivo `EmployeeContactDetails.vue` implementa o formulário de edição dos dados de contato.

Os campos identificados são:

- Street 1 e Street 2;
- City;
- Province;
- Zip Code;
- Country;
- Home Telephone;
- Mobile;
- Work Telephone;
- Work Email;
- Other Email.

A interface realiza uma requisição GET para carregar os dados existentes e uma requisição PUT para salvar as alterações.

Também existem validações no frontend para tamanho máximo de campos, formato de telefone e formato de e-mail.

### 4.2 API

A classe `EmployeeContactDetailsAPI.php` implementa as operações de consulta e atualização dos dados de contato.

Foram identificadas as seguintes regras:

- Verificação da existência e acessibilidade do funcionário;
- Validação dos tipos e tamanhos dos campos;
- Validação do formato dos telefones;
- Validação do código do país;
- Validação do formato dos e-mails;
- Verificação de unicidade dos endereços de e-mail;
- Verificação para impedir que Work Email e Other Email tenham o mesmo valor.

A API disponibiliza os métodos GET e PUT para essa funcionalidade.

As operações de criação, exclusão e listagem geral não estão implementadas nessa classe e retornam exceções de funcionalidade não implementada.

## 5. Testes automatizados existentes

O arquivo `EmployeeContactDetailsAPITest.php` contém testes de integração para a API.

Foram identificados os seguintes métodos:

- `testGetOne()`;
- `testUpdate()`;
- `testCreate()`;
- `testGetValidationRuleForCreate()`;
- `testGetAll()`;
- `testGetValidationRuleForGetAll()`;
- `testDelete()`;
- `testGetValidationRuleForDelete()`.

Os métodos `testGetOne()` e `testUpdate()` utilizam data providers alimentados pelo arquivo YAML de casos de teste.

### 5.1 Consulta de dados

O grupo `GetOne` contém dois cenários:

1. Consulta de um funcionário existente, com retorno dos dados de contato.
2. Consulta utilizando um identificador inválido de funcionário.

### 5.2 Atualização de dados

O grupo `Update` contém treze cenários declarados, contemplando situações como:

1. Atualização com corpo vazio;
2. Telefone residencial acima do limite permitido;
3. Telefones acima do limite permitido;
4. Formato de telefone inválido;
5. E-mail com formato inválido;
6. Utilização de Work Email já existente;
7. Utilização de Other Email já existente;
8. Utilização de e-mail profissional existente no campo de e-mail alternativo;
9. Utilização de e-mail alternativo existente no campo profissional;
10. Utilização de e-mail de outro funcionário;
11. E-mail acima do limite de caracteres;
12. Work Email e Other Email com valores iguais;
13. Alteração de um dos e-mails quando ambos já estavam cadastrados com o mesmo valor.

Portanto, foram identificados quinze cenários nos data providers: dois para consulta e treze para atualização.

### 5.3 Operações não implementadas

Também existem testes que verificam o comportamento das operações de criação, exclusão e listagem geral.

Esses testes esperam exceções de funcionalidade não implementada. Portanto, não representam testes funcionais de criação ou exclusão de dados de contato.

## 6. Comparação das validações entre frontend e backend

Durante a inspeção, foram identificadas diferenças nos limites de caracteres definidos na interface e na API.

| Campo | Frontend | Backend |
|---|---:|---:|
| Street 1 | 70 | 100 |
| Street 2 | 70 | 100 |
| City | 70 | 100 |
| Province | 70 | 100 |
| Zip Code | 10 | 20 |
| Telefones | 25 | 25 |
| Work Email | 50 | 50 |
| Other Email | 50 | 50 |

O limite do Work Email no backend utiliza a constante `EmployeeService::WORK_EMAIL_MAX_LENGTH`, definida com valor 50.

As diferenças nos campos de endereço e CEP indicam uma inconsistência entre as validações do frontend e do backend. Essa diferença deve ser investigada por meio de testes de fronteira e da confirmação dos requisitos funcionais.

Não é possível afirmar, somente pela inspeção do código, qual limite representa o comportamento esperado pelo negócio.


## 7. Estrutura e infraestrutura de testes do OrangeHRM

Esta seção complementa a análise dos testes da funcionalidade Edit Contact Details, considerando a organização da estrutura de testes do OrangeHRM e as orientações de configuração documentadas no próprio projeto.

### 7.1. Evidências identificadas no código-fonte

Durante a inspeção do código-fonte, foram identificadas as seguintes características da implementação dos testes:

**Organização dos testes**

Os testes relacionados à API de dados de contato estão localizados em:

`src/plugins/orangehrmPimPlugin/test/Api/`

A classe `EmployeeContactDetailsAPITest.php` estende `EndpointIntegrationTestCase`, indicando que os testes utilizam a infraestrutura de integração do OrangeHRM.

Diferentemente de um teste isolado de método, essa abordagem permite executar as requisições pelo ciclo completo do kernel HTTP, contemplando etapas como autenticação, autorização, tratamento de exceções e serialização das respostas.

**Utilização de fixtures**

A classe utiliza arquivos YAML para fornecer os dados necessários à execução dos testes. Foram identificados:

- `EmployeeContactDetailsAPI.yml`: dados utilizados como fixtures;
- `EmployeeContactDetailsAPITestCases.yaml`: cenários utilizados pelos data providers.

O carregamento dos dados é realizado por meio da infraestrutura de testes do OrangeHRM, que disponibiliza o serviço `TestDataService::populate()`.

Essa abordagem permite preparar os registros necessários para cada cenário, evitando depender de dados previamente existentes no ambiente.

**Cobertura dos testes existentes**

Na análise dos data providers, foram identificados:

- 2 cenários relacionados à consulta de dados (`GetOne`);
- 13 cenários relacionados à atualização dos dados (`Update`).

Portanto, foram identificados 15 cenários parametrizados nos testes existentes.

Os demais métodos presentes na classe, relacionados a operações como criação, exclusão e consulta de múltiplos registros, verificam comportamentos de funcionalidades não implementadas. Eles não representam cenários funcionais da jornada Edit Contact Details.

### 7.2. Infraestrutura de testes documentada no projeto

Além da inspeção do código-fonte, foram consultadas as instruções técnicas do repositório OrangeHRM, especialmente as referências relacionadas às ferramentas PHPUnit, banco de dados de testes e ambiente de desenvolvimento.

As informações desta subseção representam procedimentos documentados no projeto, não resultados de uma execução realizada durante esta análise.

**Banco de dados de testes**

O OrangeHRM utiliza um banco de dados separado para a execução dos testes automatizados, normalmente denominado `ohrm_test`.

O bootstrap do PHPUnit está localizado em:

`src/test/phpunit/Util/bootstrap.php`

Conforme a documentação do projeto, esse arquivo verifica a disponibilidade do banco de testes e a preparação dos dados essenciais antes de permitir a execução dos testes.

A criação inicial do banco é realizada pelo comando:

`php devTools/core/console.php instance:create-test-db -p root --dump-options=--ssl=0`

Esse procedimento cria o banco, executa as migrações necessárias, carrega os dados essenciais do sistema e gera um arquivo SQL para facilitar a restauração do ambiente em execuções posteriores.

**Execução dos testes com PHPUnit**

O projeto utiliza PHPUnit para executar os testes automatizados do backend.

A configuração principal está no arquivo `phpunit.xml`, localizado na raiz do repositório. Os testes são organizados em suítes por plugin, incluindo a suíte `Pim`.

A documentação apresenta diferentes possibilidades de execução:

- Executar todos os testes: `./src/vendor/bin/phpunit`
- Executar os testes do plugin PIM: `./src/vendor/bin/phpunit --testsuite Pim`
- Executar uma classe específica: `./src/vendor/bin/phpunit src/plugins/orangehrmPimPlugin/test/Api/EmployeeContactDetailsAPITest.php`

A execução direcionada permite concentrar a análise na funcionalidade selecionada, sem a necessidade de executar toda a suíte do sistema.

**Testes do frontend**

O OrangeHRM utiliza Jest para os testes unitários do frontend, desenvolvido com Vue.

A documentação aponta que a cobertura de componentes Vue é limitada e que a abordagem predominante consiste em testar funções utilitárias isoladamente, especialmente regras de validação e funções auxiliares.

Essa característica é relevante para a jornada analisada, pois as validações de campos presentes em `EmployeeContactDetails.vue` também podem ser investigadas considerando a lógica de validação utilizada pelo frontend.

**Testes de ponta a ponta**

O projeto também disponibiliza testes E2E com Cypress, localizados em:

`src/test/functional/`

Esses testes exigem uma instância do OrangeHRM em execução, devidamente instalada e configurada.

Por envolverem a interação com a aplicação em funcionamento, os testes E2E podem complementar os testes de API na verificação de jornadas completas do usuário.

### 7.3. Considerações sobre a análise

A inspeção realizada demonstra que a funcionalidade Edit Contact Details já possui testes automatizados de API, com cenários de consulta, validação e atualização de dados.

Entretanto, a existência desses testes não garante, isoladamente, a cobertura completa da jornada do usuário. A análise identificou oportunidades de investigação relacionadas à interação com o frontend, às divergências entre limites de validação da interface e da API e à persistência dos dados após a atualização.

A infraestrutura documentada pelo OrangeHRM oferece recursos para testar diferentes camadas da aplicação, incluindo API, frontend e integração ponta a ponta.

É importante destacar que a consulta às instruções técnicas não equivale à execução dos testes. Até o momento desta análise, os cenários existentes foram inspecionados estaticamente, sem confirmação de execução bem-sucedida no ambiente de testes.

Por esse motivo, os resultados de execução deverão ser registrados separadamente no Relatório da Execução dos Testes.


## 8. Lacunas e oportunidades de teste

Com base na análise realizada, foram identificadas oportunidades para ampliar a cobertura dos testes:

- Verificar os limites de caracteres dos campos de endereço;
- Verificar os limites de caracteres do CEP;
- Testar valores exatamente no limite permitido;
- Testar valores imediatamente acima do limite;
- Verificar a atualização individual de cada campo;
- Verificar a atualização simultânea de endereço, telefones e e-mails;
- Verificar a persistência dos dados após uma atualização bem-sucedida;
- Verificar o comportamento com campos opcionais vazios;
- Verificar a consistência das validações entre frontend e backend;
- Verificar o comportamento de atualização para funcionários sem dados de contato previamente cadastrados.

Esses itens representam oportunidades de teste identificadas na análise, não defeitos já comprovados.

## 9. Limitações da análise

Esta etapa foi realizada por inspeção estática dos arquivos de implementação, testes e fixtures.

Não foram executados os testes automatizados nem realizados testes manuais pela interface. Portanto, não há resultados de aprovação ou reprovação a registrar nesta etapa.

Também não foram encontrados arquivos de teste frontend com nomes correspondentes a `EmployeeContactDetails` durante a busca realizada. Isso não comprova a inexistência de testes indiretos para a interface.

## 10. Conclusão

A funcionalidade de edição dos dados de contato possui testes automatizados de integração para consulta e atualização, com cenários relacionados a identificadores inválidos, telefones, e-mails, unicidade e limites de caracteres.

A análise identificou uma diferença entre os limites de caracteres definidos no frontend e no backend para os campos de endereço e CEP. Essa inconsistência representa uma oportunidade relevante para a elaboração de novos testes de fronteira.

A próxima etapa será elaborar o Plano de Testes, definindo os cenários, dados de entrada, resultados esperados e critérios de aceitação para a jornada PIM-EditContactDetails.

## 11. Referências

Repositório oficial do OrangeHRM:  
https://github.com/orangehrm/orangehrm

Arquivos principais consultados:

- `src/client/src/orangehrmPimPlugin/pages/employee/EmployeeContactDetails.vue`
- `src/plugins/orangehrmPimPlugin/Api/EmployeeContactDetailsAPI.php`
- `src/plugins/orangehrmPimPlugin/Api/Model/EmployeeContactDetailsModel.php`
- `src/plugins/orangehrmPimPlugin/test/Api/EmployeeContactDetailsAPITest.php`
- `src/plugins/orangehrmPimPlugin/test/fixtures/testcases/EmployeeContactDetailsAPITestCases.yaml`
- `src/plugins/orangehrmPimPlugin/test/fixtures/EmployeeContactDetailsAPI.yml`
- `src/plugins/orangehrmPimPlugin/Service/EmployeeService.php`
