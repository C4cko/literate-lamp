# Eduardo Hoffmann: testes da jornada Time › Attendance (Punch In / Punch Out) no OrangeHRM

Contribuição individual no Trabalho 2 de Verificação e Validação: análise crítica dos testes existentes e testes
unitários, de integração e de sistema da jornada **Registrar entrada e saída (Punch In / Punch Out)** do OrangeHRM.

> Em andamento. Esta pasta é preenchida aos poucos, uma Issue por vez.

## Jornada

Como funcionário, quero registrar minha entrada e saída (Punch In / Punch Out) para que minhas horas trabalhadas
fiquem registradas e consultáveis em My Records. A jornada está no plugin `orangehrmAttendancePlugin`.

## Issues

| Issue | Assunto | Situação |
|---|---|---|
| #15 | Análise dos testes existentes e de terceiros | análise estática feita; falta executar a suíte original e explorar o demo |
| #16 | Plano de testes: jornada de usuário e casos de teste | planejado |
| #17 | Implementar e executar os testes da jornada | planejado |

## Organização

| Pasta | Conteúdo | Situação |
|---|---|---|
| `analise/` | análise crítica dos testes do código original e de terceiros ([`01-analise-critica.md`](analise/01-analise-critica.md)) | em andamento (#15) |
| `docs/` | jornada de usuário e projeto dos casos de teste | planejado (#16) |
| testes | unitários, de integração e de sistema (sistema em Java, JUnit 5 e Selenium, conforme a #17) | planejado (#17) |

## Versão analisada

Código-fonte: <https://github.com/orangehrm/orangehrm>, commit `56e23b3b09e7af29317a0943523b825843fff527`, o mesmo
usado pelos colegas, para que o relatório unificado trate de uma única versão. Servidor de demonstração:
<https://opensource-demo.orangehrmlive.com/>.

## Uso de IA

Claude Code (Anthropic, modelo Claude Sonnet 5.5) é usado como assistente de programação e de redação, na análise dos
testes existentes, no rascunho dos documentos e na escrita dos testes. Todo o conteúdo é revisado pelo autor, que faz
os commits manualmente. Os commits feitos com apoio da IA levam a linha `Co-Authored-By: Claude`.
