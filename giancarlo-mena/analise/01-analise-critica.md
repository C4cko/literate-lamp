# Análise crítica dos testes existentes – módulo Recruitment

Esta análise considera o código do OrangeHRM no repositório oficial
(`orangehrm/orangehrm`, branch `main`, commit `56e23b3`, junho de 2026) e dois materiais de terceiros
encontrados na web. O foco é o módulo **Recruitment**, que contém a jornada escolhida (ver `docs/01-jornada.md`).

Critérios usados (vistos na disciplina):

- **nível** do teste (unitário, integração, sistema) e isolamento;
- **técnica de projeto** aplicada (partição de equivalência, valor-limite, tabela de decisão, transição de estados);
- **oráculo**: o que o teste realmente verifica;
- **cobertura** dos requisitos e dos caminhos de erro;
- **legibilidade e manutenção** (nomes, duplicação, dados de teste).

## 1. Testes do código original

### 1.1 O que existe

Os testes do plugin ficam em `src/plugins/orangehrmRecruitmentPlugin/test/` e usam PHPUnit.

| Pasta | Arquivos | O que testa |
|---|---|---|
| `Api/` | 17 classes (`CandidateAPITest`, `CandidateShortlistingAPITest`, `CandidateHiringAPITest`...) | endpoints da API REST v2, uma classe por endpoint |
| `Dao/` | `CandidateDaoTest`, `VacancyDaoTest`, `RecruitmentAttachmentDaoTest` | consultas Doctrine contra banco com fixtures |
| `Entity/` | 5 classes | getters/setters e decorators das entidades |
| `Controller/` | 3 classes | controllers públicos (RSS, lista de vagas, candidatura online) |
| `fixtures/testcases/*.yaml` | 16 arquivos, cerca de 150 casos | dados de entrada e saída esperada dos testes de API |

Não há testes de sistema (Cypress) para Recruitment: a pasta `src/test/functional/cypress/e2e` só cobre
Leave, Admin e Login.

### 1.2 Nível real dos testes

Os testes de `Api/` estendem `EndpointIntegrationTestCase`. Eles carregam fixtures no banco, instanciam o
endpoint com serviços reais (`CandidateService`, `UserRoleManager`) e comparam o JSON resultante com o YAML.
Na prática são **testes de integração** (endpoint + serviço + DAO + banco), mesmo estando misturados com
testes de entidade que são de fato unitários. Não existe nenhum teste unitário das regras de negócio isoladas
(por exemplo, da máquina de estados do recrutamento), porque essas regras estão em dados
(`ohrm_workflow_state_machine`) e não em código.

### 1.3 Pontos fortes

- **Dados separados do código.** Os casos ficam em YAML e o PHPUnit gera um teste por caso
  (`dataProvider`). Fica fácil acrescentar um caso novo.
- **Papéis cobertos.** Quase todas as ações têm casos para Admin, Hiring Manager e ESS User, o que testa a
  autorização da ação.
- **Alguns caminhos de erro.** Candidato inexistente (404), candidato sem vaga, ação repetida
  ("already shortlisted") e limite de duas entrevistas aparecem nos YAML.
- A tabela de ações permitidas (`CandidateAllowedActionsTestCases.yaml`) tem um caso por estado da
  máquina, o que é uma boa cobertura de estados.

### 1.4 Problemas encontrados

1. **Transições inválidas quase não são testadas.** Cada ação testa só "repetir a mesma ação"
   (ex.: shortlist de quem já está `SHORTLISTED`). Não há casos como "contratar quem só está em
   `APPLICATION INITIATED`" ou "agendar entrevista de quem foi `REJECTED`". Pela técnica de transição de
   estados, a máquina tem 10 estados e 8 ações; cobrir só a diagonal deixa a maior parte dos pares
   (estado, ação) inválidos sem teste.
2. **Oráculo fraco na contratação.** `CandidateHiringTestCases.yaml` só confere o registro de histórico
   devolvido (`action.label = 'Hired'`). O efeito principal da ação, que é criar um funcionário no PIM com
   nome, e-mail e cargo do candidato (`AbstractCandidateActionAPI::setCandidateAsEmployee`), não é verificado.
3. **Faltam valores-limite.** Nomes têm limite de 30 caracteres, palavras-chave e comentário 250, entrevista
   de 1 a 5 entrevistadores. Os YAML testam só "sem o campo" e "6 entrevistadores"; não há casos para 30/31
   caracteres nem para 1 e 5 entrevistadores (os limites válidos).
4. **Partições de e-mail e telefone ausentes.** Não há caso de e-mail mal formado, e-mail vazio (`""`) ou
   telefone com letras. Ao ler o validador `Rules/Email.php`, nota-se que string vazia é aceita como válida,
   embora o e-mail seja obrigatório na interface e no contrato OpenAPI. Esse comportamento não é testado
   (vira a hipótese H1 do nosso plano).
5. **Copiar e colar nos nomes dos casos.** Em `CandidateHiringTestCases.yaml` os casos se chamam
   "Candidate decline job offer - Admin" e "Offer job candidate - ESS User", mas testam a contratação.
   O nome da classe `CandidateMarkIngInterviewFailAPITest` também tem erro de digitação. Isso atrapalha a
   leitura de relatórios de falha.
6. **Duplicação.** As 7 classes de ação (`Shortlisting`, `Rejection`, `Hiring`...) têm o mesmo corpo de
   85 linhas, mudando só o nome da classe e do YAML. Um teste parametrizado único seria mais fácil de manter.
7. **Sem testes de interface.** Divergências entre front-end e API ficam sem detecção. Exemplo: a tela limita
   o e-mail a 50 caracteres e o telefone a 25 (`SaveCandidate.vue`), a API não limita nenhum dos dois e o
   banco aceita 100 e 30 (`entity/Candidate.php`).

### 1.5 Resumo

| Critério | Avaliação |
|---|---|
| Nível | Integração quase só; unitário apenas em entidades; nenhum teste de sistema |
| Técnicas | Partição parcial (presente/ausente); valor-limite e transição de estados incompletos |
| Oráculo | Bom para o JSON de resposta; fraco para efeitos colaterais (funcionário criado, status salvo) |
| Manutenção | Dados em YAML ajudam; classes duplicadas e nomes errados atrapalham |

## 2. Material de terceiros

### 2.1 Casos manuais "OrangeHRM_TestCases" (marinaincodeland)

Fonte: <https://marinaincodeland.github.io/OrangeHRM_TestCases/html/recruitment-functionality.html>

Conjunto de 17 casos manuais (TC-037 a TC-053) para o servidor de demonstração, cobrindo cadastro, edição e
exclusão de candidatos e vagas.

- **Positivo:** formato claro (pré-condição, passos, resultado esperado) e um bom uso de valor-limite no
  campo "Number of Positions" (1, 99 válidos; 0, negativo e 100 inválidos).
- **Negativo:** nenhum caso trata a mudança de status do candidato (shortlist, entrevista, oferta,
  contratação), que é o centro do processo de recrutamento. Faltam casos negativos para o candidato
  (e-mail inválido, telefone inválido, nome longo). Vários resultados esperados são vagos ("uma mensagem
  de confirmação é exibida"), o que dificulta decidir se o teste passou.

### 2.2 Framework "OrangeHRM_UIAutomationFrameWork" (shishurajpandey)

Fonte: <https://github.com/shishurajpandey/OrangeHRM_UIAutomationFrameWork>

Framework de UI com Selenium 3.14, Java 8, TestNG e Page Object Model, que inclui uma página de
Recruitment entre os fluxos automatizados.

- **Positivo:** uso de Page Objects e relatórios (Extent Reports), que são boas práticas de testes de
  sistema.
- **Negativo:** só testa pela interface, com versões antigas (Selenium 3, Java 8) e sem camada de API.
  Testes só de UI contra o servidor público tendem a ser instáveis, porque esse servidor é reiniciado e
  alterado por outros usuários. A descrição do repositório lista os fluxos automatizados, mas não um projeto
  de casos (técnicas usadas, oráculo de cada caso).

## 3. O que levamos para o nosso plano

| Lacuna observada | Como o nosso plano trata |
|---|---|
| Transições inválidas sem teste | Tabela completa de estados × ações, testada no modelo (unitário) e na API (integração) |
| Contratação sem verificar o funcionário criado | Caso de integração que busca o funcionário no PIM após `hire` |
| Valores-limite de texto e de entrevistas | Casos para 30/31 caracteres, 250/251, 1 e 5 entrevistadores, 2ª e 3ª entrevista |
| E-mail vazio aceito pelo validador | Hipótese de defeito H1, verificada na API |
| Divergência UI × API × banco | Hipóteses H2 a H4 e casos de sistema para as mensagens da tela |
| Sem testes de sistema para Recruitment | Jornada completa pela interface com Playwright |
| Dependência de dados do servidor público | Cada teste cria a própria vaga e candidatos com nome único e remove tudo no final |
