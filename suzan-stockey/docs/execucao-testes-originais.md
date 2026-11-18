# Registro de preparação e execução dos testes originais

## Resultado no GitHub Actions — 17/11/2026

**16 testes executados, 56 asserções, zero falhas, erros, avisos ou casos ignorados**, conforme relatório JUnit. Código de saída do PHPUnit: 0. A saída textual registra `OK (16 tests, 56 assertions)`, tempo 0,137 s e memória 26 MB.

| Classe original | Testes |
|---|---:|
| EmpEmergencyContactDaoTest | 9 |
| EmpEmergencyContactServiceTest | 6 |
| EmpEmergencyContactTest (entidade) | 1 |

Execução: [GitHub Actions 44174448568](https://github.com/masmangan/literate-lamp/actions/runs/44174448568), job `originais`, concluído com sucesso. Código da contribuição usado pelo job: `a2ca05cb280907f43657d0f2e95def07790cdf5d`. Código oficial: `56e23b3b09e7af29317a0943523b825843fff527`, sem alteração dos três arquivos de teste.

Ambiente efetivo: Ubuntu 24.04, PHP 7.4.33, Composer 2.8.12, PHPUnit 9.6.16 e MariaDB 10.11.19. As versões estão preservadas em [ambiente.txt](../resultados/originais/ambiente.txt). A execução do PHPUnit ocorreu às 20:57 UTC (17:57 de Brasília).

Comando executado na raiz do clone oficial:

```bash
./src/vendor/bin/phpunit --colors=never --testdox \
  --filter '(EmpEmergencyContactDaoTest|EmpEmergencyContactServiceTest|EmpEmergencyContactTest)' \
  --log-junit ../suzan-stockey/resultados/originais/phpunit-junit.xml \
  src/plugins/orangehrmPimPlugin/test
```

Evidências preservadas:

- [Saída testdox](../resultados/originais/phpunit-testdox.txt).
- [JUnit XML](../resultados/originais/phpunit-junit.xml).
- [Código de saída](../resultados/originais/exit-code.txt).
- [Artefato original do job](https://github.com/masmangan/literate-lamp/actions/runs/44174448568/artifacts/14369351590).

Os totais do JUnit foram conferidos contra a saída textual. Os testes passaram nas condições dessa execução; esse resultado não demonstra cobertura das validações e dos fluxos ausentes nos arquivos analisados, discutidos na [análise crítica](analise-testes-originais.md).

## Reprodução

O [workflow](../../.github/workflows/suzan-emergency-contacts.yml) prepara o banco e executa o filtro no GitHub Actions. A documentação de [ambiente de testes](ambiente-local.md) descreve a configuração e os relatórios gerados.

## Uso de IA

ChatGPT/Codex (OpenAI) preparou os arquivos, conferiu a configuração, configurou a execução no GitHub Actions e conferiu os relatórios a pedido de Suzan Stockey Pereira. O GitHub Actions executou os testes automatizados. Os resultados acima correspondem às saídas reais dessas operações.
