# Exploração manual — Contatos de emergência

## Identificação

- **Atividade:** T2 — Verificação e Validação de Software.
- **Executora:** Suzan Stockey Pereira (@SuzanStockey).
- **Data:** 17/11/2026.
- **Sistema:** OrangeHRM OS 5.9, conforme versão exibida na interface.
- **Ambiente:** [demonstração pública do OrangeHRM](https://opensource-demo.orangehrmlive.com/).
- **Perfil:** Admin.
- **Funcionário sintético:** Teste TesteT2, conforme captura da página.
- **Identificador na URL:** empNumber/400.
- **Caminho:** PIM → Employee List → funcionário → Emergency Contacts.
- **Rastreabilidade:** [Issue principal #25](https://github.com/masmangan/literate-lamp/issues/25) e [análise e ambiente #26](https://github.com/masmangan/literate-lamp/issues/26).

## Objetivo e procedimento

Explorar o cadastro e a edição de contatos de emergência, observando obrigatoriedade, formatos de telefone, limites de comprimento, persistência e cancelamento. Suzan executou os passos manualmente na interface e relatou os resultados durante a atividade. As capturas anexadas documentam estados específicos dessa execução.

Foi utilizado um contato fictício com nome `Contato Ficticio T2`, parentesco `Irma` e telefone de referência `51999990000`. Os valores foram alterados isoladamente para observar cada regra. Nos casos aceitos de comprimento e atualização, a executora recarregou a página e conferiu a persistência. Ao final, os dados de referência foram restaurados.

## Obrigatoriedade e formato

| ID | Condição ou entrada | Resultado observado | Registro |
|---|---|---|---|
| EX-01 | Todos os campos vazios | Interface exige nome e parentesco | Relato da executora |
| EX-02 | Nome e parentesco preenchidos; três telefones vazios | Interface exige pelo menos um telefone | Relato da executora |
| EX-03 | Telefone composto apenas por sinais `+` | Aceito, exibido na lista e recuperado na edição após recarregar | E01, E02 e relato da executora |
| EX-04 | Home Telephone = `abc` | Rejeitado; mensagem `Allows numbers and only + - / ( )` | E04 |
| EX-05 | Atualizar Home Telephone para `51999990000` | Valor persistiu após recarregar; permaneceu um único contato | E03 e relato da executora |
| EX-06 | Alterar nome para `NAO DEVE SALVAR` e cancelar | Nome original preservado, inclusive após recarregar | Relato da executora sobre a sequência de edição e cancelamento |

## Combinações de telefones

Nome e parentesco permaneceram válidos. A hipótese usada para comparar os resultados foi: pelo menos um telefone deve estar preenchido e os campos preenchidos devem ter formato aceito.

| Caso | Home Telephone | Mobile | Work Telephone | Resultado esperado na exploração | Resultado observado |
|---|---|---|---|---|---|
| T01 | Vazio | Vazio | Vazio | Rejeitar | Rejeitado |
| T02 | `51999990000` | Vazio | Vazio | Aceitar | Aceito |
| T03 | Vazio | `51999990000` | Vazio | Aceitar | Aceito |
| T04 | Vazio | Vazio | `5133330000` | Aceitar | Aceito |
| T05 | `51999990000` | `abc` | Vazio | Rejeitar | Rejeitado |

**Fonte dos cinco resultados:** relato da executora. As combinações verificadas mostram que cada telefone pode atender isoladamente à obrigatoriedade, e que um telefone com letras impede salvar mesmo quando outro contém um número aceito. A tabela representa as combinações efetivamente executadas.

## Valores-limite

A técnica aplicada foi análise de valor-limite. Em Home Telephone, Name e Relationship foram exercitados limite menos um, limite e limite mais um. Em Mobile e Work Telephone foram exercitados o limite e o valor imediatamente superior. Os demais campos foram mantidos válidos, e os outros telefones ficaram vazios durante a avaliação isolada de cada telefone.

| Campo | Comprimento | Resultado observado | Persistência / mensagem | Registro |
|---|---:|---|---|---|
| Home Telephone | 29 | Aceito | Persistiu após recarregar | Relato da executora |
| Home Telephone | 30 | Aceito | Persistiu após recarregar | Relato da executora |
| Home Telephone | 31 | Rejeitado | `Should not exceed 30 characters` | E05 e relato da executora |
| Mobile | 30 | Aceito | Aceitação informada pela executora | Relato da executora |
| Mobile | 31 | Rejeitado | `Should not exceed 30 characters` | Relato da executora |
| Work Telephone | 30 | Aceito | Aceitação informada pela executora | Relato da executora |
| Work Telephone | 31 | Rejeitado | `Should not exceed 30 characters` | Relato da executora |
| Name | 99 | Aceito | Persistiu após recarregar | Relato da executora |
| Name | 100 | Aceito | Persistiu após recarregar | Relato da executora; E06 ilustra nome longo salvo |
| Name | 101 | Rejeitado | `Should not exceed 100 characters` | E07 e relato da executora |
| Relationship | 99 | Aceito | Persistiu após recarregar | Relato da executora |
| Relationship | 100 | Aceito | Persistiu após recarregar | Relato da executora |
| Relationship | 101 | Rejeitado | `Should not exceed 100 characters` | Relato da executora |

Os valores numéricos longos em nome e parentesco foram utilizados como massa sintética para controlar o comprimento, sem avaliar a adequação semântica de um nome ou parentesco.

## Evidências

As imagens foram preservadas sem edição. Cada referência documenta o estado visível na captura; a sequência de ações e as verificações após recarregar são complementadas pelo relato da executora.

| ID | Arquivo | Conteúdo |
|---|---|---|
| E01 | [Telefone somente com sinais na lista](../evidencias/exploracao/01-telefone-somente-sinais-listagem.png) | Um contato listado com telefone composto por sinais `+` |
| E02 | [Telefone somente com sinais na edição](../evidencias/exploracao/02-telefone-somente-sinais-edicao.png) | O valor com sinais recuperado no formulário de edição |
| E03 | [Telefone numérico na lista](../evidencias/exploracao/03-telefone-numerico-listagem.png) | Um contato com nome original e telefone `51999990000`; funcionário Teste TesteT2 |
| E04 | [Telefone com letras rejeitado](../evidencias/exploracao/04-telefone-letras-rejeitado.png) | Valor `abc` e mensagem sobre caracteres permitidos |
| E05 | [Telefone com 31 caracteres rejeitado](../evidencias/exploracao/05-telefone-31-caracteres-rejeitado.png) | Mensagem de limite de 30 caracteres |
| E06 | [Nome longo na lista](../evidencias/exploracao/06-nome-longo-listagem.png) | Nome numérico longo salvo e exibido com quebra visual de linha |
| E07 | [Nome acima do limite rejeitado](../evidencias/exploracao/07-nome-acima-limite-rejeitado.png) | Mensagem de limite de 100 caracteres |

## Síntese da exploração

Nas verificações executadas, a interface exigiu nome, parentesco e pelo menos um telefone. Rejeitou letras no telefone e comprimentos superiores a 30 caracteres nos três telefones e a 100 caracteres em nome e parentesco. A edição de telefone foi persistida e o cancelamento preservou o nome original, conforme o relato da executora.

O telefone composto exclusivamente por sinais `+` foi aceito e persistido. Esse comportamento constitui uma observação sobre a qualidade da validação: a mensagem informa os caracteres permitidos, mas não explicita a exigência de pelo menos um dígito. O registro não classifica esse comportamento como defeito confirmado. A regra observada na interface será comparada às validações e aos testes originais no trabalho da #26.

Estes resultados descrevem a exploração manual pela interface da demonstração pública. Não representam uma execução direta da API nem medições de testes unitários ou de integração.

## Uso de IA e autoria

A exploração, o preenchimento dos formulários, as verificações após recarregar, a restauração dos dados e as capturas foram realizados por Suzan Stockey Pereira. ChatGPT/Codex (OpenAI) auxiliou na orientação dos passos, na escolha das entradas e na organização e redação deste registro a partir dos relatos e imagens da executora. O escopo da jornada foi revisado por Suzan Stockey Pereira na #25.
