# Plano de Testes — Cadastro e atualização de contatos de emergência

**Autora da contribuição:** Suzan Stockey Pereira (@SuzanStockey). **Data:** 17/11/2026. **Planejamento:** [#27](https://github.com/masmangan/literate-lamp/issues/27). **Jornada:** [#25](https://github.com/masmangan/literate-lamp/issues/25). **Implementação:** [#28](https://github.com/masmangan/literate-lamp/issues/28). **Execução:** [#29](https://github.com/masmangan/literate-lamp/issues/29).

## 1. Introdução e objetivo

O OrangeHRM é um sistema de gestão de recursos humanos. Esta contribuição verifica se um responsável de RH consegue cadastrar, consultar e atualizar um contato de emergência de um funcionário, com validação dos dados e persistência correta. Código-fonte: [orangehrm/orangehrm](https://github.com/orangehrm/orangehrm), revisão de referência `56e23b3b09e7af29317a0943523b825843fff527` (OrangeHRM 5.9).

O plano deriva da análise crítica e da exploração manual da #26. Os 16 testes originais aprovados constituem a linha de base, não substituem os casos próprios deste plano. O documento especifica testes a implementar e executar; os resultados serão registrados na #29, separados dos resultados exploratórios já obtidos.

## 2. Jornada de usuário

**Persona:** Ana, responsável de RH, mantém informações de funcionários para permitir contato com uma pessoa indicada em situações de emergência.

**História:** Como responsável de RH, quero cadastrar e atualizar os contatos de emergência de um funcionário para manter disponíveis informações corretas para contato em situações urgentes.

**Pré-condições:** usuário autenticado com acesso ao funcionário; funcionário sintético existente; ambiente acessível. Login e cadastro do funcionário são preparação, não objetivos funcionais desta contribuição.

**Fluxo principal (UC-EC-01):**

1. Ana abre PIM → Employee List e seleciona o funcionário.
2. Acessa Emergency Contacts e escolhe Add.
3. Preenche nome, parentesco e pelo menos um telefone.
4. Salva e confere os dados na lista.
5. Recarrega a página e abre a edição do contato salvo.
6. Atualiza o telefone, salva e verifica o valor após nova consulta.

**Alternativas:** A1 — usar somente celular ou somente telefone comercial; A2 — usar mais de um telefone; A3 — cancelar criação ou edição sem gravar alterações.

**Exceções:** E1 — nome/parentesco ausente ou em branco; E2 — nenhum telefone; E3 — formato de telefone com caracteres proibidos; E4 — comprimento acima do limite; E5 — usuário sem acesso ao funcionário. Após uma rejeição, o contato anterior e os registros de outros funcionários devem permanecer preservados.

**Fora do escopo:** login, criação funcional de funcionário/usuário, anexos, outras abas do PIM, exclusão como jornada de negócio, carga, desempenho, auditoria ampla de segurança e correções no produto. Exclusão de registros sintéticos serve apenas à limpeza dos testes. Não serão criados novos serviços ou ambientes além do necessário para os três níveis exigidos.

## 3. Critérios de aceitação e oráculos

| ID | Dado / Quando / Então | Fundamento |
|---|---|---|
| CA01 | Dado funcionário acessível e dados válidos, quando cadastrar e consultar novamente, então existe exatamente um novo contato com campos e vínculo corretos | Objetivo da jornada e fluxo observado |
| CA02 | Dado contato existente, quando alterar telefone e consultar novamente, então a mesma identidade contém o novo valor, sem duplicar contato ou alterar outros registros | Objetivo de atualização e exploração |
| CA03 | Dado formulário aberto, quando cancelar criação ou edição, então a operação não altera o estado persistido | Comportamento de Cancel e exploração |
| CA04 | Dado nome ou parentesco vazio ou só com espaços, quando salvar, então a entrada é rejeitada e o estado persistido é preservado | `required`, `Required`, `NotBlankStringType` |
| CA05 | Dados telefones de formato/comprimento aceitos, quando salvar, então ao menos um deve estar preenchido; qualquer um dos três pode satisfazer a regra | Regra conjunta dos formulários; na API é expectativa de consistência a verificar |
| CA06 | Dado telefone preenchido, quando contiver letras ou símbolo não permitido, então rejeitar, mesmo com outro telefone válido | Expressões de formato e exploração |
| CA07 | Dados campos ASCII válidos, quando nome/parentesco tiver até 100 e telefone até 30 caracteres, então aceitar; quando ultrapassar, rejeitar | Limites do código e exploração |
| CA08 | Dado usuário sem acesso ao funcionário, quando tentar consultar/criar/atualizar seu contato, então negar a operação e preservar os dados | `IN_ACCESSIBLE_EMP_NUMBERS`; testar acesso, sem ampliar para auditoria de segurança |
| CC01 | Dado telefone `+++++++++`, quando avaliar o formato e salvar, então caracterizar a aceitação atual e conferir persistência | Código e exploração; não é regra proposta de qualidade de um telefone real |

CA05 na API é um oráculo de consistência derivado da interface, e não uma alegação de comportamento já confirmado. Se a API aceitar todos os telefones vazios, registrar esperado versus obtido e evidências na #29. CC01 não exige rejeição de sinais: exigir dígitos seria uma mudança de requisito sem fundamento formal nesta etapa.

## 4. Estratégia e ambiente

| Nível | Ferramenta e fronteira | Isolamento e verificação |
|---|---|---|
| Unitário (UT) | PHPUnit 9.6: validadores originais `Required`, `NotBlankStringType`, `Phone`, regra de comprimento e serviço original com DAO mockado | Sem banco na lógica exercitada; objetos e mocks próprios por caso. Instanciar regras originais, sem copiar regex ou regra de negócio para o teste |
| Integração (IT) | PHPUnit e utilitários do OrangeHRM: endpoint real, validação explícita da coleção de regras, serviço/DAO reais e MariaDB | Fixtures próprias; autenticação/permissões preparadas pelos utilitários do projeto; limpar EntityManager antes de conferir dados. Não classificar chamada direta de método como teste HTTP |
| Sistema (ST) | Playwright para navegar na interface do OrangeHRM 5.9; ações de cadastro/edição sempre pela UI | Preparação e limpeza de dados sintéticos; asserções por campos/mensagens/lista e após recarregar, sem esperas fixas arbitrárias |

A execução PHP utilizará o ambiente GitHub Actions já verificado na #26 (PHP 7.4, MariaDB 10.11, versão exata registrada a cada execução). Instruções atuais em [ambiente-local.md](https://github.com/masmangan/literate-lamp/blob/bc0552f/suzan-stockey/docs/ambiente-local.md). Testes de sistema podem usar a demo pública fornecida no enunciado; a versão exibida e data de cada execução serão registradas, sem presumir que a demo corresponde ao commit do clone. Não depende de instalação de Docker no computador da autora.

**Massa base M0:** nome `Contato Ficticio T2 <sufixo>`, parentesco `Irma`, homePhone `51999990000`, mobilePhone e officePhone vazios. O sufixo identifica execução e caso. Preparar dois funcionários sintéticos A/B e um contato de controle que não seja alvo da operação. Nenhum dado pessoal real. Na demonstração, alterar e limpar apenas registros próprios, identificados por seus IDs; não apagar registros existentes da turma.

**Variações:** `D(n)` é a sequência ASCII `1234567890` repetida e cortada para produzir exatamente n caracteres. Cada linha parametrizada mantém os demais campos válidos e registra a variante usada. Valores em branco: `''` e `'   '`. Telefone inválido: `abc` e `123@456`. Telefone de caracterização: `+++++++++`.

## 5. Tabela de decisão — presença dos telefones

Nome e parentesco válidos; cada telefone preenchido tem formato/comprimento aceitos. H = residencial, M = celular, W = comercial. Aplicar a tabela à criação e à atualização; o estado inicial de cada variante é preparado independentemente.

| Regra | H | M | W | Esperado segundo CA05 |
|---|---|---|---|---|
| D01 | vazio | vazio | vazio | Rejeitar |
| D02 | preenchido | vazio | vazio | Aceitar |
| D03 | vazio | preenchido | vazio | Aceitar |
| D04 | vazio | vazio | preenchido | Aceitar |
| D05 | preenchido | preenchido | vazio | Aceitar |
| D06 | preenchido | vazio | preenchido | Aceitar |
| D07 | vazio | preenchido | preenchido | Aceitar |
| D08 | preenchido | preenchido | preenchido | Aceitar |

Regra complementar D09: um telefone válido e outro inválido → rejeitar por formato. D01 na API investiga a divergência estática; não ajustar o esperado para ocultar eventual falha.

## 6. Casos unitários

Pré-condição comum: autoload das dependências originais disponível; sem persistência ou navegador. Cada variante recebe um sufixo rastreável no relatório.

| ID | Técnica / dados | Passos | Resultado esperado | Critério |
|---|---|---|---|---|
| UT01 | Equivalência: `Required` com nulo, vazio e texto; `NotBlankStringType` com vazio, espaços, texto e número | Instanciar cada validador original e chamar `validate` | Required rejeita nulo/vazio e aceita texto; tipo rejeita espaços/número e aceita vazio/texto. A combinação de regras, não tipo isolado, garante obrigatoriedade | CA04 |
| UT02 | Equivalência: Phone com vazio, número M0, `abc`, `123@456`, `+++++++++` | Avaliar cada entrada no validador original | Aceita vazio individual, número e sinais; rejeita letras e `@`. Vazio individual não comprova a regra conjunta | CA06, CC01 |
| UT03 | Valor-limite: regra original de comprimento parametrizada com D(99/100/101) e D(29/30/31) | Instanciar a classe usada por `Rules::LENGTH` com os mesmos limites do endpoint e validar | Aceita limite−1 e limite; rejeita limite+1 | CA07 |
| UT04 | Interação: serviço com contato M0 e DAO mockado | Esperar uma chamada `saveEmployeeEmergencyContact` com o contato; configurar retorno; chamar serviço | Delegação com argumento correto e retorno esperado; sem alegar persistência | CA01, CA02 |

## 7. Casos de integração

Pré-condição comum: banco exclusivo preparado, fixtures e contexto de usuário válidos. Usar o pipeline de validação antes de chamar a operação do endpoint; criar/atualizar diretamente sem validar não representa a fronteira planejada. Em rejeições, conferir ausência de inserção/alteração no banco. A leitura de conferência usa contexto ORM limpo.

| ID | Técnica / dados | Passos | Resultado esperado | Critério |
|---|---|---|---|---|
| IT01 | Caso de uso: M0, funcionário A e controle B | Validar e criar pelo endpoint; limpar ORM; consultar pelo ID/funcionário | Exatamente um novo contato com campos/vínculo corretos; controle B preservado | CA01 |
| IT02 | Transição criado → atualizado: contato existente de A | Atualizar telefone para `5133330000`; limpar ORM; consultar e contar | Mesmo ID, novo telefone, mesma quantidade; controle preservado | CA02 |
| IT03 | Equivalência: nome e parentesco, cada um vazio e só espaços | Executar criação e atualização com cada variante | Validação rejeita o campo; banco preservado | CA04 |
| IT04 | Tabela de decisão D01–D09 | Executar criação e atualização por variante; consultar estado após operação | D01/D09 rejeitados; demais aceitos; nenhum efeito parcial nas rejeições. Eventual aceitação de D01 é documentada como divergência | CA05, CA06 |
| IT05 | Valor-limite por campo: nome/parentesco D(99/100/101); cada telefone D(29/30/31) | Validar e criar com um campo variando; consultar quando aceito | Dentro/no limite aceito e persistido; acima rejeitado sem inserção | CA07 |
| IT06 | Autorização: usuário de A tentando acessar B | Validar consulta, criação e atualização com empNumber de B | Acesso negado pela validação/autorização; nenhum dado de B modificado ou retornado | CA08 |
| IT07 | Caracterização: telefone `+++++++++` | Validar/criar e recuperar em contexto limpo | Valor aceito e persistido conforme regra atual; registrar separadamente de defeitos | CC01 |

## 8. Casos de sistema

Pré-condição comum: sessão Admin, funcionário sintético acessível e M0; contexto e ambiente registrados. Recarregar e reabrir a edição sempre que a persistência for o oráculo. Nas rejeições, corrigir/cancelar e conferir que o estado anterior foi preservado.

| ID | Técnica / dados | Passos | Resultado esperado | Critério |
|---|---|---|---|---|
| ST01 | Caso de uso: M0 | Navegar pela jornada, adicionar, salvar, recarregar e editar | Um novo contato, com valores e funcionário corretos | CA01 |
| ST02 | Transição de estado: contato existente | Alterar telefone, salvar, recarregar, editar e contar | Novo telefone no mesmo contato, sem duplicação | CA02 |
| ST03 | Alternativas de caso de uso | Abrir Add com M0 e cancelar; separadamente editar nome para `NAO DEVE SALVAR` e cancelar; recarregar | Nenhuma criação no primeiro cenário; nome original mantido no segundo | CA03 |
| ST04 | Equivalência: nome/parentesco vazio e espaços | Tentar salvar variando cada obrigatório isoladamente | Mensagem `Required` no campo e nenhuma gravação | CA04 |
| ST05 | Tabela D01–D09 | Criar e editar por variante, com preparação independente | D01 exige pelo menos um telefone; D09 mostra erro de formato; demais persistem | CA05, CA06 |
| ST06 | Valor-limite: cinco campos, mesmas variantes de IT05 | Preencher, tentar salvar e conferir persistência dos aceitos | Limite aceito; excesso rejeitado com `Should not exceed 100 characters` ou `Should not exceed 30 characters` | CA07 |
| ST07 | Caracterização: `+++++++++` | Salvar telefone, recarregar e abrir edição | Mesmo valor preservado; caracterização, sem classificá-lo automaticamente como defeito | CC01 |

Mensagens são verificadas com a interface em inglês, como na exploração. Textos específicos da obrigatoriedade conjunta e de formato devem corresponder à tradução utilizada pela instância e à captura da execução. O teste de acesso sem permissão está alocado em IT06 para evitar ampliar o fluxo de sistema com outra jornada de autenticação.

Os IDs acima identificam famílias parametrizadas. O relatório contará execuções concretas por variante, separadas da quantidade de famílias; não apresentará 18 famílias como se fossem 18 execuções. A implementação manterá a identificação de campo, operação e variante em cada resultado.

## 9. Matriz de rastreabilidade

| Critério / fluxo | Unitário | Integração | Sistema |
|---|---|---|---|
| CA01 — cadastro e consulta / principal 1–5 | UT04 | IT01 | ST01 |
| CA02 — atualização / principal 6 | UT04 | IT02 | ST02 |
| CA03 — cancelar / A3 | — | Preservação verificada nos testes de rejeição, não como evento de Cancel | ST03 |
| CA04 — obrigatórios / E1 | UT01 | IT03 | ST04 |
| CA05 — presença dos telefones / A1, A2, E2 | Vazio individual em UT02; regra conjunta não é substituída por cópia no teste | IT04 | ST05 |
| CA06 — formato / E3 | UT02 | IT04 | ST05 |
| CA07 — comprimento / E4 | UT03 | IT05 | ST06 |
| CA08 — acesso / E5 | — | IT06 | — |
| CC01 — somente sinais | UT02 | IT07 | ST07 |

Cada regra fica na camada apropriada; não é necessário repetir todos os comportamentos nos três níveis. Em especial, um mock não será utilizado como evidência de persistência e um teste de DAO não será apresentado como teste da interface.

## 10. Execução, evidências e conclusão

**Entrada:** versão fixada, análise #26 disponível, dependências/banco configurados e dados próprios para a instância usada. Implementação na #28 deve informar comandos exatos de execução dos testes próprios e manter o escopo deste plano.

**Registro na #29:** ID e variante, data/ambiente, esperado, obtido, aprovado/falhou/bloqueado/não executado, duração quando disponível e link para log/JUnit/captura/trace pertinente. Falhas de ambiente são separadas de falhas do produto. O relatório consolidado deve resumir os três níveis e eventuais defeitos, com passos reproduzíveis e fontes.

**Saída:** todas as variantes planejadas possuem resultado explícito e evidência compatível; falhas investigadas; comandos e dados reproduzíveis; relatório do plano e da execução publicados nas respectivas Issues e arquivos versionados. Aprovação geral não será declarada quando houver divergências. Defeitos conhecidos serão preservados nos resultados e explicados, sem alterar o oráculo apenas para tornar a execução verde.

## 11. Fontes e uso de IA

- [Enunciado T2](https://moodle.pucrs.br/mod/page/view.php?id=3938865).
- [Análise dos testes originais e fontes fixadas](https://github.com/masmangan/literate-lamp/blob/bc0552f/suzan-stockey/docs/analise-testes-originais.md).
- [Validações UI/API e avaliação do material de terceiros](https://github.com/masmangan/literate-lamp/blob/bc0552f/suzan-stockey/docs/analise-validacoes-e-terceiros.md).
- [Exploração manual e evidências de Suzan](https://github.com/masmangan/literate-lamp/blob/bc0552f/suzan-stockey/docs/exploracao-contatos-emergencia.md).
- [Linha de base original: 16 testes e 56 asserções](https://github.com/masmangan/literate-lamp/blob/bc0552f/suzan-stockey/docs/execucao-testes-originais.md).

Plano estruturado e redigido com auxílio do ChatGPT/Codex (OpenAI), com base no escopo revisado por Suzan Stockey Pereira na #25, em sua exploração manual e nas análises da #26. A elaboração deste documento não implica que os novos casos já tenham sido implementados ou executados. A execução original no GitHub Actions é uma linha de base distinta.
