# Relatório da Execução dos Testes: PIM › Add Employee

> Plano correspondente: Issue #52, "Relatório do Plano de Testes" (`relatorios/plano-de-testes.md`).
> Execução em 12/11/2026, no ambiente local descrito em `docs/ambiente.md`; reexecução de confirmação em 16/11
> (unitários e integração) e 17/11 (sistema), já com a quarentena, com os mesmos resultados.

## 1. Resumo

**Foram encontrados 2 defeitos.** Dos 113 testes criados para a jornada, 107 passam e 6 falham. As 6 falhas são
os mesmos 2 defeitos, detectados nos três níveis:

| Nível | Ferramenta | Issue | Testes | Passam | Falham |
|---|---|---|---|---|---|
| Unitário | PHPUnit 9.6 | #10 | 49 | 47 | 2 (D1, D2) |
| Integração | JUnit 5 + REST Assured | #14 | 41 | 39 | 2 (D1, D2) |
| Sistema | JUnit 5 + Playwright for Java | #18 | 23 | 21 | 2 (D1, D2) |
| **Total** | | | **113** | **107** | **6** |

| Defeito | Severidade | Testes que o expõem |
|---|---|---|
| **D1**: depois de cadastrar um funcionário **com foto**, o sistema sugere de novo o mesmo Employee Id, e o próximo cadastro falha com "Employee Id already exists" | Média | UT-05, IT-12, ST-12 |
| **D2**: nenhuma senha de **8** caracteres é aceita, embora o sistema anuncie 8 como mínimo (inconsistência de especificação) | Baixa | UT-08, IT-10, ST-10 |

Os testes que falham não foram desativados nem marcados como ignorados. Eles descrevem o comportamento correto e
devem passar quando os defeitos forem corrigidos. Ficam em **quarentena** (marca `defeito-conhecido`): fora da
execução padrão, que fica verde e denuncia qualquer falha nova, e numa execução à parte (`mvn test -Pdefeitos`,
`phpunit --group defeito-conhecido`), que falha enquanto os defeitos existirem.

## 2. Ambiente de execução

| Item | Versão |
|---|---|
| Sistema sob teste | OrangeHRM 5.9, imagem `orangehrm/orangehrm:5.9` |
| Código-fonte para os testes unitários | `orangehrm/orangehrm`, commit `56e23b3b` |
| Banco de dados | MariaDB 10.11 |
| PHP / PHPUnit | 8.3 / 9.6.16 |
| Java / Maven | 21 (código compilado para 17) / 3.8.7 |
| Navegador | Chromium do Playwright 1.49 |
| Orquestração | Docker 29.9, Compose 5.7 (`ambiente/docker-compose.yml`) |

As suítes de integração e de sistema foram executadas **duas vezes seguidas**, com resultados idênticos. Depois de
cada execução, o sistema volta ao estado inicial: só o funcionário e o usuário Admin.

## 3. Resultados por nível

### 3.1 Testes originais do OrangeHRM (linha de base)

| Execução | Resultado |
|---|---|
| Suíte `Pim` completa | 674 testes, 2046 asserções: **todos passam** (53 *risky* por avisos de depreciação do PHP 8.3 impressos pelas classes de teste) |
| Testes da jornada em outros plugins (gerador de ID, usuários, política de senha) | 55 testes: **todos passam** |
| Suíte `Pim` + os nossos testes, sobre banco de teste recém-criado | 723 testes: falham **só** os 2 testes nossos que expõem D1 e D2 |

**Cobertura** dos testes originais nas classes da jornada (PCOV): `EmployeeAPI` 99,66% das linhas,
`IDGeneratorService` 100%, `EmployeePictureService` 100%, `UserAPI` 86,79%, `PasswordStrengthService` 85,81%,
`EmployeeService` 69,23%. Mesmo com cobertura quase total, nenhum teste original detecta o D1 (ver seção 5).

### 3.2 Unitários (`unitario/T2AddEmployee/`)

| Caso | Resultado |
|---|---|
| UT-01 a UT-04: nomes, obrigatórios, Employee Id (inclusive ID de expurgado reutilizável), foto | 26 de 26 passam |
| UT-05: contador de ID **sem** foto (controle) e foto gravada no banco | passam |
| UT-05: contador de ID **com** foto | **falha: D1** (esperado `0009`, obtido `0008`) |
| UT-06: usuário ESS não cria funcionário | passa |
| UT-07: formato do ID sugerido e valor gravado por `incrementId` | 7 de 7 passam |
| UT-08: classes inválidas de senha (I13–I20), senhas de 9 e 64 caracteres | passam |
| UT-08: senha de 8 caracteres que cumpre as regras | **falha: D2** ("could be guessable") |

### 3.3 Integração (`integracao/`)

| Caso | Resultado |
|---|---|
| IT-01 a IT-04: cadastro, obrigatórios, limites 30/31 e 10/11, ID duplicado | passam |
| IT-05 a IT-07: foto gravada, tipos proibidos, 1 MB − 1 e 1 MB aceitos, 1 MB + 1 e tamanho declarado falso recusados | passam |
| IT-08 e IT-09: usuário vinculado, username 4/5/40/41 e existente | passam |
| IT-10: senhas inválidas (422) e válidas de 9 e 64 caracteres (200) | passam |
| IT-10: senha de 8 caracteres que cumpre as regras | **falha: D2** (422) |
| IT-11: usuário recusado → funcionário permanece sem login | passa (caracteriza o risco R1) |
| IT-12: ID sugerido sem foto (controle) | passa |
| IT-12: ID sugerido depois de cadastrar com foto | **falha: D1** |

### 3.4 Sistema (`sistema/`)

| Caso | Resultado |
|---|---|
| ST-01 a ST-04: cadastro mínimo, com foto, com login (conferido em User Management), foto + login desativado | passam |
| ST-05 a ST-09: Required, ID duplicado, foto inválida e > 1 MB, foto de 1 MB exato, username existente, senhas diferentes | passam |
| ST-10: mensagens da política para senhas inválidas | passam |
| ST-10: senha de 8 caracteres que cumpre as regras | **falha: D2** |
| ST-11: limites de First Name, Employee Id e Username | passam |
| ST-12: ID sugerido sem foto (controle) | passa |
| ST-12: ID sugerido depois de cadastrar com foto | **falha: D1** |
| ST-13: Cancel | passa |

## 4. Defeitos encontrados

Descrição completa, com evidências, em `docs/defeitos.md`. Os dois defeitos estão registrados na Issue #22.

### D1: cadastro com foto não avança o Employee Id sugerido (severidade média)

**Passos:**
1. Abrir PIM › Add Employee e anotar o Employee Id sugerido (ex.: `0005`).
2. Cadastrar um funcionário **com foto**, sem alterar o ID.
3. Abrir Add Employee de novo.

**Esperado:** o sistema sugere `0006`.
**Obtido:** sugere de novo `0005`, e quem não alterar o campo recebe "Employee Id already exists" (API: 422).
Sem foto, o ID avança normalmente.

**Causa:** em `EmployeeAPI::create()`, só o ramo sem foto chama `EmployeeService::saveNewEmployee()`, que é quem
incrementa o contador (`IDGeneratorService::incrementId`). O ramo com foto salva o funcionário em cascata pela
entidade da foto e nunca incrementa o contador.

**Correção sugerida:** incrementar o contador também no ramo com foto.

### D2: senha de 8 caracteres nunca é aceita (severidade baixa, inconsistência de especificação)

**Passos:** Add Employee → *Create Login Details* → senha `Qz7#mX2k`. Ela tem 8 caracteres, maiúscula, minúscula,
número e caractere especial.

**Esperado:** aceita, pois o sistema informa "Should have at least 8 characters".
**Obtido:** "Your password meets the minimum requirements, but it could be guessable", e o cadastro do usuário
é recusado (API: 422). Com 9 caracteres (`t7#Rw!9Zp`) é aceita.

**Evidência:** 2.000 senhas aleatórias de 8 caracteres foram avaliadas com o mesmo algoritmo do sistema (zxcvbn),
cumprindo todas as regras explícitas. Nenhuma alcança a força mínima *strong* (score 3): todas ficam em score
2 ou menos. Com 9 e com 10 caracteres, todas alcançam score 3.

**Causa:** a política padrão combina mínimo 8 com força mínima *strong*, e essa força é inalcançável com 8
caracteres. O mínimo efetivo é 9.

**Classificação:** inconsistência entre a regra comunicada ao usuário e o comportamento real. O código faz o que
foi programado, mas o mínimo anunciado (8) é inatingível. Não é falha de segurança, porque o sistema fica mais
restritivo do que o anunciado.

**Correção sugerida:** anunciar 9 como mínimo quando a força exigida for *strong*, ou ajustar o padrão.

## 5. Riscos investigados

| Risco (da leitura do código) | Conclusão |
|---|---|
| R1: funcionário e usuário criados em duas requisições | **Confirmado como comportamento** (IT-11): se a 2ª requisição falha, o funcionário fica sem login. Na interface é difícil provocar a falha, porque as mesmas regras são validadas antes do envio. Fica registrado como risco de projeto, não como defeito. |
| R2: contador de ID no cadastro com foto | **Confirmado: defeito D1.** |
| R3: limite de 1 MB só no cliente | **Descartado** (IT-07): a API também valida o limite e compara o tamanho declarado com o conteúdo real. |
| R4: unicidade do Employee Id ignora funcionários expurgados | **Comportamento intencional confirmado** (UT-03): o ID de um expurgado pode ser reutilizado. |

## 6. Achados sobre os testes existentes

- **Cobertura alta não garantiu detecção.** `EmployeeAPI` tem 99,66% das linhas cobertas pelos testes originais.
  O caso "com foto" executa exatamente o ramo com defeito, mas o oráculo compara só o JSON de resposta, e o D1
  passa despercebido. A cobertura mede o que é executado, não o que é verificado.
- **A suíte original do PIM não é repetível.** `CustomFieldDaoTest` deixa registros no banco, e da 2ª execução
  em diante `CustomFieldAPITest::testDelete` falha. Reproduzido passo a passo (`analise/codigo-original.md`). O
  CI do projeto não percebe porque sempre parte de um banco novo.
- **Testes de terceiros desatualizados.** O projeto TestNG analisado espera a mensagem de mínimo de 7
  caracteres, mas a política atual é 8. O caso positivo dele usa senhas de 8 caracteres, que o D2 impede de
  passar (`analise/terceiros.md`).

## 7. Conclusão

A jornada **Admin cadastra um funcionário** funciona corretamente no fluxo principal, nos fluxos com foto, com
login e com ID manual, e em todas as exceções planejadas, nos três níveis. Os testes revelaram **dois defeitos**:
- **D1**, encontrado por teste guiado por risco a partir da leitura do código;
- **D2**, encontrado por análise de valor-limite da política de senha e classificado como inconsistência de
  especificação.

Os dois foram reproduzidos de forma independente nos três níveis.

## 8. Uso de IA e fontes

- **IA:** Claude Code (Anthropic, modelo Claude Opus 5.5) foi usado para:
  - montar o ambiente Docker;
  - implementar os testes;
  - executar as suítes e investigar as falhas;
  - escrever a primeira versão deste relatório.

  Todo o material foi revisado pelo autor. O registro detalhado (data e finalidade) está no diário do trabalho.
- **Fontes:**
  - OrangeHRM, https://github.com/orangehrm/orangehrm (commit `56e23b3b`) e imagem `orangehrm/orangehrm:5.9`.
  - zxcvbn-php (`bjeavons/zxcvbn-php` ~1.3.1, https://github.com/bjeavons/zxcvbn-php), biblioteca de força de
    senha usada pelo OrangeHRM (dependência em `src/composer.json`).
  - PHPUnit (https://phpunit.de), REST Assured (https://rest-assured.io), Playwright for Java (https://playwright.dev/java).
  - Projetos de terceiros citados em `analise/terceiros.md`.
