<?php

/**
 * T2 VERVAL (PUCRS): testes da jornada PIM > Add Employee.
 * Base comum para os testes de EmployeeAPI::create, reaproveitando a infraestrutura de teste do OrangeHRM
 * (kernel, banco de teste e fixtures YAML), como em test/Api/EmployeeAPITest.php.
 */

namespace OrangeHRM\Tests\Pim\T2AddEmployee;

use DateTime;
use OrangeHRM\Admin\Service\UserService;
use OrangeHRM\Core\Service\DateTimeHelperService;
use OrangeHRM\Core\Authorization\Manager\UserRoleManagerFactory;
use OrangeHRM\Core\Service\ConfigService;
use OrangeHRM\Framework\Services;
use OrangeHRM\Pim\Api\EmployeeAPI;
use OrangeHRM\Pim\Service\EmployeeService;
use OrangeHRM\Tests\Util\EndpointIntegrationTestCase;
use OrangeHRM\Tests\Util\Integration\TestCaseParams;

abstract class AddEmployeeApiTestCase extends EndpointIntegrationTestCase
{
    public const ADMIN_USER_ID = 1;
    public const ESS_USER_ID = 3;

    /**
     * Carrega fixtures/AddEmployee.yml: cópia de EmployeeAPITest.yml (7 funcionários, IDs 0001..0007;
     * 0004 expurgado) mais o contador de IDs de funcionário (last_id = 7). Precisa ser um arquivo só,
     * porque cada populate restaura o banco antes de inserir.
     */
    protected function loadFixtures(): void
    {
        $this->populateFixtures('AddEmployee.yml', __DIR__ . '/fixtures');
    }

    /** @template T @param class-string<T> $entityClass */
    protected function repository(string $entityClass)
    {
        return $this->getEntityManager()->getRepository($entityClass);
    }

    /**
     * Cria o endpoint como o OrangeHRM faz nos próprios testes: kernel com usuário autenticado
     * simulado e os serviços reais usados por EmployeeAPI.
     */
    protected function createApi(array $body, int $userId = self::ADMIN_USER_ID): EmployeeAPI
    {
        $params = new TestCaseParams();
        $params->setUserId($userId);
        $params->setBody($body);
        $params->setServices([
            'pim.employee_service' => EmployeeService::class,
            'core.config_service' => ConfigService::class,
            'admin.user_service' => UserService::class,
        ]);
        $params->setFactories([
            'core.authorization.user_role_manager' => [UserRoleManagerFactory::class, 'getNewUserRoleManager'],
        ]);

        // relógio fixo: create() registra o evento de criação (EmployeeEventService) com a data atual
        $clock = $this->getMockBuilder(DateTimeHelperService::class)->onlyMethods(['getNow'])->getMock();
        $clock->method('getNow')->willReturnCallback(fn () => new DateTime('2026-11-12 10:00:00'));

        $this->createKernelWithMockServices([
            Services::AUTH_USER => $this->getMockAuthUser($params),
            Services::DATETIME_HELPER_SERVICE => $clock,
        ]);
        $this->registerServices($params);

        return $this->getApiEndpointMock(EmployeeAPI::class, $params);
    }

    /** Executa só a validação de criação, como o framework faz antes de chamar create(). */
    protected function validateCreate(array $body): bool
    {
        $api = $this->createApi($body);
        return $this->validate($body, $api->getValidationRuleForCreate());
    }

    protected function assertCreateInvalid(array $body, array $invalidParams): void
    {
        $api = $this->createApi($body);
        $this->assertInvalidParamException(
            fn () => $this->validate($body, $api->getValidationRuleForCreate()),
            $invalidParams
        );
    }

    /** Imagem PNG 1x1 válida, em base64, no formato aceito pela API (Base64Attachment). */
    protected static function pngAttachment(string $name = 'foto.png', string $type = 'image/png'): array
    {
        $png = base64_decode(
            'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4z8AAAAMBAQDJ/pLvAAAAAElFTkSuQmCC'
        );
        return [
            'name' => $name,
            'type' => $type,
            'size' => (string) strlen($png),
            'base64' => base64_encode($png),
        ];
    }
}
