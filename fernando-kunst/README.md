# Testes do OrangeHRM — Fernando Kunst

Contribuição individual para o Trabalho 2 de Verificação e Validação.

A jornada selecionada consiste em solicitar um dia de licença pelo módulo Leave e consultar o pedido em My Leave.

## Casos implementados

| Caso | Nível | Verificação |
| --- | --- | --- |
| CT-UNI-01 | Unitário | Converter nomes de status em identificadores, preservando a ordem. |
| CT-INT-01 | Integração | Filtrar direitos de licença por colaborador, tipo e período pelo DAO original. |
| CT-SIS-01 | Sistema | Verificar a obrigatoriedade do campo Leave Type. |
| CT-SIS-02 | Sistema | Solicitar um dia de licença, consultar o pedido e cancelá-lo ao finalizar. |

Os testes de sistema utilizam Java, Selenium e JUnit. Os testes unitário e de integração utilizam PHP e PHPUnit. O Maven automatiza a execução dos quatro casos.

## Ambiente utilizado

- macOS em arquitetura ARM64.
- Java 27 no terminal; compilação configurada para Java 17.
- Maven 3.10.0.
- Chrome 157.
- Selenium 4.52.0 e JUnit 5.11.4.
- PHP 8.3.36 e Composer 2.11.2.
- PHPUnit 9.6.16, instalado pelas dependências do OrangeHRM.
- MySQL 8.4.13.
- OrangeHRM OS 5.9 no servidor público de demonstração.

## Preparação do código original

O clone do OrangeHRM deve ficar ao lado do repositório da turma:

```text
diretorio/
├── literate-lamp/
│   └── fernando-kunst/
└── orangehrm/
```

A partir da raiz de `literate-lamp`, execute:

```bash
git clone https://github.com/orangehrm/orangehrm.git ../orangehrm
git -C ../orangehrm checkout 56e23b3b09e7af29317a0943523b825843fff527
composer install --working-dir=../orangehrm/src --no-scripts
```

Se o clone já existir, não repita o comando `git clone`. O checkout pressupõe que não existam alterações locais pendentes nesse clone.

O PHP deve possuir a extensão `pdo_mysql` habilitada.

## Preparação do banco local

Os testes foram configurados para conectar ao MySQL em `127.0.0.1:3306`, com usuário `root`, senha vazia e banco `orangehrm_fernando_test`.

Essa configuração corresponde ao ambiente local utilizado. Para outro ambiente, ajuste os dados em `src/test/php/bootstrap-integration.php`. Não publique senhas pessoais no repositório.

Com o MySQL iniciado, crie o banco:

```sql
CREATE DATABASE IF NOT EXISTS orangehrm_fernando_test
CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

O teste de integração cria uma tabela temporária e insere seus próprios quatro registros. A tabela existe apenas durante a conexão.

## Preparação dos testes de sistema

Servidor utilizado:

https://opensource-demo.orangehrmlive.com/

Os testes utilizam as credenciais públicas de demonstração `Admin` e `admin123`.

Antes da execução:

1. Identifique o colaborador associado à conta utilizada.
2. Garanta saldo disponível do tipo `CAN - Personal` para esse colaborador.
3. Garanta que a validade do saldo inclua a data do pedido.
4. Verifique que não exista solicitação ativa conflitante nessa data.

A data padrão configurada no teste é **30/11/2026**. O CT-SIS-02 utiliza um comentário único para identificar o pedido criado e realiza seu cancelamento na etapa de limpeza.

O servidor é compartilhado e seus dados podem ser alterados ou reinicializados.

## Execução pelo Maven

Na raiz de `literate-lamp`, execute:

```bash
mvn -f fernando-kunst/pom.xml test
```

No macOS com PHP 8.3 instalado pelo Homebrew, é possível indicar o executável explicitamente:

```bash
mvn -f fernando-kunst/pom.xml "-Dphp.executable=$(brew --prefix php@8.3)/bin/php" test
```

Os resultados Java são gravados em `target/surefire-reports`, dentro de `fernando-kunst`. O resultado PHP é gravado em `target/phpunit-results.xml`.

Os resumos Java e PHP aparecem separadamente no terminal. Uma falha interrompe a execução do Maven; nesse caso, os testes das etapas seguintes podem não ser executados.

## Resultado registrado

Em 15/11/2026, a execução integrada terminou com `BUILD SUCCESS`.

Foram aprovados dois testes de sistema em Java e dois testes PHP. O PHPUnit apresentou `OK (2 tests, 4 assertions)`.

## Limitações

O teste de integração executa o DAO e o mapeamento Doctrine originais contra o MySQL real, mas utiliza uma tabela temporária com esquema reduzido e sem chaves estrangeiras. Portanto, verifica os filtros da consulta, sem validar a integridade referencial ou a instalação completa do OrangeHRM.

Os quatro casos não representam cobertura completa do sistema.

## Documentação e fontes

- [Plano de Testes](https://github.com/masmangan/literate-lamp/wiki/Plano-de-Testes)
- [Relatório da Execução dos Testes](https://github.com/masmangan/literate-lamp/wiki/Relatorio-da-Execucao-dos-Testes)
- [Código original do OrangeHRM](https://github.com/orangehrm/orangehrm)
- [Método e testes de status analisados](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/src/plugins/orangehrmLeavePlugin/test/Service/LeaveRequestServiceTest.php)
- [Testes originais do DAO analisados](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/src/plugins/orangehrmLeavePlugin/test/Dao/LeaveEntitlementDaoTest.php)

## Uso de IA

Foi utilizado o Codex, da OpenAI, como apoio ao planejamento, análise, implementação dos testes, configuração do ambiente, diagnóstico de erros e redação da documentação. Os testes foram executados pelo participante.