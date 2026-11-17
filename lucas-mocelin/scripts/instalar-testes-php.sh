#!/usr/bin/env bash
# Copia os testes PHP da jornada para dentro de um clone do OrangeHRM,
# no diretório do plugin de autenticação (namespace OrangeHRM\Tests\Authentication\JornadaLogin).
# Uso: scripts/instalar-testes-php.sh /caminho/para/orangehrm
set -euo pipefail
ROOT="${1:?informe o diretório do orangehrm}"
HERE="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/src/plugins/orangehrmAuthenticationPlugin/test/JornadaLogin"
mkdir -p "$DEST"
cp -r "$HERE/testes/php/." "$DEST/"
echo "Testes copiados para $DEST"
