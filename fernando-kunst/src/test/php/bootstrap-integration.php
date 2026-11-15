<?php

use Doctrine\ORM\EntityManager;
use Doctrine\ORM\ORMSetup;
use OrangeHRM\Framework\ServiceContainer;
use OrangeHRM\Framework\Services;

$orangehrm = dirname(__DIR__, 5) . '/orangehrm';

require_once $orangehrm . '/src/vendor/autoload.php';

$config = ORMSetup::createAnnotationMetadataConfiguration(
    glob($orangehrm . '/src/plugins/*/entity', GLOB_ONLYDIR),
    true,
    sys_get_temp_dir()
);

$entityManager = EntityManager::create([
    'driver' => 'pdo_mysql',
    'host' => '127.0.0.1',
    'port' => 3306,
    'dbname' => 'orangehrm_fernando_test',
    'user' => 'root',
    'password' => '',
    'charset' => 'utf8mb4',
], $config);

ServiceContainer::getContainer()->set(
    Services::DOCTRINE,
    $entityManager
);

return $entityManager;