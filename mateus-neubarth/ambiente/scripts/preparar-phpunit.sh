#!/usr/bin/env bash
# Prepara o clone do OrangeHRM para rodar PHPUnit (executar UMA vez, a partir da pasta ambiente/):
#   docker compose up -d db phpunit
#   docker compose exec phpunit bash /scripts/preparar-phpunit.sh
# Passos iguais aos do CI oficial (.github/workflows/test.yml), sem o front-end.
set -euo pipefail
cd /app

echo ">> composer install (src e devTools/core)"
composer install -d src --no-interaction --no-progress
composer install -d devTools/core --no-interaction --no-progress

if [ ! -f lib/confs/Conf.php ]; then
  echo ">> instalando o OrangeHRM no banco orangehrm_dev"
  php installer/cli_install.php
else
  echo ">> lib/confs/Conf.php já existe; instalação ignorada"
fi

echo ">> criando o banco de testes (i:create-test-db)"
php devTools/core/console.php i:create-test-db -p root --dump-options=--ssl=0

echo ">> pronto. Ex.: docker compose exec phpunit ./src/vendor/bin/phpunit --testsuite Pim"
