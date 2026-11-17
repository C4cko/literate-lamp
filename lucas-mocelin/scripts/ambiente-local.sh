#!/usr/bin/env bash
# Prepara uma instalação LOCAL do OrangeHRM 5.x para rodar todos os testes da jornada.
# Testado em Ubuntu 24.04 (PHP 8.3, MariaDB 10.11, Node 22).
# Uso: scripts/ambiente-local.sh /caminho/onde/clonar
set -euo pipefail
DEST="${1:-$HOME/orangehrm}"
HERE="$(cd "$(dirname "$0")/.." && pwd)"

# 1. dependências do sistema
sudo apt-get update
sudo apt-get install -y git mariadb-server composer \
  php8.3-cli php8.3-mysql php8.3-gd php8.3-intl php8.3-zip php8.3-xml \
  php8.3-mbstring php8.3-curl php8.3-ldap php8.3-bcmath php8.3-pcov
pip install --user aiosmtpd   # SMTP falso para o teste de recuperação de senha

# 2. banco de dados (usuário root / senha root, como no cli_install_config.yaml)
sudo service mariadb start || sudo mysqld_safe &
sleep 5
sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED VIA mysql_native_password USING PASSWORD('root'); FLUSH PRIVILEGES;"

# 3. código-fonte e dependências
git clone https://github.com/orangehrm/orangehrm.git "$DEST"
cd "$DEST"
(cd src && composer install --no-interaction)
(cd devTools/core && composer install --no-interaction)
(cd src/client && yarn install && yarn build)          # gera web/dist

# 4. instalação + banco de testes do PHPUnit
php installer/cli_install.php                          # Admin / Ohrm@1423
git checkout installer/cli_install_config.yaml
php devTools/core/console.php i:create-test-db -p root --dump-options=--ssl=0

# 5. e-mail via SMTP local (Maildir em /tmp/orangehrm-mail)
mysql -uroot -proot orangehrm_mysql -e "REPLACE INTO ohrm_email_configuration
  (id, mail_type, sent_as, smtp_host, smtp_port, smtp_auth_type, smtp_security_type)
  VALUES (1,'smtp','noreply@example.com','127.0.0.1',1025,'none','none');"

# 6. copia os testes PHP da jornada para dentro do OrangeHRM
"$HERE/scripts/instalar-testes-php.sh" "$DEST"

cat <<MSG

Ambiente pronto. Em terminais separados, deixe rodando:
  cd $DEST && php -S 127.0.0.1:8080 -t .
  mkdir -p /tmp/orangehrm-mail && python3 -m aiosmtpd -n -l 127.0.0.1:1025 -c aiosmtpd.handlers.Mailbox /tmp/orangehrm-mail
Depois siga a seção "Como rodar os testes" do README.
MSG
