# 3. Relatório de Execução dos Testes – Jornada "Autenticação (Login)"

> Documento da **Issue "Execução dos Testes"**. Plano correspondente: [`02-plano-de-testes.md`](02-plano-de-testes.md).

## 3.1 Ambiente

| Item | Versão |
|---|---|
| OrangeHRM | 5.9 – commit `56e23b3` (instalação local via `installer/cli_install.php`) |
| PHP / PHPUnit | 8.3.6 / 9.6.16 (cobertura com pcov) |
| Banco | MariaDB 10.11.14 (`orangehrm_mysql` para o sistema, `test_orangehrm_mysql` para o PHPUnit) |
| Servidor web | `php -S 127.0.0.1:8080` (servidor embutido do PHP) |
| E-mail | SMTP falso `aiosmtpd` em 127.0.0.1:1025, gravando em Maildir |
| Navegador / E2E | Chromium 153 (Chrome for Testing) / Playwright 1.65.1, Node 22 |
| SO | Ubuntu 24.04 |
| Data da execução | 16/11/2026 |

## 3.2 Resultado consolidado

| Nível | Planejados | Executados | Passaram | Falharam | Evidência |
|---|---|---|---|---|---|
| Unitário | 32 | 32 | 32 | 0 | `resultados/phpunit-testdox.txt`, `phpunit-junit.xml` |
| Integração | 15 | 15 | 15 | 0 | idem |
| Sistema (local) | 19 | 19 | 19 | 0 | `resultados/playwright-local.txt`, `playwright-junit.xml` |
| Sistema (demo pública) | 19 | 18 | 18 | 0 | `resultados/playwright-demo-junit.xml` (ST-17 não se aplica) |
| **Total (local)** | **66** | **66** | **66** | **0** | |

Suíte original `Authentication` no mesmo ambiente: 54 testes, 160 asserções, todos passaram.

**Conclusão: todos os testes passaram. Nenhum defeito funcional foi encontrado na jornada.**
Foram registradas 4 observações (seção 3.4), que não violam requisitos documentados do produto,
mas representam riscos de segurança ou de usabilidade.

## 3.3 Detalhamento

### Unitário (PHPUnit, sem banco)

```
Authentication Service Decision Table   7/7  ✔  (UT-AS-01..07)
Password Policy Boundary               20/20 ✔  (UT-PW-01..20)
Password Strength Validation            5/5  ✔  (UT-SV-01..05)
Tempo: 0,04 s
```

### Integração (PHPUnit + MariaDB)

```
Login Integration                      15/15 ✔  (IT-01..15)   30 asserções
Tempo: 1,9 s
```

Destaques:
* **IT-10**: um usuário com hash MD5 legado consegue entrar, e a senha é regravada em bcrypt
  (`password_verify` confirma o novo hash).
* **IT-11**: com a política de senha forte ligada, o login de `fabio/fabio123` gera
  `PasswordEnforceException`, redireciona para `changeWeakPassword/resetCode/…` e cria um
  registro em `ohrm_enforce_password`.
* **IT-14**: entradas com injeção de SQL não autenticam (o Doctrine usa parâmetros).

### Sistema (Playwright, instalação local)

```
01-login.spec.js               ST-01..ST-10  10 ✔
02-sessao.spec.js              ST-11..ST-13   3 ✔
03-recuperacao-senha.spec.js   ST-14..ST-17   4 ✔
04-seguranca.spec.js           ST-18..ST-19   2 ✔
19 passed (43.8s)
```

### Sistema (Playwright, servidor de demonstração público)

Executado em 17/11/2026 contra <https://opensource-demo.orangehrmlive.com> (OrangeHRM OS 5.9),
Windows 11, Node 26, Chromium do Playwright, usuário `Admin`/`admin123`:

```
18 passed, 1 skipped (1.7m)
```

O ST-17 é ignorado automaticamente na demo porque precisa ler a caixa de e-mail (só na
instalação local). Evidência: `resultados/playwright-demo-junit.xml`.

### Eficácia (mutação e cobertura)

| Métrica | Suíte original (Authentication) | Suíte da jornada |
|---|---|---|
| Mutantes detectados | 3 de 9 (33 %) | **9 de 9 (100 %)** |
| Linhas de `LocalAuthProvider` | 0 % | 91,3 % |
| Linhas de `AuthenticationService` | 100 % | 100 % |
| Linhas de `PasswordStrengthService` | 85,8 % | 55,4 %* |

\* A suíte da jornada não cobre os métodos de troca de senha por *reset code* (`validateUrl`,
`saveEnforcedPassword`…), que pertencem a outra jornada. A suíte original cobre esses métodos.
As duas suítes se complementam. Detalhes em `resultados/mutacao.txt` e `resultados/cobertura-*.txt`.

## 3.4 Observações (não são defeitos)

| ID | Severidade | Descrição | Caso | Recomendação |
|---|---|---|---|---|
| OBS-01 | Média (segurança) | **Sem bloqueio de conta nem limitação de tentativas.** Depois de 10 senhas erradas seguidas, a senha correta é aceita na hora (confirmado na instalação local e na demo pública). Isso facilita ataques de força bruta ou *credential stuffing*. | ST-19 | Limitar tentativas por usuário/IP com atraso progressivo ou CAPTCHA (OWASP ASVS 2.2.1). |
| OBS-02 | Baixa (segurança) | **O código de recuperação de senha expõe o nome de usuário**: o código é Base64 de `Admin#SEPARATOR#<bytes aleatórios>`. Quem vê o link (proxy, log, histórico) descobre o login. | ST-17 | Usar só um token aleatório e buscar o usuário no servidor. |
| OBS-03 | Baixa (usabilidade) | **O usuário não é aparado (*trim*)**: " Admin " é recusado com "Invalid credentials", sem pista do motivo (comum ao copiar e colar). | ST-09 | Remover espaços nas pontas do usuário antes de autenticar. |
| OBS-04 | Informativa | **Usuário não diferencia maiúsculas/minúsculas, senha sim.** O comportamento vem da *collation* do banco, não de uma regra explícita do código. Pode mudar se o banco usar outra *collation*. | IT-04, IT-05, ST-10 | Documentar a regra ou normalizar o usuário explicitamente. |

Comportamentos de segurança **confirmados como corretos**: mensagem única para usuário ou senha
inválidos (ST-03); recuperação de senha sem enumeração de usuários (ST-16); link de redefinição
de uso único (ST-17); proteção CSRF no login (ST-18); logout invalida a sessão também para a API
(ST-12); injeção de SQL não funciona (IT-14).

## 3.5 Problemas encontrados nos próprios testes (lições aprendidas)

* **IT-11 falhou de forma intermitente** na 1ª versão: a tabela `ohrm_enforce_password` não
  consta da fixture e por isso não é limpa entre execuções. O teste esperava exatamente 1
  registro e encontrou 2 vindos de execuções anteriores. Correção: medir a **diferença** de
  registros antes e depois do login. É o mesmo tipo de dependência entre execuções criticado
  nos testes de terceiros que usam a demo pública.
* **ST-19 falhou duas vezes na demo pública** (passava na instalação local), sem relação com
  defeito do OrangeHRM:
  1. *Tempo-limite*: 11 envios de formulário a ~5 s cada estouraram o limite padrão de 60 s.
     Correção: `test.setTimeout(180_000)` só nesse caso.
  2. *Condição de corrida*: após uma tentativa errada a página recarrega; o teste digitava o
     usuário durante o recarregamento, o campo era apagado e o formulário saía com "Required"
     (confirmado pelo instantâneo da página no momento da falha). Correção: abrir a tela de
     login do zero (`login.goto()`) antes de cada tentativa. Isso também tornou o oráculo mais
     forte: o alerta "Invalid credentials" verificado passa a ser o da tentativa atual, e não o
     que sobrou da anterior.
  Lição: um teste E2E verde em ambiente rápido pode esconder dependência de tempo; rodar em
  mais de um ambiente revelou o problema.
* Os testes de exceção usam `expectException()` ou `fail()` dentro do `try`, para não repetir a
  falha F1 da suíte original (exceção esperada que nunca é verificada).

---
- Uso de IA: instalação do ambiente, implementação e execução dos testes, análise de mutação e cobertura
  e redação inicial feitas com Claude (Anthropic, Claude Opus 5.5), revisadas pelo autor.
