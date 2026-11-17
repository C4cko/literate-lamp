#!/usr/bin/env bash
set -euo pipefail
cd /app
mkdir -p /resultados
{
  date -u '+%Y-%m-%dT%H:%M:%SZ'
  git -c safe.directory=/app rev-parse HEAD
  php --version
  composer --version
  ./src/vendor/bin/phpunit --version
  php -r '$p=new PDO("mysql:host=db;dbname=suzan_emergency_dev", "root", "root"); echo "Banco: ".$p->query("SELECT VERSION()")->fetchColumn().PHP_EOL;'
} > /resultados/ambiente.txt 2>&1
set +e
./src/vendor/bin/phpunit --colors=never --testdox \
  --filter '(EmpEmergencyContactDaoTest|EmpEmergencyContactServiceTest|EmpEmergencyContactTest)' \
  --log-junit /resultados/phpunit-junit.xml \
  src/plugins/orangehrmPimPlugin/test 2>&1 | tee /resultados/phpunit-testdox.txt
status=${PIPESTATUS[0]}
set -e
printf '%s\n' "$status" > /resultados/exit-code.txt
exit "$status"
