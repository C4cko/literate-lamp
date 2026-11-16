# Plano de Testes e Casos de Teste: PIM › Add Employee

> Base: [jornada.md](jornada.md). Versão do código analisada: commit `56e23b3b` de `orangehrm/orangehrm`.

## 1. Plano de Testes

### 1.1 Objetivo
Verificar se o cadastro de funcionário (fluxo principal, A1–A3 e exceções E1–E9) segue as regras da
jornada, e investigar os riscos R1–R4 levantados na leitura do código.

### 1.2 Escopo
- **Dentro:** formulário *Add Employee*; `POST /api/v2/pim/employees`; `POST /api/v2/admin/users`
  na criação do login; regras de validação, política de senha e geração de Employee Id.
- **Fora:** edição de *Personal Details*, importação CSV, relatórios e outros módulos.

### 1.3 Níveis, ferramentas e ambiente

| Nível | Alvo | Ferramenta | Ambiente |
|---|---|---|---|
| Unitário | classes PHP do `orangehrmPimPlugin` (e serviços que ele usa) | **PHPUnit** (infra do próprio OrangeHRM: `TestCase`, `EndpointTestCase`, fixtures YAML) | OrangeHRM local via Docker |
| Integração | API REST v2 | **Java 17 + JUnit 5 + REST Assured** (Maven) | OrangeHRM local via Docker (principal) e servidor de demonstração (verificação pontual) |
| Sistema (E2E) | jornada pela interface | **Java 17 + JUnit 5 + Playwright for Java**, padrão Page Object | servidor de demonstração e/ou local |

Por que Java nos níveis de integração e sistema: é a linguagem de preferência do autor, e tanto o
REST Assured quanto o Playwright têm suporte oficial a ela.

### 1.4 Técnicas de projeto
- **Particionamento em classes de equivalência (PE)** para cada campo.
- **Análise de valor-limite (AVL)** nos tamanhos (0, 1, máx., máx.+1) e no tamanho da foto (1 MB).
- **Tabela de decisão (TD)** para a combinação foto × login × validade dos dados.
- **Teste baseado em casos de uso (CU)** para o fluxo principal, os alternativos e as exceções.
- **Teste guiado por risco** para R1–R4.

### 1.5 Critérios
- **Entrada:** ambiente de pé, usuário Admin disponível, política de senha nos valores padrão.
- **Saída:** todos os casos de prioridade Alta executados; defeitos registrados como Issue com evidência.
- **Oráculo:** regras confirmadas no código (seção 3 da jornada) e mensagens oficiais de
  `installer/Migration/*/lang-string/*.yaml`.

### 1.6 Isolamento e dados
- Dados únicos a cada execução (sufixo com timestamp em nomes, IDs e usernames), para rodar no
  servidor compartilhado sem colisões.
- Limpeza: os testes de integração e E2E excluem os funcionários e usuários que criarem
  (`DELETE /api/v2/pim/employees`, `DELETE /api/v2/admin/users`).
- Os testes unitários usam as fixtures e o banco de teste do próprio OrangeHRM.

### 1.7 Riscos do próprio plano
- O servidor de demonstração é reiniciado com frequência e é usado por muita gente: a execução
  oficial deve ser feita no ambiente local (Docker).
- Se a política de senha do servidor for alterada, os casos de E6 precisam ser revistos.

## 2. Classes de equivalência e valores-limite

| Campo | Classes válidas | Classes inválidas | Limites testados |
|---|---|---|---|
| First / Last Name | V1: 1–30 caracteres | I1: vazio; I2: > 30; I3: só espaços* | 0, 1, 30, 31 |
| Middle Name | V2: vazio; V3: 1–30 | I4: > 30 | 0, 30, 31 |
| Employee Id | V4: vazio (API); V5: 1–10, único | I5: > 10; I6: já existente | 10, 11 |
| Foto | V6: gif/jpg/jpeg/jfif/png ≤ 1 MB | I7: outro tipo (pdf, bmp, svg); I8: > 1 MB; I9: nome > 100 | 1 MB, 1 MB + 1 byte |
| Username | V7: 5–40, único | I10: < 5; I11: > 40; I12: existente | 4, 5, 40, 41 |
| Password | V8: 8–64, atende à política | I13: < 8; I14: sem maiúscula; I15: sem minúscula; I16: sem número; I17: sem especial; I18: com espaço; I19: > 64; I20: fraca/adivinhável | 7, 8, 64, 65 |
| Confirm Password | V9: igual | I21: diferente | – |

\* Comportamento com nome só de espaços não está definido no código; o caso serve para descobrir o comportamento.

> Em andamento: as demais seções entram nos próximos commits.

## Fontes
- Jornada e regras: [jornada.md](jornada.md) e os arquivos do OrangeHRM citados nela.
- Técnicas: material da disciplina (particionamento em equivalência, valor-limite, tabela de decisão, casos de uso).
- Ferramentas: PHPUnit (https://phpunit.de), REST Assured (https://rest-assured.io), Playwright for Java (https://playwright.dev/java).
- Uso de IA: projeto inicial dos casos feito com Claude Code (Claude Opus 5.5), revisado pelo autor.
