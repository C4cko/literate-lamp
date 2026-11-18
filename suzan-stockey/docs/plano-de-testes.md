# Plano de Testes — Contatos de emergência

**Autora:** Suzan Stockey Pereira (@SuzanStockey). **Data:** 17/11/2026.
**Jornada:** [#25](https://github.com/masmangan/literate-lamp/issues/25). **Plano:** [#27](https://github.com/masmangan/literate-lamp/issues/27). **Implementação:** [#28](https://github.com/masmangan/literate-lamp/issues/28). **Execução:** [#29](https://github.com/masmangan/literate-lamp/issues/29).

## 1. Objetivo e escopo

Verificar a jornada de cadastro e atualização de um contato de emergência de um funcionário no OrangeHRM, com testes unitários, de integração e de sistema, conforme os níveis solicitados no T2. O plano utiliza a análise e a exploração da #26. Os testes próprios serão implementados na #28 e seus resultados registrados na #29; os 16 testes originais executados são uma linha de base distinta.

Código de referência: [OrangeHRM](https://github.com/orangehrm/orangehrm/tree/56e23b3b09e7af29317a0943523b825843fff527), revisão `56e23b3b09e7af29317a0943523b825843fff527`.

**Incluído:** cadastro, consulta para conferência, atualização, cancelamento e validações dos campos dessa jornada.

**Fora do escopo:** testes de login, cadastro de funcionário, exclusão como jornada, anexos, outras abas, permissões de outros usuários, desempenho e alterações no produto. Login e funcionário sintético são somente pré-condições. Este plano não exige criação de workflow, Docker, serviços ou infraestrutura compartilhada.

## 2. Jornada

**Persona:** responsável de RH que mantém o contato de emergência de um funcionário.

**História:** como responsável de RH, quero cadastrar e atualizar um contato de emergência para manter os dados disponíveis em situações urgentes.

**Pré-condições:** sessão autenticada, funcionário sintético acessível e instância disponível.

1. Acessar PIM → Employee List e selecionar o funcionário de teste.
2. Abrir Emergency Contacts → Add.
3. Informar nome, parentesco e pelo menos um telefone; salvar.
4. Conferir os valores na lista e após recarregar.
5. Editar o contato, alterar o telefone e salvar.
6. Recarregar e confirmar o novo valor no mesmo contato.

**Alternativas:** usar residencial, celular ou comercial; cancelar criação ou edição.
**Exceções:** obrigatório vazio, nenhum telefone, formato inválido e comprimento excedido.

## 3. Critérios e resultados esperados

| ID | Critério | Fonte do esperado |
|---|---|---|
| CA01 | Cadastrar um contato com os valores informados e associado ao funcionário escolhido; valores mantidos após nova consulta | Jornada e exploração |
| CA02 | Atualizar o telefone do contato existente sem criar outro contato | Jornada e exploração |
| CA03 | Cancelar criação ou edição sem gravar a alteração | Ação Cancel da interface |
| CA04 | Rejeitar nome ou parentesco vazio ou somente com espaços | Regras de obrigatoriedade analisadas na #26 |
| CA05 | Na interface, exigir pelo menos um dos três telefones | Validação observada na exploração |
| CA06 | Rejeitar telefone com letras ou `@`; caracterizar a aceitação de `+++++++++` sem presumir defeito | Validador original e exploração |
| CA07 | Aceitar nome/parentesco com 99 e 100 caracteres e rejeitar 101; aceitar telefones com 29 e 30 e rejeitar 31 | Código e valores-limite explorados |

A regra conjunta de telefones da interface não será tratada como requisito confirmado da API. Este plano não acrescenta cenários de autorização nem obriga a testar toda a API.

## 4. Abordagem, ferramentas e dados

| Nível | Abordagem | Evidência |
|---|---|---|
| Unitário | PHPUnit e validadores originais; serviço com DAO mockado | Saída dos casos e asserções; mock não comprova persistência |
| Integração | PHPUnit, serviço e DAO reais com banco de testes, utilizando o suporte existente do OrangeHRM | Gravação e leitura dos dados em nova consulta, sem mock do DAO |
| Sistema | Jornada pela interface da instância indicada no enunciado, com Playwright | Resultados e capturas pertinentes; conferência após recarregar |

Usar os recursos de teste existentes do OrangeHRM. A #28 registrará as dependências e os comandos efetivamente usados, sem criar infraestrutura adicional como entregável. A [descrição do ambiente da #26](https://github.com/masmangan/literate-lamp/blob/faff914/suzan-stockey/docs/ambiente-local.md) documenta a execução original; não estabelece uma nova automação obrigatória para este plano.

Massa sintética: nome `Contato Ficticio T2`, parentesco `Irma`, telefone `51999990000`. Identificar os registros próprios de cada caso e alterar ou limpar somente esses registros. Para limites, usar a sequência `1234567890` repetida e cortada no comprimento indicado, mantendo os demais campos válidos. Registrar a versão exibida pela instância e a data, sem presumir igualdade entre a demo e a revisão do código.

## 5. Casos unitários

| ID | Dados e execução | Esperado | Critério |
|---|---|---|---|
| UT01 | Avaliar `Required` e `NotBlankStringType` originais com vazio, espaços e texto | A combinação das regras rejeita obrigatório vazio/espaços e aceita texto | CA04 |
| UT02 | Avaliar `Phone` original com número, `abc`, `123@456` e `+++++++++` | Número e sinais aceitos pela regra atual; letras e `@` rejeitados | CA06 |
| UT03 | Avaliar a regra original de comprimento com 99/100/101 para nome/parentesco e 29/30/31 para telefone | Limite−1 e limite aceitos; limite+1 rejeitado | CA07 |
| UT04 | Chamar o serviço com contato sintético e DAO mockado | Argumento delegado e retorno conferidos; sem alegar persistência | CA01, CA02 |

## 6. Casos de integração

Pré-condição: banco de testes e dados sintéticos preparados com os utilitários existentes do projeto. Conferir a leitura sem depender apenas do objeto que acabou de ser salvo em memória.

| ID | Passos | Esperado | Critério |
|---|---|---|---|
| IT01 | Salvar um contato por serviço/DAO reais e consultar pelo funcionário e identificador | Campos e vínculo corretos no banco | CA01 |
| IT02 | Preparar contato, alterar telefone, salvar e consultar novamente | Mesmo identificador, novo telefone e nenhuma duplicação | CA02 |

Esses casos verificam a integração com persistência; não serão apresentados como testes HTTP ou validações da interface.

## 7. Casos de sistema

Pré-condição: sessão e funcionário sintético preparados. Cada variante começa com dados válidos e independentes. Em rejeições, conferir que não houve gravação da alteração.

| ID | Passos e dados | Esperado | Critério |
|---|---|---|---|
| ST01 | Executar o fluxo principal: cadastrar, recarregar, editar telefone e recarregar | Contato criado e atualização mantida, sem duplicação | CA01, CA02 |
| ST02 | Preencher Add e cancelar; em caso separado alterar contato e cancelar | Nenhuma criação ou alteração gravada | CA03 |
| ST03 | Variar nome e parentesco, um por vez, com vazio e espaços | Obrigatoriedade indicada; gravação rejeitada | CA04 |
| ST04 | Tentar salvar sem telefones; depois usar somente residencial, somente celular e somente comercial | Sem telefone rejeitado; cada telefone isolado permite salvar | CA05 |
| ST05 | Variar telefone com `abc`, `123@456` e `+++++++++` | Letras e `@` rejeitados; sinais aceitos e mantidos conforme comportamento observado | CA06 |
| ST06 | Variar nome/parentesco com 99/100/101 e cada telefone com 29/30/31 caracteres | Limites aceitos e persistidos; excesso rejeitado com mensagem de comprimento | CA07 |

As linhas com vários dados são casos parametrizados. O relatório identificará campo e valor de cada execução. Usar classes de equivalência para vazios/formato, valores-limite para comprimento e o cenário da jornada para cadastro/atualização.

Tabela de decisão de ST04 (nome e parentesco válidos):

| Variante | Residencial | Celular | Comercial | Esperado na interface |
|---|---|---|---|---|
| Nenhum | Vazio | Vazio | Vazio | Rejeitar |
| Residencial | Válido | Vazio | Vazio | Aceitar |
| Celular | Vazio | Válido | Vazio | Aceitar |
| Comercial | Vazio | Vazio | Válido | Aceitar |

## 8. Rastreabilidade

| Critério | Unitário | Integração | Sistema |
|---|---|---|---|
| CA01 — cadastro | UT04 | IT01 | ST01 |
| CA02 — atualização | UT04 | IT02 | ST01 |
| CA03 — cancelar | — | — | ST02 |
| CA04 — obrigatórios | UT01 | — | ST03 |
| CA05 — pelo menos um telefone | — | — | ST04 |
| CA06 — formato e caracterização | UT02 | — | ST05 |
| CA07 — comprimento | UT03 | — | ST06 |

## 9. Registro e conclusão

Na #29, registrar ID/variante, ambiente/data, dados, esperado, obtido, situação e link para log ou captura pertinente. Relatar separadamente os resultados originais e os testes próprios. Diferenciar falha do produto de problema do ambiente e registrar os casos não executados com seu motivo.

Concluir a atividade com os casos implementados e seus resultados documentados, análise das divergências encontradas e links dos arquivos/commits/PRs nas Issues do plano e da execução. Não declarar aprovação de casos que não foram executados nem modificar o esperado apenas para obter aprovação.

## 10. Fontes e uso de IA

- [Enunciado T2](https://moodle.pucrs.br/mod/page/view.php?id=3938865).
- [Análise dos testes originais](https://github.com/masmangan/literate-lamp/blob/faff914/suzan-stockey/docs/analise-testes-originais.md).
- [Validações e material de terceiros](https://github.com/masmangan/literate-lamp/blob/faff914/suzan-stockey/docs/analise-validacoes-e-terceiros.md).
- [Exploração e evidências](https://github.com/masmangan/literate-lamp/blob/faff914/suzan-stockey/docs/exploracao-contatos-emergencia.md).
- [Execução dos testes originais](https://github.com/masmangan/literate-lamp/blob/faff914/suzan-stockey/docs/execucao-testes-originais.md).

Plano redigido e revisado com auxílio do ChatGPT/Codex (OpenAI), a pedido de Suzan Stockey Pereira, com base no escopo que ela revisou na #25 e em sua exploração manual. Esta revisão simplifica o planejamento; não representa execução dos novos casos.
