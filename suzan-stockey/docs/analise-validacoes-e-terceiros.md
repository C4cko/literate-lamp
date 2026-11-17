# Validações da interface, API e material de terceiros

## Identificação e método

Análise para a [Issue #26](https://github.com/masmangan/literate-lamp/issues/26), realizada em 17/11/2026. Código original fixado na revisão `56e23b3b09e7af29317a0943523b825843fff527`. A leitura complementa a [análise dos testes originais](analise-testes-originais.md) e a [exploração manual de Suzan](exploracao-contatos-emergencia.md).

Foram examinados os formulários de criação e edição, utilitários de validação do cliente, endpoint da API e regras PHP efetivamente referenciadas. Para terceiros, foram lidas as planilhas de casos e resultados do projeto ezethufokazi/OrangeHRM-Testing na revisão `b34cb03920c4f3c1ecae4826472459b66b13730c`. As conclusões de código são estáticas; os resultados manuais próprios são os registrados na exploração de Suzan. Resultados declarados por terceiros não são apresentados como execuções desta contribuição.

## Interface: criação e edição

`SaveEmergencyContact.vue` e `EditEmergencyContact.vue` aplicam as mesmas regras de campos:

- Nome e parentesco: `required` e comprimento máximo 100.
- Telefones: formato permitido e comprimento máximo 30 em cada campo.
- Uma função associada a Home Telephone exige que pelo menos um entre homePhone, mobilePhone e officePhone seja diferente de string vazia. Ela considera os três campos, e não torna o telefone residencial obrigatório isoladamente.
- Os três telefones usam `v-model.trim`, removendo espaços externos do valor vinculado.

No utilitário `rules.ts`, `required` rejeita strings vazias e compostas apenas por espaços. O limite usa o comprimento de string JavaScript, relevante para futuras entradas Unicode; os testes numéricos ASCII de Suzan evitam ambiguidade de contagem.

`validPhoneNumberFormat` aceita vazio ou uma sequência de dígitos, sinais `+`, `-`, `/`, parênteses e espaço. A expressão não exige nenhum dígito. Portanto, a aceitação de sinais `+` observada por Suzan é coerente com a implementação do cliente; a regra valida caracteres permitidos, não a existência de um telefone utilizável.

Na criação, o envio chama `http.create`. Na edição, o componente carrega o contato com `http.get(id)` e salva com `http.update(id, ...)`. O cancelamento emite o evento de fechamento e não chama esses métodos de gravação. Isso fundamenta os casos de edição, consulta e cancelamento, mas a leitura do componente não substitui a verificação dos efeitos no sistema.

## API: regras efetivas

`EmpEmergencyContactAPI` usa `getCommonBodyValidationRules()` tanto na criação quanto na atualização. Essa função estabelece:

| Regra | Interface | API na revisão analisada |
|---|---|---|
| Nome e parentesco obrigatórios | `required` | `requiredParamRule` com tipo string e limite 100 |
| Limite nome/parentesco | 100 | 100 |
| Limite de cada telefone | 30 | 30 |
| Formato dos telefones | Expressão de caracteres permitidos | `Phone`, com o mesmo conjunto de caracteres |
| Vazio em um telefone individual | Aceito pelo formato | `Phone::validate` aceita string vazia e `NotBlankStringType` também aceita string vazia |
| Pelo menos um telefone | Função conjunta explícita | Nenhuma regra conjunta em `getCommonBodyValidationRules()` |
| Somente sinais `+` | Aceito pelo formato | Aceito pela regra `Phone`, sujeito às demais regras |
| Apenas espaços no telefone | Removidos por `.trim` | `NotBlankStringType` rejeita string composta apenas por espaços |
| Acesso ao funcionário | Interação visível depende do perfil | Regras de endpoint incluem `IN_ACCESSIBLE_EMP_NUMBERS` |

O nome `STRING_TYPE` mapeia para `NotBlankStringType`, não apenas para uma checagem genérica de string. Nome e parentesco vazios são rejeitados por `Required`, cujo comportamento padrão considera nulo e string vazia como ausentes; strings só com espaços são rejeitadas por `NotBlankStringType`.

Os limites usam `Rules::LENGTH`, mapeado para a biblioteca Respect Validation, enquanto a interface usa comprimento JavaScript. A equivalência observada para entradas ASCII não demonstra equivalência para todos os caracteres Unicode.

### Achado A01 — regra conjunta de telefone

A interface comunica e aplica a exigência de pelo menos um telefone. No endpoint lido, os três campos são validados separadamente e os validadores individuais permitem string vazia. Isso sustenta uma hipótese concreta de divergência: requisição com nome e parentesco válidos e os três telefones como strings vazias.

**Classificação:** achado de análise estática a verificar em teste de integração do endpoint. Não foi registrada resposta HTTP nem gravação obtida por essa requisição. O oráculo proposto é rejeitar cadastro/edição sem telefone, com fundamento na regra comunicada pela interface; a expectativa deve permanecer rastreável a essa regra.

### Achado A02 — telefone sem dígitos

Ambas as expressões de formato permitem sinais sem dígitos. A exploração manual confirmou aceitação e persistência pela interface. Essa consistência técnica não estabelece adequação ao objetivo de contato de emergência. O caso deve distinguir o comportamento atual de uma eventual recomendação de exigir dígito; não se atribui defeito confirmado apenas por preferência de formato.

## Material de terceiros

**Fonte:** [ezethufokazi/OrangeHRM-Testing](https://github.com/ezethufokazi/OrangeHRM-Testing/tree/b34cb03920c4f3c1ecae4826472459b66b13730c). A avaliação usou diretamente as planilhas, sem modificar seus arquivos. Na aba `My Info - Emergency Contacts`, linhas 2–5, foram encontrados:

| Caso | Ação | Avaliação crítica |
|---|---|---|
| TC_EC_001 | Adicionar contato com nome, parentesco e três telefones preenchidos | Fluxo positivo reproduzível e resultado esperado na tabela. Usar todos os telefones não verifica que apenas um basta |
| TC_EC_002 | Salvar com obrigatórios vazios | Exercita rejeição, mas não discrimina cada obrigatório nem especifica mensagens exatas ou combinações dos telefones |
| TC_EC_003 | Editar contato existente | Verifica apresentação do valor alterado. A instrução de atualizar qualquer campo é mais ampla que a massa, que fornece um telefone; não exige recarregar ou consultar em nova sessão |
| TC_EC_004 | Excluir e confirmar | Oráculo de ausência na tabela. Pode orientar limpeza, embora exclusão funcional não integre o escopo da jornada #25 |

Na planilha de resultados, os mesmos quatro IDs estão marcados como PASS. Esses registros são declarações do autor e não foram reexecutados nesta análise.

### Qualidade, técnicas e limites

- **Nível:** teste funcional manual de sistema pela interface; não há teste unitário ou integração de API nesses quatro casos.
- **Técnicas:** cenários de uso positivos e um cenário negativo de obrigatoriedade. Os quatro casos não explicitam tabela de decisão nem valores-limite.
- **Oráculo:** observa inclusão, atualização ou ausência na tabela, o que é mais significativo que verificar apenas um clique ou notificação. Mensagens exatas, contagem de registros e leitura após recarregar fortaleceriam a precisão.
- **Isolamento:** edição e exclusão exigem contato existente, sem identificar um registro exclusivo nem definir preparação e limpeza por caso. Isso permite dependência de dados compartilhados na demonstração.
- **Reprodutibilidade:** há passos e massa, mas o ambiente é a demo mutável. A revisão do material foi fixada nesta análise; não há versão/commit do sistema sob teste nos quatro casos.
- **Escopo:** o caminho My Info difere de PIM → Employee List da nossa jornada. As regras são comparáveis, mas contexto de acesso e permissões não devem ser presumidos equivalentes.
- **Lacunas para nossa jornada:** formatos inválidos, somente sinais, limites 29/30/31 e 99/100/101, combinações dos três telefones, cancelamento, preservação de outros registros e diferença entre UI/API.

### O que será aproveitado

A estrutura de ID, pré-condições, passos, massa e resultado esperado é uma boa referência documental. Os casos de cadastro e edição ajudam a reconhecer o fluxo básico. A contribuição própria acrescentará critérios mais precisos, dados exclusivos, verificação de persistência e cenários derivados das regras. Nenhum código, planilha ou resultado de terceiros foi incorporado como produção própria.

## Comparação consolidada

| Aspecto | Testes originais analisados | Material de terceiros | Direção para os testes próprios |
|---|---|---|---|
| Persistência | DAO e entidade exercitam ORM; serviço usa mock | Observação da tabela da interface | Atualizar e recuperar dados, inclusive em contexto independente |
| Validação | Arquivos de serviço/DAO/entidade não exercitam essas regras da jornada | Um caso de obrigatórios vazios | Partições, limites e tabela de decisão nas camadas adequadas |
| Formato | Regras do produto permitem sinais sem dígitos | Sem caso específico nos quatro cenários | Caracterizar comportamento e avaliar a regra de negócio |
| Consistência UI/API | Não demonstrada pelos três arquivos lidos | Só interface | Verificar a hipótese de telefone vazio diretamente na API |
| Permissões | Associação a funcionário é exercitada | Fluxo Admin em My Info | Separar vínculo de dados de autorização de acesso |

## Fontes fixadas

Original, revisão `56e23b3b09e7af29317a0943523b825843fff527`:

- [SaveEmergencyContact.vue](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/src/client/src/orangehrmPimPlugin/components/SaveEmergencyContact.vue).
- [EditEmergencyContact.vue](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/src/client/src/orangehrmPimPlugin/components/EditEmergencyContact.vue).
- [rules.ts: required, shouldNotExceedCharLength e validPhoneNumberFormat](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/src/client/src/core/util/validation/rules.ts).
- [EmpEmergencyContactAPI.php](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/src/plugins/orangehrmPimPlugin/Api/EmpEmergencyContactAPI.php).
- [Rules.php: mapeamento dos validadores](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/src/plugins/orangehrmCorePlugin/Api/V2/Validator/Rules.php).
- [Phone.php](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/src/plugins/orangehrmCorePlugin/Api/V2/Validator/Rules/Phone.php).
- [NotBlankStringType.php](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/src/plugins/orangehrmCorePlugin/Api/V2/Validator/Rules/NotBlankStringType.php).
- [Required.php](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/src/plugins/orangehrmCorePlugin/Api/V2/Validator/Rules/Required.php), [IsEmptyTrait.php](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/src/plugins/orangehrmCorePlugin/Api/V2/Validator/Helpers/IsEmptyTrait.php) e [ValidationDecorator.php](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/src/plugins/orangehrmCorePlugin/Api/V2/Validator/Helpers/ValidationDecorator.php).

Terceiros, revisão `b34cb03920c4f3c1ecae4826472459b66b13730c`:

- [README e descrição do ambiente](https://github.com/ezethufokazi/OrangeHRM-Testing/blob/b34cb03920c4f3c1ecae4826472459b66b13730c/README.md).
- [OrangeHRM_TestCases.xlsx](https://github.com/ezethufokazi/OrangeHRM-Testing/blob/b34cb03920c4f3c1ecae4826472459b66b13730c/OrangeHRM/OrangeHRM_TestCases.xlsx), aba `My Info - Emergency Contacts`, A2:F5.
- [OrangeHRM_TestExecutionResults.xlsx](https://github.com/ezethufokazi/OrangeHRM-Testing/blob/b34cb03920c4f3c1ecae4826472459b66b13730c/OrangeHRM/OrangeHRM_TestExecutionResults.xlsx), mesma aba, A2:H5.

## Uso de IA e autoria

ChatGPT/Codex (OpenAI) realizou a pesquisa, leitura dos validadores, extração das planilhas e redação desta comparação a pedido de Suzan Stockey Pereira. Os comportamentos observados manualmente são de autoria e execução de Suzan, conforme registro da exploração. Não se atribuem à autora a execução dos casos de terceiros nem resultados de chamadas à API que não foram realizadas.
