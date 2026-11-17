# Plano de testes – jornada "Recrutador contrata um candidato"

## 1. Introdução

**Sistema sob teste.** OrangeHRM Starter (open source), versão 5.x, sistema web de gestão de RH.
Back-end em PHP 8 (Symfony + Doctrine), front-end em Vue 3, API REST em `/web/index.php/api/v2`.

- Código-fonte: <https://github.com/orangehrm/orangehrm> (analisado no commit `56e23b3`).
- Servidor de demonstração: <https://opensource-demo.orangehrmlive.com> (usuário `Admin`, senha `admin123`).
- Descrição do produto: <https://www.orangehrm.com>.

**Escopo.** Módulo Recruitment: cadastro de candidato, etapas do processo seletivo e contratação, mais o
efeito da contratação no módulo PIM. Fora do escopo: candidatura pública (`/recruitmentApply`), anexos,
relatórios, outros papéis além de Admin.

**Linguagem e ferramentas.** Todos os testes estão em Java 21 com JUnit 5 (Jupiter), em um único projeto
Maven (`testes/`):

| Nível | Pacote | Ferramentas | Alvo |
|---|---|---|---|
| Unitário | `...modelo`, `...cliente`, `FormatoDataTest` e `TextoExatoTest` (sem tag) | JUnit 5 | modelo da especificação (máquina de estados e regras dos formulários) e cliente HTTP |
| Integração | `...integracao` (tag `integracao`) | JUnit 5 + `java.net.http.HttpClient` + Jackson | API REST v2 + serviços + banco |
| Sistema | `...sistema` (tag `sistema`) | JUnit 5 + Playwright for Java | aplicação completa pelo navegador |

**Sobre os testes unitários.** O OrangeHRM é escrito em PHP, então código Java não consegue chamar as
classes PHP diretamente. A camada unitária testa o **modelo executável da especificação** que construímos
(`FluxoRecrutamento`, `RegrasCandidato`, `RegrasEntrevista`) e o cliente HTTP. Esse modelo é depois usado
como **oráculo** pelos testes de integração e de sistema: o teste pergunta ao modelo qual deveria ser o
resultado e compara com o que o sistema real fez. Testar o oráculo isoladamente garante que um defeito
encontrado na integração é do sistema, e não do teste.

## 2. Estratégia e técnicas

| Técnica | Onde é aplicada |
|---|---|
| Transição de estados | tabela completa estados × ações (unitário); transições válidas e inválidas na API (integração); caminho principal na UI (sistema) |
| Partição de equivalência | e-mail, telefone, nomes, entrevistadores, horário |
| Análise de valor-limite | nomes 30/31, palavras-chave 250/251, título da entrevista 100/101, entrevistadores 0/1/5/6, número de entrevistas 1/2/3 |
| Tabela de decisão | campos obrigatórios do candidato (primeiro nome × sobrenome × e-mail) |
| Teste baseado em hipótese de defeito | H1 a H5, derivadas da leitura do código (seção 4) |

**Isolamento dos dados.** Cada teste de integração e de sistema cria a própria vaga e os próprios
candidatos com um sufixo único (`VV` + data/hora + número aleatório) e apaga tudo ao final
(`@AfterAll`). Assim os testes não dependem de dados do servidor nem uns dos outros.

**Ambiente.** Por padrão os testes apontam para o servidor de demonstração. Para usar uma instalação local,
basta definir a variável `ORANGEHRM_URL` (ver `testes/README.md`).

**Critério de aprovação.** Um caso passa quando o resultado observado é igual ao resultado esperado da
tabela. Casos de hipótese (H) que falham são registrados como defeitos no relatório de execução.

## 3. Casos de teste

### 3.1 Unitários (`mvn test`)

**Máquina de estados – `FluxoRecrutamentoTest`**

| ID | Técnica | Entrada | Resultado esperado |
|---|---|---|---|
| U01 | Transição de estados (válidas) | cada uma das 15 transições do diagrama | estado resultante igual ao da tabela `ohrm_workflow_state_machine` |
| U02 | Transição de estados (inválidas) | os 57 pares (estado, ação) fora do diagrama | ação não permitida (`Optional.empty`) |
| U03 | Transição de estados | estados finais *Hired* e *Rejected* | nenhuma ação permitida |
| U04 | Valor-limite | *Shortlisted* com 0, 1 e 2 entrevistas | "schedule" permitido com 0 e 1, proibido com 2 |
| U05 | Valor-limite | *Interview Passed* com 1 e 2 entrevistas | "schedule" permitido com 1, proibido com 2 |
| U06 | Caminho principal | sequência shortlist → schedule → pass → offer → hire | termina em *Hired* |
| U07 | Robustez | aplicar ação proibida | lança `TransicaoInvalidaException` com estado e ação na mensagem |

**Candidato – `RegrasCandidatoTest`**

| ID | Técnica | Entrada | Resultado esperado |
|---|---|---|---|
| U10 | Partição (válida) | candidato completo e válido | nenhum erro |
| U11 | Tabela de decisão | 8 combinações de primeiro nome / sobrenome / e-mail presentes ou ausentes | erro exatamente nos campos ausentes |
| U12 | Valor-limite | primeiro nome com 1, 29, 30 e 31 caracteres | 31 inválido, demais válidos |
| U13 | Valor-limite | sobrenome e nome do meio com 30 e 31 | 31 inválido |
| U14 | Partição e valor-limite | e-mail: `ana@empresa.com`, `ana.souza+rh@sub.empresa.com.br`, `a_b-c@x1.io` (válidos); `ana`, `ana@`, `@empresa.com`, `ana@empresa`, `ana empresa@x.com`, `ana@-x.com` (inválidos); 50 e 51 caracteres | conforme a classe; 51 inválido |
| U15 | Partição | nome só com espaços | inválido (obrigatório) |
| U16 | Partição e valor-limite | telefone: `51 3320-3500`, `+55 (51) 99999-0000`, `3320/3500` (válidos); `abc123`, `51#3320`, `ramal 12` (inválidos); vazio ou ausente (válido, campo opcional); 25 e 26 dígitos | conforme a classe; 26 inválido |
| U17 | Valor-limite | palavras-chave e comentário com 250 e 251 caracteres | 251 inválido |

**Entrevista – `RegrasEntrevistaTest`**

| ID | Técnica | Entrada | Resultado esperado |
|---|---|---|---|
| U20 | Valor-limite | 0, 1, 5 e 6 entrevistadores | 0 e 6 inválidos |
| U21 | Valor-limite | título com 100 e 101 caracteres; título vazio | 101 e vazio inválidos |
| U22 | Partição | horário `09:30`, `23:59` (válidos); `9h30`, `24:00`, `12:60` (inválidos); ausente (válido) | conforme a classe |
| U23 | Partição | data ausente | inválido |

**Cliente – `TokenCsrfTest`, `AcaoRecrutamentoTest`**

| ID | Técnica | Entrada | Resultado esperado |
|---|---|---|---|
| U30 | Partição | HTML da página de login com o atributo `:token` | token extraído sem as entidades `&quot;` |
| U31 | Partição | HTML sem token | `IllegalStateException` |
| U32 | Tabela | cada ação com id de candidato 7 e entrevista 3 | caminho e método HTTP corretos (`PUT /recruitment/candidates/7/shortlist`...) |
| U33 | Partição | formatos de data `yyyy-dd-mm`, `yyyy-mm-dd`, `dd-mm-yyyy`, `mm/dd/yyyy` e vazio (`FormatoDataTest`) | data formatada como o campo da tela espera |
| U34 | Partição | rótulos da tela com e sem caracteres especiais (`TextoExatoTest`); incluído após a 1ª execução, ver relatório de execução | expressão sem `\Q...\E`, casa só o texto exato |

### 3.2 Integração – API REST (`mvn test -Pintegracao`)

Pré-condição comum: sessão de Admin; vaga ativa criada pelo teste com o primeiro cargo (job title) e o
primeiro funcionário do servidor como gerente de contratação.

| ID | Técnica | Passos | Resultado esperado |
|---|---|---|---|
| I01 | Partição (válida) | `POST /recruitment/candidates` com dados válidos e vaga | 200; `GET` do candidato devolve os mesmos dados e status id 1 (*Application Initiated*) |
| I02 | Valor-limite | primeiro nome com 30 caracteres | 200 |
| I03 | Partição / valor-limite (inválidas) | sem primeiro nome; sem sobrenome; e-mail `ana@`; primeiro nome com 31; telefone `abc` | 422 em todos |
| I04 | Transição de estados (caminho principal) | shortlist → schedule → pass → offer → hire | 200 em cada passo; status e ações permitidas (`GET .../actions/allowed`) iguais ao modelo após cada passo |
| I05 | Efeito colateral | após I04, `GET /pim/employees?nameOrId=<sobrenome>` | 1 funcionário com mesmo primeiro nome e sobrenome |
| I06 | Histórico | após I04, `GET /recruitment/candidates/{id}/history` | todas as ações da jornada registradas, incluindo *Hired* |
| I07 | Transição de estados (inválidas) | candidatos em *Application Initiated*, *Shortlisted*, *Interview Failed*, *Hired* e *Rejected*; tentar cada ação proibida pelo modelo | 403 e status inalterado |
| I08 | Valor-limite | 1ª e 2ª entrevista; 3ª entrevista | 200, 200, 400; "Schedule Interview" some das ações permitidas |
| I09 | Valor-limite | entrevista com 0 e 6 entrevistadores | 422 |
| I10 | Valor-limite | entrevista com 5 entrevistadores | 200 |
| I11 | Caminho alternativo | vaga fechada (`status: false`) e shortlist | 403; lista de ações permitidas vazia |
| I12 | Caminho alternativo | offer → decline → reject | status *Offer Declined* e depois *Rejected* |

### 3.3 Sistema – interface (`mvn test -Psistema`)

Pré-condição comum: vaga ativa criada pela API; navegador Chromium headless (Playwright).

| ID | Técnica | Passos | Resultado esperado |
|---|---|---|---|
| S01 | Caso de uso (caminho principal) | login → Add Candidate com vaga → Shortlist → Schedule Interview → Mark Interview Passed → Offer Job → Hire | após cada etapa a tela mostra o status esperado; ao final *Status: Hired* e o funcionário existe no PIM |
| S02 | Tabela de decisão | salvar formulário vazio | mensagem *Required* em First Name, Last Name e Email |
| S03 | Partição | e-mail `ana@` | *Expected format: admin@example.com* |
| S04 | Valor-limite | primeiro nome com 31 caracteres; depois com 30 | *Should not exceed 30 characters* só para 31 |
| S05 | Partição | telefone `abc` | *Allows numbers and only + - / ( )* |
| S06 | Transição de estados | Reject a partir de *Application Initiated* | *Status: Rejected* e nenhum botão de ação de avanço |

## 4. Hipóteses de defeito (testes de integração com tag `hipotese`)

Encontradas lendo o código; cada uma vira um teste que espera o comportamento **especificado**. Se o teste
falhar, a hipótese se confirma e o defeito vai para o relatório.

| ID | Hipótese | Evidência no código | Esperado (especificação) |
|---|---|---|---|
| H1 | A API aceita e-mail vazio `""` | `Rules/Email.php` retorna `true` para string vazia; OpenAPI marca `email` como obrigatório | 422 |
| H2 | A API aceita primeiro nome vazio ou só com espaços | regra `LENGTH [null, 30]` sem mínimo em `CandidateAPI` | 422 |
| H3 | Telefone com 31+ caracteres causa erro interno | API sem limite; coluna `contact_number` tem 30; tela limita a 25 | 422 (e não 500) |
| H4 | E-mail com 101+ caracteres causa erro interno | API sem limite; coluna `email` tem 100; tela limita a 50 | 422 (e não 500) |
| H5 | É possível aprovar a entrevista de outro candidato | regra `ENTITY_ID_EXISTS` só confere se a entrevista existe, não de quem é | 4xx |

## 5. Rastreabilidade

| Critério de aceitação | Casos |
|---|---|
| CA1 | U10–U17, I01–I03, S02–S05, H1–H4 |
| CA2 | I01, S01 |
| CA3 | U01–U03, U06–U07, I04, I07, I12, S06 |
| CA4 | U04–U05, U20–U23, U33, I08–I10, H5 |
| CA5 | I05, S01 |
| CA6 | I11 |
| CA7 | I06 |

## 6. Riscos

- O servidor de demonstração é compartilhado e reiniciado com frequência; pode ficar fora do ar ou lento.
  Mitigação: tempo limite maior nos testes de sistema e opção de ambiente local (`ORANGEHRM_URL`).
- Outros usuários podem alterar dados do servidor. Mitigação: dados próprios com sufixo único.
- O formato de data da tela depende da configuração do servidor. Mitigação: o teste de sistema lê o
  `placeholder` do campo de data e formata a data de acordo.
