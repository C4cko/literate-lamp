# Testes da jornada Recruitment › Contratar candidato (OrangeHRM)

Contribuição individual no Trabalho 2 de Verificação e Validação de Software: análise crítica dos testes
existentes e testes unitários, de integração e de sistema, em **Java 21**, para a jornada
**"Recrutador contrata um candidato"** do módulo Recruitment do OrangeHRM.

## Organização

| Pasta | Conteúdo |
|---|---|
| `analise/` | análise crítica dos testes do código original e de material de terceiros |
| `docs/` | jornada de usuário, plano e casos de teste, relatório de execução, textos das Issues |
| `testes/` | projeto Maven (Java 21, JUnit 5) com os três níveis de teste |
| `resultados/` | saída das execuções (gerada por `executar-testes.sh`) |

## Documentos

1. [Análise crítica](analise/01-analise-critica.md)
2. [Jornada de usuário](docs/01-jornada.md)
3. [Plano e casos de teste](docs/02-plano-de-testes.md)
4. [Relatório de execução](docs/03-relatorio-execucao.md)

## Como rodar

Pré-requisitos: JDK 21 e Maven 3.9. Detalhes em [testes/README.md](testes/README.md).

```bash
cd testes
mvn test                 # unitários (não precisa de servidor)
mvn test -Pintegracao    # API REST do servidor de demonstração
mvn test -Psistema       # interface, com Playwright
```

Ou, para rodar tudo e guardar os resultados: `./executar-testes.sh`.

## Uso de IA

Este trabalho usou o Claude (Anthropic, modelo Claude Opus 5.5) como assistente de programação e de
redação. Todo o conteúdo foi revisado pelo autor.
