# Relatório da execução dos testes

Resultados reais da execução com Maven na máquina do autor. As saídas completas estão em
[`resultados/`](../resultados/).

## 1. Ambiente da execução

| Item | Valor |
|---|---|
| Data | 17/11/2026, entre 15h30 e 15h50 (horário de Brasília) |
| Servidor | servidor de demonstração `https://opensource-demo.orangehrmlive.com` (usuário Admin) |
| JDK | Eclipse Temurin 21 |
| Maven | 3.10.1 |
| Sistema operacional | Windows 11 |
| Comandos | `mvn -B test`, `mvn -B test -Pintegracao`, `mvn -B test -Psistema` |

## 2. Resumo

Os números abaixo são **execuções** do JUnit: um teste parametrizado roda uma vez por linha de dados.
Por isso há mais execuções do que casos de teste no plano (U01–U34, I01–I12, S01–S06, H1–H5).

| Nível | Execução | Execuções | Passaram | Falharam | Saída |
|---|---|---|---|---|---|
| Unitário | 1ª | 184 | 184 | 0 | `resultados/execucao-1/unitario-saida.txt` |
| Integração | 1ª | 51 | 46 | 5 | `resultados/execucao-1/integracao-saida.txt` |
| Sistema | 1ª | 6 | 0 | 6 | `resultados/execucao-1/sistema-saida.txt` |
| Sistema | 2ª (após correção) | 6 | 6 | 0 | `resultados/execucao-2/sistema-saida.txt` |

- As **5 falhas de integração** são as hipóteses de defeito H1 a H5 e indicam **defeitos do OrangeHRM** (seção 5).
- As **6 falhas de sistema da 1ª execução** foram um **defeito do próprio código de teste** (seção 4),
  corrigido no commit `b1a353b`, Issue #37. Na 2ª execução todos passaram.

## 3. Resultado por caso

### 3.1 Unitários – 184 execuções, todas passaram

| Classe | Casos | Execuções | Resultado |
|---|---|---|---|
| `FluxoRecrutamentoTest` | U01–U07 | 101 | passou |
| `RegrasCandidatoTest` | U10–U17 | 42 | passou |
| `RegrasEntrevistaTest` | U20–U23 | 20 | passou |
| `TokenCsrfTest` | U30–U31 | 6 | passou |
| `AcaoRecrutamentoTest` | U32 | 10 | passou |
| `FormatoDataTest` | U33 | 5 | passou |

O teste U34 (`TextoExatoTest`, 5 execuções) foi criado depois da 1ª execução, junto com a correção da
seção 4. Ele roda com `mvn test`, mas ainda não fazia parte da execução registrada aqui.

### 3.2 Integração – 51 execuções, 46 passaram

| Caso | Execuções | Resultado | Observação |
|---|---|---|---|
| I01 cadastro válido | 1 | passou | dados salvos e status *Application Initiated* |
| I02 primeiro nome com 30 | 1 | passou | |
| I03 dados inválidos → 422 | 5 | passou | sem nome, sem sobrenome, `ana@`, nome com 31, telefone `abc` |
| I04 jornada completa | 1 | passou | status e ações permitidas iguais ao modelo em todos os 5 passos |
| I05 funcionário criado no PIM | 1 | passou | |
| I06 histórico | 1 | passou | |
| I07 ações proibidas → 403 | 29 | passou | 29 pares (estado, ação) recusados e status inalterado |
| I08 limite de 2 entrevistas | 1 | passou | 3ª entrevista recusada com 400 |
| I09 0 ou 6 entrevistadores → 422 | 2 | passou | |
| I10 5 entrevistadores | 1 | passou | |
| I11 vaga fechada | 1 | passou | nenhuma ação oferecida; shortlist → 403 |
| I12 oferta recusada e rejeição | 1 | passou | |
| H1–H5 hipóteses | 6 | **5 falharam** | ver seção 5 |

### 3.3 Sistema – 2ª execução, 6 de 6 passaram

| Caso | Resultado |
|---|---|
| S01 jornada completa pela interface até *Hired* + funcionário no PIM | passou |
| S02 formulário vazio → *Required* nos 3 campos | passou |
| S03 e-mail inválido → *Expected format: admin@example.com* | passou |
| S04 primeiro nome com 31 / 30 caracteres | passou |
| S05 telefone com letras → mensagem de formato | passou |
| S06 rejeição encerra o processo | passou |

## 4. Defeito no código de teste (corrigido)

Na 1ª execução, todos os testes de sistema falharam por esgotar o tempo (*timeout*) ao procurar os campos
*Email*, *Contact Number* e *Vacancy*.

- **Causa.** A busca do campo pelo rótulo usava `Pattern.quote`, que gera a sintaxe `\Q...\E`. Ela existe nas
  expressões regulares do Java, mas não nas do JavaScript. Como o Playwright executa a expressão dentro do
  navegador, nenhum rótulo era encontrado.
- **Evidência de que só a busca por rótulo falhava.** No S02, a contagem de mensagens *Required*, que não
  depende de rótulo, passou e encontrou as 3 esperadas. Só a etapa seguinte, que procura o grupo *Email*,
  falhou.
- **Correção.** Escape manual compatível com as duas linguagens (`Tela.textoExato`), mais o teste unitário
  U34 para o erro não voltar.

Lição: um teste que falha nem sempre aponta um defeito do sistema. Antes de registrar um defeito, é preciso
descartar defeito no próprio teste.

## 5. Defeitos encontrados no OrangeHRM

As 5 hipóteses tiradas da leitura do código se confirmaram, embora com resultados um pouco diferentes do
previsto em H1 e H2.

### D1 (H5) – É possível aprovar a entrevista de outro candidato

| | |
|---|---|
| Gravidade | **Alta**: quebra a integridade do processo seletivo e do histórico |
| Passos | Deixar os candidatos A (id 92) e B (id 93) em *Interview Scheduled*. Chamar `PUT /api/v2/recruitment/candidates/92/interviews/13/pass`, onde a entrevista 13 é do candidato B. |
| Esperado | Erro 4xx: a entrevista não pertence ao candidato |
| Obtido | **HTTP 200**. A ação é registrada no histórico do candidato A apontando para a entrevista de B |
| Causa provável | `CandidateMarkingInterviewPassAPI` valida o `interviewId` só com `Rules::ENTITY_ID_EXISTS`, que confere se a entrevista existe, mas não de quem ela é |

### D2 (H1) – E-mail vazio derruba a API com erro interno

| | |
|---|---|
| Gravidade | **Média**: erro não tratado em vez de mensagem de validação |
| Passos | `POST /api/v2/recruitment/candidates` com `"email": ""` e os outros campos válidos |
| Esperado | HTTP 422 (e-mail é obrigatório no contrato OpenAPI e na tela) |
| Obtido | **HTTP 500** `{"error":{"status":"500","message":"Unexpected Error Occurred"}}` |
| Causa provável | `Rules/Email.php` aceita string vazia como válida. O erro acontece depois, na gravação |

A hipótese previa que o cadastro seria aceito. Na prática o servidor quebra, o que é mais grave.

### D3 (H2) – Primeiro nome vazio é aceito

| | |
|---|---|
| Gravidade | **Média**: candidato salvo sem nome |
| Passos | `POST /api/v2/recruitment/candidates` com `"firstName": ""` |
| Esperado | HTTP 422 |
| Obtido | **HTTP 200**, candidato salvo com `firstName` vazio (id 90) |
| Observação | Com o nome só com espaços (`"   "`), a API recusou com 422, como esperado |
| Causa provável | Regra `LENGTH [null, 30]` sem tamanho mínimo em `CandidateAPI` |

### D4 (H3) – Telefone acima do limite é aceito

| | |
|---|---|
| Gravidade | **Baixa** |
| Passos | Cadastro com `contactNumber` de 31 dígitos |
| Esperado | HTTP 422 (a tela limita a 25; a coluna do banco tem 30) |
| Obtido | **HTTP 200** (id 91) |

### D5 (H4) – E-mail acima do limite é aceito

| | |
|---|---|
| Gravidade | **Baixa** |
| Passos | Cadastro com e-mail de 101 caracteres |
| Esperado | HTTP 422 (a tela limita a 50; a coluna do banco tem 100) |
| Obtido | **HTTP 200** (id 89) |

D4 e D5 mostram regras diferentes em cada camada: a tela, a API e o banco não concordam sobre o tamanho
máximo. Não foi verificado se o banco cortou o valor salvo.

Os defeitos D2 a D5 acontecem só pela API. A tela impede esses dados com as próprias validações, como
mostram S02 a S05. Mas qualquer cliente da API REST (integrações, scripts) consegue gravá-los.

## 6. Conclusão

- **A jornada principal funciona.** Cadastro, as transições do caminho principal e do alternativo, a recusa das 29 transições proibidas
  testadas, o limite de entrevistas, a vaga fechada e a criação do funcionário na contratação se comportaram
  como especificado, pela API e pela interface.
- **A validação da API é mais fraca que a da tela.** Foram encontrados 5 defeitos. O mais grave (D1) permite
  misturar entrevistas entre candidatos.
- **Os testes originais do OrangeHRM não cobrem nenhum desses defeitos.** Isso confirma as lacunas da análise
  crítica: faltam valores-limite, partições de e-mail e verificação de a quem pertence cada entidade.
- **Limitações.**
  - Os testes rodaram só no servidor de demonstração, que é compartilhado.
  - Só o papel Admin foi testado.
  - Os testes unitários testam o modelo da especificação, não o código PHP.
