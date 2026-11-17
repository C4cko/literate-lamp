#!/usr/bin/env bash
# Análise de mutação manual: injeta um defeito por vez no código da autenticação,
# roda a suíte ORIGINAL do plugin e a suíte da JORNADA, e informa quem detectou.
# Uso: scripts/mutacao.sh /caminho/para/orangehrm
set -u
ROOT="${1:?informe o diretório do orangehrm}"
cd "$ROOT"
PHPUNIT="php src/vendor/bin/phpunit"
AUTH=src/plugins/orangehrmAuthenticationPlugin
ORIG="--testsuite Authentication --exclude-group JornadaLogin"
NOVO="$AUTH/test/JornadaLogin"

declare -a M=(
 "M1|$AUTH/Service/AuthenticationService.php|if (!\$user->getStatus()) {|if (false) {|conta desabilitada deixa de ser bloqueada"
 "M2|$AUTH/Service/AuthenticationService.php|} elseif (\$user->getEmployee()->getEmployeeTerminationRecord() instanceof EmployeeTerminationRecord) {|} elseif (false) {|funcionário desligado deixa de ser bloqueado"
 "M3|$AUTH/Service/PasswordStrengthService.php|return \$this->getTextHelper()->strLength(\$password) < \$minLength;|return \$this->getTextHelper()->strLength(\$password) <= \$minLength;|limite mínimo de comprimento deslocado (< para <=)"
 "M4|$AUTH/Service/PasswordStrengthService.php|return \$this->getTextHelper()->strLength(\$password) > \$maxLength;|return \$this->getTextHelper()->strLength(\$password) >= \$maxLength;|limite máximo de comprimento deslocado (> para >=)"
 "M5|$AUTH/Service/PasswordStrengthService.php|return \$isSpacesAllowed === 'false' && preg_match_all|return \$isSpacesAllowed === 'true' && preg_match_all|regra de espaços invertida"
 "M6|src/plugins/orangehrmAdminPlugin/Service/UserService.php|return \$hash == md5(\$password);|return false;|login com hash legado (MD5) deixa de funcionar"
 "M7|$AUTH/Auth/LocalAuthProvider.php|->getValue(ConfigService::KEY_ENFORCE_PASSWORD_STRENGTH) === 'on') {|->getValue(ConfigService::KEY_ENFORCE_PASSWORD_STRENGTH) === 'off') {|política de senha forte no login ignorada"
 "M8|$AUTH/Service/AuthenticationService.php|\$user->isDeleted()) {|false) {|usuário excluído volta a conseguir logar"
 "M9|$AUTH/Service/AuthenticationService.php|throw AuthenticationException::userDisabled();|return false;|conta desabilitada retorna false em vez de exceção"
)

printf "%-4s | %-48s | %-10s | %-10s\n" "ID" "Mutação" "Original" "Jornada"
for entry in "${M[@]}"; do
  IFS='|' read -r id file from to desc <<< "$entry"
  cp "$file" /tmp/mut.bak
  FROM="$from" TO="$to" php -r '$f=$argv[1];$s=file_get_contents($f);$n=0;$s=str_replace(getenv("FROM"),getenv("TO"),$s,$n);file_put_contents($f,$s);exit($n?0:1);' "$file" \
    || { echo "$id: trecho não encontrado"; continue; }
  $PHPUNIT $ORIG >/dev/null 2>&1 && o="sobrevive" || o="MORTO"
  $PHPUNIT $NOVO >/dev/null 2>&1 && n="sobrevive" || n="MORTO"
  cp /tmp/mut.bak "$file"
  printf "%-4s | %-48s | %-10s | %-10s\n" "$id" "$desc" "$o" "$n"
done
