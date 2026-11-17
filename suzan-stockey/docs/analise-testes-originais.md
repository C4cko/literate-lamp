# Análise crítica dos testes originais — Contatos de emergência

## Escopo e método

Análise estática do código original do OrangeHRM na revisão `56e23b3b09e7af29317a0943523b825843fff527`, realizada em 17/11/2026 para a [Issue #26](https://github.com/masmangan/literate-lamp/issues/26). Foram lidos integralmente três arquivos de teste do PIM, a fixture compartilhada e as implementações de serviço e DAO. As conclusões sobre cobertura referem-se aos cenários presentes nesses arquivos, não a uma medição de cobertura de linhas nem à execução da suíte.

## Inventário e classificação

| Arquivo | Métodos de teste | Fronteira exercitada | Classificação pelo comportamento |
|---|---:|---|---|
| EmpEmergencyContactServiceTest.php | 6 | Serviço real → DAO mockado; preparação com fixtures e referência ORM | Testes de delegação com mock, mas dependentes de infraestrutura de banco na preparação; não são unitários totalmente isolados |
| EmpEmergencyContactDaoTest.php | 9 | DAO real → ORM/banco de teste | Integração de persistência e consulta |
| EmpEmergencyContactTest.php | 1 | Entidade → ORM/banco | Integração do mapeamento, apesar de estar na pasta Entity |

As quantidades são métodos declarados no código, não casos executados ou aprovados. A classificação não foi deduzida apenas dos nomes das pastas.

## Serviço: o que o mock comprova

Em `testSaveEmpEmergencyContact`, o teste prepara um contato, configura `saveEmployeeEmergencyContact` no mock para receber esse contato uma vez e devolvê-lo, injeta o mock no serviço e compara nome, parentesco, telefones e funcionário retornados.

**Ponto forte:** verifica a interação esperada entre serviço e DAO e os dados devolvidos. A implementação do serviço é uma delegação direta, portanto esse contrato é pertinente.

**Limite do oráculo:** os campos verificados pertencem ao próprio objeto usado como retorno programado do mock. O teste não demonstra gravação no banco, validação de entrada ou recuperação em nova sessão.

**Isolamento:** embora o DAO seja mockado, `setUp()` popula uma fixture e o teste acessa a referência do funcionário e seu nome (`Kayla`). Essa dependência aumenta o custo e a necessidade de infraestrutura para verificar uma delegação simples.

Os outros métodos verificam listagem, obtenção de um contato, exclusão, busca e contagem com retornos programados. A busca confere quantidade e tipo do primeiro resultado, mas não todos os dados retornados.

## DAO: persistência e consulta

`testSaveEmployeeEmergencyContact` instancia o DAO real, salva o contato e procura-o com `findOneBy`, usando funcionário 1 e sequência 3. Compara os campos e a associação ao funcionário. A fixture já contém sequências 1 e 2 desse funcionário, justificando o próximo identificador esperado.

**Ponto forte:** exercita a geração da sequência e a integração com o repositório ORM, oferecendo evidência mais direta de persistência que o teste de serviço mockado.

**Limite:** não há limpeza explícita do contexto ORM nem abertura de uma nova sessão nesse método. Assim, a consulta não equivale, por si só, a comprovar uma leitura independente do cache de identidade. Uma verificação adicional em contexto limpo fortaleceria o oráculo.

Outros cenários presentes:

- sequência explícita 80 aceita e sequência 100 rejeitada com `InvalidArgumentException`;
- listagem com dois nomes em ordem alfabética;
- contato existente e contato inexistente, com retorno nulo no segundo caso;
- exclusão de dois contatos, verificando contagem e ausência posterior;
- busca por funcionário e nome, limite de um resultado e contagem.

**Técnicas:** exemplos concretos de classes válida/inválida e um valor no limite rejeitado da sequência. A implementação exige `0 < seqNo < 100`, com zero usado inicialmente para geração automática; os testes não constituem uma análise completa dos limites 1/99/100 e dos fluxos de geração.

**Qualidade dos oráculos:** gravação e exclusão possuem asserções sobre os efeitos. Na busca por nome, o teste verifica quantidade e tipo, mas não compara explicitamente o nome do objeto encontrado. Um filtro incorreto que retornasse outro único contato poderia escapar dessa asserção.

**Dados e manutenção:** a fixture repopulada no `setUp()` fornece cenário conhecido com dois funcionários e três contatos. Os IDs e nomes fixos tornam a relação com a fixture compreensível, mas exigem atualização conjunta quando a massa muda. O arquivo não contém cenário explícito de atualização seguido de leitura em contexto independente, nem uma verificação de que uma operação sobre um funcionário preserva os registros do outro.

## Entidade: mapeamento não é validação da jornada

`testEmpEmergencyContactEntity` limpa as tabelas de contato e funcionário, persiste um funcionário e um contato e consulta a entidade pelo funcionário e sequência. Confere associação, sequência, parentesco e telefones.

**Ponto forte:** verifica a integração básica do mapeamento ORM e da associação ao funcionário.

**Limites:** o teste não define nem verifica o nome do contato. Ele não passa pela interface ou pelas regras da API; portanto não comprova a obrigatoriedade do nome na jornada. A consulta usa funcionário 1, dependendo da convenção de IDs do ambiente de teste. Não há cenário de edição ou entradas inválidas nesse arquivo.

## Relação com a exploração manual

| Comportamento explorado por Suzan | Cobertura nos três arquivos lidos | Consequência para o plano |
|---|---|---|
| Nome e parentesco obrigatórios | Sem casos específicos de validação | Exercitar a camada que aplica a regra e a interface |
| Pelo menos um dos três telefones | Sem tabela de decisão ou casos de combinação | Derivar combinações e comparar interface/API |
| Letras rejeitadas e sinais `+` aceitos | Sem casos de formato equivalentes | Separar caracteres permitidos de presença de dígitos; fundamentar o esperado |
| Telefones nos limites 29/30/31 | Sem esses casos | Adicionar valores-limite na camada de validação e na jornada |
| Nome/parentesco nos limites 99/100/101 | Sem esses casos | Adicionar valores-limite rastreáveis |
| Atualização persistida após recarregar | Cadastro inicial é testado; edição explícita não aparece nesses arquivos | Verificar atualização e leitura independente |
| Cancelamento preserva o registro | Não exercitado, pois os arquivos não controlam navegador | Teste de sistema pela interface |
| Associação ao funcionário | Verificada no cadastro e em consultas | Complementar com preservação dos dados de outro funcionário |

Essas lacunas são relativas aos arquivos inspecionados. Não se afirma ausência de testes equivalentes em toda a aplicação. Também não se exige do DAO que reproduza validações pertencentes a outras camadas: a contribuição deve testar cada regra na fronteira em que ela é aplicada.

## Conclusões

A suíte lida oferece uma base útil para delegação, mapeamento, cadastro, consulta e exclusão. Os testes de integração verificam dados concretos, enquanto os mocks mantêm controlado o contrato do serviço. A principal oportunidade para a jornada é complementar essa base com validação de campos, combinações de telefones, atualização e comportamento visível ao usuário, mantendo rastreabilidade aos resultados da [exploração manual](exploracao-contatos-emergencia.md).

Não foi identificado defeito confirmado por esta leitura. A aceitação de sinais sem dígitos permanece uma observação cujo resultado esperado deve ser fundamentado nas regras de validação.

## Fontes primárias

Todos os links fixam a revisão analisada:

- [Testes do serviço](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/src/plugins/orangehrmPimPlugin/test/Service/EmpEmergencyContactServiceTest.php).
- [Testes do DAO](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/src/plugins/orangehrmPimPlugin/test/Dao/EmpEmergencyContactDaoTest.php).
- [Teste da entidade](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/src/plugins/orangehrmPimPlugin/test/Entity/EmpEmergencyContactTest.php).
- [Fixture do DAO](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/src/plugins/orangehrmPimPlugin/test/fixtures/EmpEmergencyContactDao.yml).
- [Implementação do serviço](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/src/plugins/orangehrmPimPlugin/Service/EmpEmergencyContactService.php).
- [Implementação do DAO](https://github.com/orangehrm/orangehrm/blob/56e23b3b09e7af29317a0943523b825843fff527/src/plugins/orangehrmPimPlugin/Dao/EmpEmergencyContactDao.php).

## Uso de IA e participação

ChatGPT/Codex (OpenAI) realizou a leitura e auxiliou na classificação, análise crítica e redação deste documento. Suzan Stockey Pereira analisou inicialmente o teste de serviço e identificou corretamente o uso de DAO mockado, a ausência de comprovação da gravação pelo mock e a ausência de entradas inválidas nesse método. A comparação com o comportamento da interface utiliza a exploração manual executada por Suzan e suas evidências. A revisão do escopo pela autora está registrada na #25.
