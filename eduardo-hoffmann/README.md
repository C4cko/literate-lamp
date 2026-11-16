# Eduardo Hoffmann: testes da jornada Time › Attendance (Punch In / Punch Out) no OrangeHRM

Contribuição individual no Trabalho 2 de Verificação e Validação: análise crítica dos testes existentes e testes
unitários, de integração e de sistema da jornada **Registrar entrada e saída (Punch In / Punch Out)** do OrangeHRM.

## Jornada

Como funcionário, quero registrar minha entrada e saída (Punch In / Punch Out) para que minhas horas trabalhadas
fiquem registradas e consultáveis em My Records. A jornada está no plugin `orangehrmAttendancePlugin`.

## Issues

| Issue | Assunto | Situação |
|---|---|---|
| #15 | Análise dos testes existentes e de terceiros | concluída |
| #16 | Plano de testes: jornada de usuário e casos de teste | concluído |
| #17 | Implementar e executar os testes da jornada | testes implementados e executados; Relatório da Execução em [`docs/03-relatorio-execucao.md`](docs/03-relatorio-execucao.md) |

## Organização

| Pasta | Conteúdo | Situação |
|---|---|---|
| `analise/` | análise crítica dos testes do código original e de terceiros ([`01-analise-critica.md`](analise/01-analise-critica.md)) | concluído (#15) |
| `docs/` | jornada ([`01-jornada.md`](docs/01-jornada.md)), plano e casos ([`02-casos-de-teste.md`](docs/02-casos-de-teste.md)) e relatório da execução ([`03-relatorio-execucao.md`](docs/03-relatorio-execucao.md)) | concluído (#16, #17) |
| `ambiente/` | Docker: MariaDB, PHPUnit e OrangeHRM 5.9 local (adaptado de `mateus-neubarth/ambiente`) | concluído (#15, #17) |
| `testes-php/` | unitários (UT-01 a UT-05) e de integração (IT-01 a IT-11) em PHPUnit ([README](testes-php/README.md)) | concluído (#17) |
| `sistema/` | testes de sistema (ST-01 a ST-09) em Java, JUnit 5 e Selenium ([README](sistema/README.md)) | concluído (#17) |

## Resultado

78 testes executados: **77 passaram e 1 falhou de propósito**, o IT-10, que confirma um defeito (o endpoint de
Punch In/Out não verifica a permissão do papel). Detalhes no [relatório](docs/03-relatorio-execucao.md).

## Versão analisada

Código-fonte: <https://github.com/orangehrm/orangehrm>, commit `56e23b3b09e7af29317a0943523b825843fff527`, o mesmo
usado pelos colegas, para que o relatório unificado trate de uma única versão. Servidor de demonstração:
<https://opensource-demo.orangehrmlive.com/>.

## Uso de IA

Claude Code (Anthropic, modelo Claude Sonnet 5.5) é usado como assistente de programação e de redação, na análise dos
testes existentes, no rascunho dos documentos e na escrita dos testes. Todo o conteúdo é revisado pelo autor, que faz
os commits manualmente. Os commits feitos com apoio da IA levam a linha `Co-Authored-By: Claude`.
