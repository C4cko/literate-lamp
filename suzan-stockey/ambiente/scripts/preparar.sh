#!/usr/bin/env bash
set -euo pipefail
cd /app
expected=56e23b3b09e7af29317a0943523b825843fff527
actual=$(git -c safe.directory=/app rev-parse HEAD)
if [ "$actual" != "$expected" ]; then
  echo "Revisão incorreta: $actual; esperada: $expected" >&2
  exit 1
fi
composer install -d src --no-interaction --no-progress
composer install -d devTools/core --no-interaction --no-progress
if [ ! -f lib/confs/Conf.php ]; then
  php installer/cli_install.php
fi
php devTools/core/console.php i:create-test-db -p root --dump-options=--ssl=0
