# Issues do trabalho

Textos das Issues abertas no repositório (#32 a #38), uma por atividade, todas atribuídas ao autor.

---

## Issue #32 – Análise crítica dos testes do módulo Recruitment

**Descrição.** Identificar os testes do código original do OrangeHRM para o módulo Recruitment
(PHPUnit em `src/plugins/orangehrmRecruitmentPlugin/test`) e pelo menos dois materiais de terceiros, e
avaliar nível, técnicas de projeto, oráculo, cobertura e manutenção.

**Entrega.** `giancarlo-mena/analise/01-analise-critica.md`

- [ ] levantar os testes originais e o nível real de cada grupo
- [ ] avaliar técnicas e oráculos
- [ ] analisar dois materiais de terceiros (com link)
- [ ] listar as lacunas que o plano vai cobrir

---

## Issue #33 – Jornada de usuário "Recrutador contrata um candidato"

**Descrição.** Descrever a jornada com persona, história de usuário, critérios de aceitação, passos e a
máquina de estados do processo seletivo.

**Entrega.** `giancarlo-mena/docs/01-jornada.md`

---

## Issue #34 – Relatório do Plano de Testes

**Descrição.** Projeto dos casos de teste unitários, de integração e de sistema da jornada, com as técnicas
de partição de equivalência, valor-limite, tabela de decisão e transição de estados, mais as hipóteses de
defeito tiradas do código.

**Entrega.** `giancarlo-mena/docs/02-plano-de-testes.md`

**Resumo do plano**

- Sistema: OrangeHRM 5 (código em github.com/orangehrm/orangehrm, servidor de demonstração).
- Jornada: cadastrar candidato em uma vaga e levá-lo até a contratação (Shortlist → Interview → Offer → Hire).
- Unitários (U01–U33): modelo da máquina de estados, regras dos formulários e cliente HTTP, em JUnit 5.
- Integração (I01–I12): API REST v2 com `java.net.http.HttpClient`.
- Sistema (S01–S06): interface com Playwright for Java.
- Hipóteses de defeito H1–H5.
- Como rodar: `giancarlo-mena/testes/README.md`.

---

## Issue #35 – Implementar testes unitários (Java 21 + JUnit 5)

**Entrega.** `testes/src/main/.../modelo`, `testes/src/main/.../cliente`, testes em `testes/src/test/.../modelo`
e `.../cliente`.

- [ ] modelo da máquina de estados + U01–U07
- [ ] regras do candidato + U10–U17
- [ ] regras da entrevista + U20–U23
- [ ] cliente HTTP + U30–U33

---

## Issue #36 – Implementar testes de integração da API

**Entrega.** `testes/src/test/.../integracao`

- [ ] cadastro de candidato (I01–I03)
- [ ] jornada completa, funcionário no PIM e histórico (I04–I06, I12)
- [ ] transições inválidas, entrevistas e vaga fechada (I07–I11)
- [ ] hipóteses de defeito (H1–H5)

---

## Issue #37 – Implementar testes de sistema (Playwright)

**Entrega.** `testes/src/test/.../sistema`

- [ ] page objects e base
- [ ] jornada completa pela interface (S01, S06)
- [ ] validações do formulário (S02–S05)

---

## Issue #38 – Relatório da Execução dos Testes

**Descrição.** Executar os três níveis com `./executar-testes.sh`, guardar a saída em `resultados/` e
registrar o resultado de cada caso, as hipóteses confirmadas e os defeitos encontrados.

**Entrega.** `giancarlo-mena/docs/03-relatorio-execucao.md` e `giancarlo-mena/resultados/`

---

## Uso de IA (para colar no final de cada Issue)

> Este trabalho usou o Claude (Anthropic, modelo Claude Opus 5.5) como assistente de programação e de
> redação. Todo o conteúdo foi revisado pelo autor.
