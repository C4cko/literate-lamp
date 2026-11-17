<?php

/**
 * T2 VERVAL (PUCRS): base dos testes de integração de Claim (renan-bertoletti/plano, seção 6.2).
 *
 * Cada chamada monta o endpoint como o framework faz (kernel com o usuário autenticado, serviços reais,
 * UserRoleManager real, máquina de estados carregada do banco), aplica a validação e só então executa a operação.
 * As exceções da API são traduzidas para o código HTTP que o cliente receberia.
 *
 * Diferença em relação aos testes originais (#5): o oráculo também relê o estado no BANCO (dbClaim), e não só a
 * resposta da API.
 */

namespace OrangeHRM\Tests\Claim\T2ClaimIntegracao;

use DateTime;
use OrangeHRM\Claim\Api\ClaimRequestActionAPI;
use OrangeHRM\Claim\Api\EmployeeClaimRequestAPI;
use OrangeHRM\Claim\Api\MyClaimRequestAPI;
use OrangeHRM\Config\Config;
use OrangeHRM\Core\Api\V2\Exception\BadRequestException;
use OrangeHRM\Core\Api\V2\Exception\ForbiddenException;
use OrangeHRM\Core\Api\V2\Exception\InvalidParamException;
use OrangeHRM\Core\Api\V2\Exception\RecordNotFoundException;
use OrangeHRM\Core\Service\AccessFlowStateMachineService;
use OrangeHRM\Core\Service\DateTimeHelperService;
use OrangeHRM\Framework\Services;
use OrangeHRM\Tests\Util\EndpointIntegrationTestCase;
use OrangeHRM\Tests\Util\Integration\TestCaseParams;
use OrangeHRM\Tests\Util\TestDataService;

abstract class ClaimIntegrationTestCase extends EndpointIntegrationTestCase
{
    // usuários da fixture T2Claim.yaml
    public const ADMIN = 1;
    public const ESS = 2;
    public const SUPERVISOR = 3;
    public const OUTRO = 4;

    public const EMP_ESS = 1;

    // reivindicações da fixture, uma por estado de partida
    public const INITIATED = 1;
    public const SUBMITTED = 2;
    public const APPROVED = 3;
    public const REJECTED = 4;
    public const CANCELLED = 5;
    public const PAID = 6;
    public const ATRIBUIDA_PELO_ADMIN = 7;
    public const ATRIBUIDA_PELO_SUPERVISOR = 8;
    public const DO_PROPRIO_ADMIN = 9;
    public const SEM_DESPESAS = 10;

    private const SERVICES = [
        'claim.claim_service' => \OrangeHRM\Claim\Service\ClaimService::class,
        'core.config_service' => \OrangeHRM\Core\Service\ConfigService::class,
        'core.authorization.user_role_manager_helper' => \OrangeHRM\Core\Authorization\Helper\UserRoleManagerHelper::class,
        'pim.employee_service' => \OrangeHRM\Pim\Service\EmployeeService::class,
        'admin.user_service' => \OrangeHRM\Admin\Service\UserService::class,
        'core.normalizer_service' => \OrangeHRM\Core\Service\NormalizerService::class,
        'admin.pay_grade_service' => \OrangeHRM\Admin\Service\PayGradeService::class,
    ];

    private const FACTORIES = [
        'core.authorization.user_role_manager' => [
            \OrangeHRM\Core\Authorization\Manager\UserRoleManagerFactory::class,
            'getNewUserRoleManager',
        ],
    ];

    public static function setUpBeforeClass(): void
    {
        TestDataService::populate(Config::get(Config::TEST_DIR) . '/phpunit/fixtures/WorkflowStateMachine.yaml', true);
    }

    protected function setUp(): void
    {
        parent::setUp();
        // banco recarregado a cada caso: nenhum teste depende de outro
        $this->populateFixtures('T2Claim.yaml', __DIR__ . '/fixtures', true);
    }

    /**
     * Executa uma operação de API como o usuário $userId e devolve ['http' => código, 'data' => ..., 'meta' => ...].
     *
     * @param class-string $apiClass
     */
    protected function call(
        int $userId,
        string $apiClass,
        string $operation,
        array $attributes = [],
        array $body = [],
        array $query = [],
        string $now = '2026-11-10 10:00:00'
    ): array {
        // Cada requisição real é um processo PHP novo: sem entidades em memória e sem o cache ESTÁTICO de
        // transições. Sem o reset, as entidades cacheadas, que EmployeeClaimRequestAPI::getAllowedActions()
        // altera (action '1' → 'SUBMIT'), vazariam para a chamada seguinte e a ação deixaria de casar.
        $this->getEntityManager()->clear();
        AccessFlowStateMachineService::resetWorkflowCache();

        $params = new TestCaseParams();
        $params->setUserId($userId);
        $params->setAttributes($attributes);
        $params->setBody($body);
        $params->setQuery($query);
        $params->setServices(self::SERVICES);
        $params->setFactories(self::FACTORIES);

        $clock = $this->getMockBuilder(DateTimeHelperService::class)->onlyMethods(['getNow'])->getMock();
        $clock->method('getNow')->willReturnCallback(fn () => new DateTime($now));

        $this->createKernelWithMockServices([
            Services::AUTH_USER => $this->getMockAuthUser($params),
            Services::DATETIME_HELPER_SERVICE => $clock,
        ]);
        $this->registerServices($params);
        $api = $this->getApiEndpointMock($apiClass, $params);

        try {
            $this->validate(
                array_merge($attributes, $query, $body),
                $api->{'getValidationRuleFor' . ucfirst($operation)}()
            );
            $result = $api->$operation();
            return [
                'http' => 200,
                'data' => $result->normalize(),
                'meta' => $result->getMeta() ? $result->getMeta()->all() : null,
            ];
        } catch (InvalidParamException $e) {
            return ['http' => 422, 'error' => $e->getMessage()];
        } catch (ForbiddenException $e) {
            return ['http' => 403, 'error' => $e->getMessage()];
        } catch (RecordNotFoundException $e) {
            return ['http' => 404, 'error' => $e->getMessage()];
        } catch (BadRequestException $e) {
            return ['http' => 400, 'error' => $e->getMessage()];
        }
    }

    /** PUT /api/v2/claim/requests/{id}/action */
    protected function action(int $userId, int $claimId, string $action, string $now = '2026-11-10 10:00:00'): array
    {
        return $this->call($userId, ClaimRequestActionAPI::class, 'update', ['requestId' => $claimId], ['action' => $action], [], $now);
    }

    /** Executa a ação e exige sucesso com o estado esperado na resposta. */
    protected function assertAction(int $userId, int $claimId, string $action, string $expectedStatus, string $now = '2026-11-10 10:00:00'): void
    {
        $response = $this->action($userId, $claimId, $action, $now);
        $this->assertSame(200, $response['http'], "$action deveria ser aceito: " . ($response['error'] ?? ''));
        $this->assertSame($expectedStatus, $response['data']['status']);
        $this->assertSame($expectedStatus, $this->dbClaim($claimId)['status'], 'estado gravado no banco');
    }

    /**
     * Ações que o usuário recebe em meta.allowedActions (é o que decide os botões da tela), em ordem alfabética.
     * O dono usa GET /claim/requests/{id}; o gestor, GET /claim/employees/{empNumber}/requests/{id}.
     */
    protected function allowedActions(int $userId, int $claimId): array
    {
        $response = $userId === self::ESS
            ? $this->call($userId, MyClaimRequestAPI::class, 'getOne', ['id' => $claimId])
            : $this->call($userId, EmployeeClaimRequestAPI::class, 'getOne', ['empNumber' => self::EMP_ESS, 'id' => $claimId]);
        $this->assertSame(200, $response['http'], 'consulta da reivindicação: ' . ($response['error'] ?? ''));
        $actions = array_column($response['meta']['allowedActions'] ?? [], 'action');
        sort($actions);
        return $actions;
    }

    /** Estado e data de submissão lidos direto da tabela, sem passar pela API nem pelo Doctrine. */
    protected function dbClaim(int $claimId): array
    {
        return $this->getEntityManager()->getConnection()->fetchAssociative(
            'SELECT status, submitted_date FROM ohrm_claim_request WHERE id = ?',
            [$claimId]
        );
    }
}
