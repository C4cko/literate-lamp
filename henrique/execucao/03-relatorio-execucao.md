# [PIM-EditContactDetails] Relatório da Execução dos Testes

## 1. Identificação

- **Projeto:** OrangeHRM
- **Jornada:** PIM — Employee List → Edit Contact Details
- **Responsável:** Henrique
- **Issue:** #74
- **Classe de teste:** `EmployeeContactDetailsAPITest.php`
- **Framework:** PHPUnit 9.6.16

## 2. Objetivo

Registrar a execução dos testes automatizados relacionados à API de dados de contato do funcionário, verificando os comportamentos de consulta e atualização, bem como as respostas esperadas para operações ainda não implementadas.

## 3. Ambiente de execução

Os testes foram executados no GitHub Codespaces, utilizando o ambiente Docker do OrangeHRM.

| Componente | Configuração |
|---|---|
| Sistema de execução | GitHub Codespaces |
| Container PHP | `os_dev_php74` |
| PHP | 7.4.33 |
| Composer | 2.11.2 |
| PHPUnit | 9.6.16 |
| Banco de dados | MariaDB 10.3 |
| Banco principal | `orangehrm_mysql` |
| Banco de testes | `test_orangehrm_mysql` |

O banco principal foi instalado e sua conexão foi validada. Foram identificadas 171 tabelas.

O banco de testes foi preparado pelo comando:

```bash
php devTools/core/console.php instance:create-test-db -p root --dump-options=--ssl=0
```

A execução retornou:

```text
[OK] Test db test_orangehrm_mysql created
[OK] Core fixtures generated.
```

## 4. Procedimento de execução

A classe de teste foi executada a partir da raiz do OrangeHRM:

```bash
./src/vendor/bin/phpunit \
  src/plugins/orangehrmPimPlugin/test/Api/EmployeeContactDetailsAPITest.php
```

Posteriormente, foi realizada uma segunda execução utilizando a opção `--testdox`, permitindo identificar individualmente os cenários executados:

```bash
./src/vendor/bin/phpunit \
  --testdox \
  src/plugins/orangehrmPimPlugin/test/Api/EmployeeContactDetailsAPITest.php
```

## 5. Resultados obtidos

**Resultado geral: 21 testes aprovados, com 25 asserções e nenhuma falha.**

Saída da primeira execução:

```text
PHPUnit 9.6.16 by Sebastian Bergmann and contributors.

.....................                                             21 / 21 (100%)

Time: 00:00.724, Memory: 22.00 MB

OK (21 tests, 25 assertions)
```

Saída da segunda execução:

```text
Time: 00:00.529, Memory: 18.00 MB

OK (21 tests, 25 assertions)
```

### 5.1 Cenários de consulta

Foram aprovados dois cenários:

- Consulta dos dados de um funcionário válido.
- Consulta utilizando um `empNumber` inválido.

### 5.2 Cenários de atualização

Foram aprovados 13 cenários:

- Atualização com corpo vazio.
- Telefone residencial com mais de 25 caracteres.
- Todos os telefones com mais de 25 caracteres.
- Número de telefone inválido.
- E-mail com formato inválido.
- E-mail profissional já cadastrado.
- Outro e-mail já cadastrado.
- Verificação de unicidade entre os tipos de e-mail.
- Utilização do e-mail alternativo de outro funcionário como e-mail profissional.
- E-mail com mais de 50 caracteres.
- Mesmo endereço de e-mail nos campos profissional e alternativo.
- Alteração de um dos e-mails quando ambos estavam previamente salvos com o mesmo valor.

### 5.3 Operações não implementadas

Também foram aprovados seis testes que verificam o comportamento esperado para operações não implementadas:

- Create.
- Get validation rule for create.
- Get all.
- Get validation rule for get all.
- Delete.
- Get validation rule for delete.

Esses testes não comprovam o funcionamento dessas operações. Eles verificam que a API apresenta o comportamento previsto para funcionalidades não disponíveis.

## 6. Análise dos resultados

Todas as verificações executadas pela classe `EmployeeContactDetailsAPITest` foram aprovadas nas duas execuções.

Os resultados indicam que os comportamentos cobertos pelos testes existentes estão funcionando conforme as expectativas definidas nos casos de teste.

Foram exercitadas validações de campos obrigatórios, limites de caracteres, formatos de telefone e e-mail, unicidade de endereços de e-mail e regras de atualização.

Não foram identificadas falhas durante essas execuções.

Entretanto, o resultado não comprova a ausência de defeitos em toda a jornada. A classe executada é voltada à API e não substitui testes de interface, testes exploratórios ou testes end-to-end da navegação completa no OrangeHRM.

## 7. Limitações

- A execução foi concentrada na classe de testes da API de dados de contato.
- Não foi realizada uma execução automatizada da jornada completa pela interface gráfica.
- Não foram executados testes específicos de frontend com Jest.
- Os limites divergentes entre frontend e backend identificados na análise estática ainda precisam ser investigados por testes próprios.
- Os resultados apresentados correspondem aos testes existentes no projeto, não necessariamente a todos os casos definidos no plano de testes deste trabalho.

## 8. Conclusão

A execução da classe `EmployeeContactDetailsAPITest` foi concluída com sucesso. Foram aprovados 21 testes e 25 asserções, sem falhas, em duas execuções consecutivas.

Os resultados fornecem evidências positivas sobre os comportamentos de consulta e atualização de dados de contato implementados na API.

As limitações identificadas indicam oportunidades para ampliar a cobertura, especialmente em relação à interface gráfica, à persistência dos dados após atualização e às divergências entre as regras de validação do frontend e do backend.

## 9. Uso de inteligência artificial

Foi utilizada inteligência artificial como apoio à análise do código-fonte, interpretação da infraestrutura de testes, preparação do ambiente de execução e organização deste relatório.

A execução dos comandos e a conferência dos resultados foram realizadas no ambiente do projeto.

## 10. Evidências

As evidências da execução são as saídas reais do PHPUnit registradas no terminal do GitHub Codespaces, incluindo:

- Execução convencional: `OK (21 tests, 25 assertions)`.
- Execução com `--testdox`: identificação individual dos 21 testes aprovados.
- Criação do banco de testes e geração das Core Fixtures.