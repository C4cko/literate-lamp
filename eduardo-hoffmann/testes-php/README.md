# Testes PHPUnit: Time › Attendance (Punch In / Punch Out)

Issue #17. Casos de [`../docs/02-casos-de-teste.md`](../docs/02-casos-de-teste.md), escritos com a infraestrutura de
teste do próprio OrangeHRM (PHPUnit 9.6, `EndpointTestCase`, `KernelTestCase`).

## Onde os testes ficam

A pasta `T2Attendance/` foi feita para viver dentro do plugin Attendance, em
`src/plugins/orangehrmAttendancePlugin/test/T2Attendance/`. Assim o autoload de testes
(`OrangeHRM\Tests\Attendance\` → `plugins/orangehrmAttendancePlugin/test`) encontra as classes e a suíte
`Attendance` do `phpunit.xml` passa a incluí-las.

No ambiente local, o `ambiente/docker-compose.yml` monta a pasta nesse caminho (somente leitura); nada é copiado para
o clone do OrangeHRM.

## Como rodar

Com o ambiente pronto (`../ambiente/`, ver o README da pasta `eduardo-hoffmann`), a partir de `../ambiente`:

```bash
docker compose up -d db phpunit
docker compose exec phpunit bash /scripts/preparar-phpunit.sh   # uma vez
docker compose exec phpunit ./src/vendor/bin/phpunit --group T2Attendance --testdox
```

No Git Bash do Windows, prefixe os comandos com `MSYS_NO_PATHCONV=1`, para o `/scripts` não ser convertido em um
caminho do Windows.

## Arquivos

| Arquivo | Casos | Técnica | Nível real |
|---|---|---|---|
| `Unitario/RelogioDaPunchTest.php` | UT-01 (margem de 180 s) e UT-02 (configuração × hora) | AVL e tabela de decisão | unitário: relógio e configuração simulados, sem banco |
| `Unitario/ValidacaoDePunchTest.php` | UT-03 (nota, data, hora) e UT-04 (fuso) | PE e AVL | unitário das regras de validação |
| `Unitario/MensagensDeExcecaoTest.php` | UT-05 (9 mensagens de `AttendanceServiceException`) | PE | unitário |

## Resultado

Ver o Relatório da Execução dos Testes (comentário final da issue #17).
