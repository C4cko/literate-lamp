# Plano de Testes — PIM Edit Contact Details

**Issue:** #73  
**Identificador:** PIM-EditContactDetails  
**Projeto:** OrangeHRM  
**Funcionalidade:** Edição dos dados de contato de funcionários  
**Responsável:** Henrique

## 1. Objetivo

Definir os testes necessários para avaliar a funcionalidade de consulta e edição dos dados de contato de um funcionário no módulo PIM do OrangeHRM.

O plano busca verificar o comportamento da interface, as validações dos campos, a comunicação com a API e a persistência das alterações realizadas.

## 2. Escopo dos testes

A jornada contemplada é:

1. Acessar o módulo PIM.
2. Abrir a opção Employee List.
3. Selecionar um funcionário.
4. Acessar a seção Contact Details.
5. Consultar os dados existentes.
6. Alterar os dados de contato.
7. Salvar as alterações.
8. Verificar se os dados foram persistidos corretamente.

Os campos contemplados são:

- Street 1;
- Street 2;
- City;
- Province;
- Zip Code;
- Country;
- Home Telephone;
- Mobile;
- Work Telephone;
- Work Email;
- Other Email.

## 3. Estratégia de testes

Serão priorizados testes funcionais e de validação, considerando os seguintes aspectos:

- **Testes positivos:** verificar se dados válidos são aceitos e salvos.
- **Testes negativos:** verificar se dados inválidos são rejeitados.
- **Testes de fronteira:** verificar valores nos limites e acima dos limites permitidos.
- **Testes de integração:** verificar a comunicação entre a interface e a API.
- **Testes de persistência:** verificar se as alterações permanecem após recarregar a página.

A análise também considera os testes automatizados existentes na API, evitando tratar cenários já cobertos como se fossem necessariamente inéditos.

## 4. Ambiente e pré-condições

### Ambiente

- Aplicação OrangeHRM instalada e acessível.
- Navegador atualizado.
- Banco de dados configurado.
- Usuário autenticado com permissão para acessar o módulo PIM.
- Funcionário de teste cadastrado.

### Pré-condições gerais

- O usuário deve estar autenticado.
- O funcionário utilizado deve existir no sistema.
- O usuário deve possuir permissão para consultar e editar os dados de contato.
- Os dados utilizados nos testes devem ser controlados para evitar conflitos de unicidade de e-mail.

## 5. Casos de teste

### CT01 — Consultar dados de contato

**Objetivo:** verificar se os dados de contato de um funcionário são carregados corretamente.

**Pré-condições:** funcionário existente e com dados de contato cadastrados.

**Procedimento:**
1. Acessar o módulo PIM.
2. Abrir Employee List.
3. Selecionar um funcionário.
4. Acessar Contact Details.

**Resultado esperado:** os campos devem apresentar os dados de contato previamente cadastrados para o funcionário selecionado.

**Tipo:** Funcional / Positivo.

### CT02 — Atualizar dados de contato com informações válidas

**Objetivo:** verificar se o sistema permite atualizar os dados de contato utilizando informações válidas.

**Pré-condições:** funcionário existente e usuário com permissão de edição.

**Dados de entrada:**
- Street 1: Rua de Teste, 100;
- Street 2: Apartamento 101;
- City: Porto Alegre;
- Province: RS;
- Zip Code: 90000-000;
- Home Telephone: 5133334444;
- Mobile: 51999999999;
- Work Telephone: 5132223333;
- Work Email: henrique.teste01@example.com;
- Other Email: henrique.alternativo01@example.com.

**Procedimento:**
1. Abrir Contact Details.
2. Preencher os campos com os dados informados.
3. Acionar Save.
4. Aguardar a resposta do sistema.

**Resultado esperado:** o sistema deve aceitar os dados válidos, apresentar confirmação da atualização e manter os valores informados.

**Tipo:** Funcional / Positivo.

### CT03 — Atualizar somente um campo

**Objetivo:** verificar se é possível alterar individualmente um dado de contato sem modificar os demais.

**Pré-condições:** funcionário com dados de contato previamente cadastrados.

**Procedimento:**
1. Acessar Contact Details.
2. Alterar somente o campo City.
3. Salvar as alterações.
4. Recarregar a página.

**Resultado esperado:** o novo valor de City deve permanecer salvo, enquanto os demais campos devem conservar seus valores anteriores.

**Tipo:** Funcional / Positivo.

### CT04 — Salvar campos opcionais vazios

**Objetivo:** verificar o comportamento do sistema quando campos opcionais são deixados vazios.

**Pré-condições:** funcionário existente.

**Procedimento:**
1. Acessar Contact Details.
2. Limpar um ou mais campos opcionais, como Street 2 e Other Email.
3. Salvar as alterações.

**Resultado esperado:** o sistema deve aceitar os campos vazios quando não houver uma regra de obrigatoriedade aplicável, sem apresentar erros indevidos.

**Tipo:** Funcional / Negativo controlado.

### CT05 — Informar telefone com formato inválido

**Objetivo:** verificar se o sistema rejeita números de telefone em formato inválido.

**Dados de entrada:**
- Home Telephone: `telefone-invalido`.

**Procedimento:**
1. Acessar Contact Details.
2. Informar um valor inválido no campo Home Telephone.
3. Tentar salvar.

**Resultado esperado:** a interface deve indicar que o telefone possui formato inválido e impedir o envio dos dados incorretos.

**Tipo:** Validação / Negativo.

### CT06 — Informar telefone acima do limite de caracteres

**Objetivo:** verificar a validação do tamanho máximo dos campos de telefone.

**Dados de entrada:**
- Home Telephone: valor com 26 caracteres.

**Procedimento:**
1. Acessar Contact Details.
2. Informar um telefone com 26 caracteres.
3. Tentar salvar.

**Resultado esperado:** o sistema deve rejeitar o valor que ultrapassa o limite de 25 caracteres.

**Tipo:** Validação / Fronteira.

### CT07 — Informar e-mail com formato inválido

**Objetivo:** verificar se o sistema rejeita endereços de e-mail malformados.

**Dados de entrada:**
- Work Email: `email-invalido`.

**Procedimento:**
1. Acessar Contact Details.
2. Informar o endereço inválido no campo Work Email.
3. Tentar salvar.

**Resultado esperado:** o sistema deve apresentar uma indicação de formato inválido e impedir a atualização com esse valor.

**Tipo:** Validação / Negativo.

### CT08 — Utilizar e-mail já cadastrado para outro funcionário

**Objetivo:** verificar a regra de unicidade dos endereços de e-mail.

**Pré-condições:** existência de outro funcionário com o endereço de e-mail que será utilizado.

**Procedimento:**
1. Acessar Contact Details de um funcionário.
2. Informar no campo Work Email um endereço já utilizado por outro funcionário.
3. Tentar salvar.

**Resultado esperado:** o sistema deve rejeitar o endereço já cadastrado e informar que o valor não está disponível.

**Tipo:** Validação / Negativo.

### CT09 — Utilizar o mesmo endereço nos dois campos de e-mail

**Objetivo:** verificar se o sistema impede que Work Email e Other Email possuam o mesmo valor.

**Dados de entrada:**
- Work Email: `henrique.teste02@example.com`;
- Other Email: `henrique.teste02@example.com`.

**Procedimento:**
1. Acessar Contact Details.
2. Informar o mesmo endereço nos dois campos.
3. Tentar salvar.

**Resultado esperado:** o sistema deve rejeitar a atualização, pois os dois campos não podem possuir o mesmo endereço de e-mail.

**Tipo:** Validação / Negativo.

### CT10 — Informar e-mail acima do limite permitido

**Objetivo:** verificar o limite máximo de caracteres dos campos de e-mail.

**Dados de entrada:**
- Work Email: endereço com mais de 50 caracteres.

**Procedimento:**
1. Acessar Contact Details.
2. Informar um endereço com mais de 50 caracteres.
3. Tentar salvar.

**Resultado esperado:** o sistema deve rejeitar o valor que ultrapassa o limite de 50 caracteres.

**Tipo:** Validação / Fronteira.

### CT11 — Verificar limite dos campos de endereço

**Objetivo:** investigar a diferença identificada entre os limites de caracteres do frontend e do backend.

**Dados de entrada:**
- Street 1: valor com 71 caracteres;
- City: valor com 71 caracteres;
- Province: valor com 71 caracteres.

**Procedimento:**
1. Acessar Contact Details.
2. Informar valores com 71 caracteres nos campos indicados.
3. Observar a validação da interface.
4. Caso seja possível, verificar também o comportamento da API.

**Resultado esperado:** registrar se a interface rejeita os valores e comparar o comportamento com a API. A interface define limite de 70 caracteres, enquanto a API define limite de 100. O comportamento esperado definitivo depende da confirmação do requisito funcional.

**Tipo:** Validação / Fronteira / Investigativo.

### CT12 — Verificar limite do CEP

**Objetivo:** investigar a diferença entre o limite de caracteres do CEP na interface e na API.

**Dados de entrada:**
- Zip Code: valor com 11 caracteres.

**Procedimento:**
1. Acessar Contact Details.
2. Informar um CEP com 11 caracteres.
3. Observar a validação da interface.
4. Caso seja possível, verificar também o comportamento da API.

**Resultado esperado:** registrar o comportamento da interface e da API. A interface define limite de 10 caracteres, enquanto a API define limite de 20. O comportamento esperado definitivo depende da confirmação do requisito funcional.

**Tipo:** Validação / Fronteira / Investigativo.

### CT13 — Verificar persistência após recarregar a página

**Objetivo:** verificar se as alterações realizadas são efetivamente persistidas.

**Pré-condições:** atualização válida realizada com sucesso.

**Procedimento:**
1. Alterar um ou mais dados de contato.
2. Salvar as alterações.
3. Recarregar a página.
4. Consultar novamente os dados do funcionário.

**Resultado esperado:** os dados atualizados devem continuar sendo apresentados após o recarregamento da página.

**Tipo:** Integração / Persistência.

## 6. Priorização

| Caso | Prioridade | Justificativa |
|---|---|---|
| CT01 | Alta | Verifica a consulta inicial dos dados. |
| CT02 | Alta | Verifica a operação principal da funcionalidade. |
| CT03 | Média | Verifica atualização parcial dos dados. |
| CT04 | Média | Verifica o tratamento de campos opcionais. |
| CT05 | Alta | Verifica validação de telefone. |
| CT06 | Média | Verifica limite de telefone. |
| CT07 | Alta | Verifica formato de e-mail. |
| CT08 | Alta | Verifica unicidade de e-mail. |
| CT09 | Alta | Verifica regra de e-mails diferentes. |
| CT10 | Média | Verifica limite de e-mail. |
| CT11 | Alta | Investiga divergência entre frontend e backend. |
| CT12 | Alta | Investiga divergência entre frontend e backend. |
| CT13 | Alta | Verifica persistência das alterações. |

## 7. Critérios de entrada

Os testes poderão ser iniciados quando:

- A aplicação estiver disponível;
- O ambiente estiver configurado;
- Existir um usuário com permissões adequadas;
- Existir pelo menos um funcionário para utilização nos testes;
- Os dados de teste estiverem definidos.

## 8. Critérios de saída

A execução será considerada concluída quando todos os casos planejados tiverem sido executados ou justificados como não executados.

Os resultados deverão ser registrados individualmente como:

- **Passou:** comportamento observado corresponde ao resultado esperado.
- **Falhou:** comportamento observado diverge do resultado esperado.
- **Bloqueado:** não foi possível executar por dependência ou impedimento.
- **Não executado:** teste ainda não realizado.

Falhas deverão conter evidências, como mensagens apresentadas, capturas de tela ou respostas da API, quando aplicável.

## 9. Riscos e observações

A análise estática identificou divergências entre os limites de caracteres definidos na interface e na API para Street 1, Street 2, City, Province e Zip Code.

Essas diferenças devem ser observadas durante a execução, pois podem provocar comportamentos distintos dependendo da forma de envio dos dados.

Os cenários CT11 e CT12 possuem caráter investigativo. Não se deve classificar automaticamente o comportamento como defeito sem confirmar os requisitos funcionais.