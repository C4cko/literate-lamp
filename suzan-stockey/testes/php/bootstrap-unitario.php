<?php
// Usa o autoload original, sem executar o bootstrap que conecta ao banco.
$root = getenv('OHRM_ROOT');
if (!$root || !is_file($root . '/src/vendor/autoload.php')) {
    throw new RuntimeException('Defina OHRM_ROOT para o clone OrangeHRM com dependencias Composer instaladas.');
}
require_once $root . '/src/vendor/autoload.php';
