
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

## 7. Lacunas e oportunidades de teste

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

## 8. Limitações da análise

Esta etapa foi realizada por inspeção estática dos arquivos de implementação, testes e fixtures.

Não foram executados os testes automatizados nem realizados testes manuais pela interface. Portanto, não há resultados de aprovação ou reprovação a registrar nesta etapa.

Também não foram encontrados arquivos de teste frontend com nomes correspondentes a `EmployeeContactDetails` durante a busca realizada. Isso não comprova a inexistência de testes indiretos para a interface.

## 9. Conclusão

A funcionalidade de edição dos dados de contato possui testes automatizados de integração para consulta e atualização, com cenários relacionados a identificadores inválidos, telefones, e-mails, unicidade e limites de caracteres.

A análise identificou uma diferença entre os limites de caracteres definidos no frontend e no backend para os campos de endereço e CEP. Essa inconsistência representa uma oportunidade relevante para a elaboração de novos testes de fronteira.

A próxima etapa será elaborar o Plano de Testes, definindo os cenários, dados de entrada, resultados esperados e critérios de aceitação para a jornada PIM-EditContactDetails.

## 10. Referências

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
