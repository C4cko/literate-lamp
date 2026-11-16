<?php

/**
 * T2 VERVAL (PUCRS): base comum dos testes de integração de Punch In / Punch Out (IT-01 a IT-11).
 *
 * Reaproveita a infraestrutura de teste do OrangeHRM (kernel, banco de teste e fixtures YAML), como em
 * test/Api/MyAttendanceRecordAPITest.php, mas o oráculo é diferente: além da resposta, cada teste confere o
 * estado gravado em ohrm_attendance_record.
 */

namespace OrangeHRM\Tests\Attendance\T2Attendance\Integracao;

use Closure;
use DateTime;
use DateTimeZone;
use OrangeHRM\Admin\Service\UserService;
use OrangeHRM\Attendance\Api\MyAttendanceRecordAPI;
use OrangeHRM\Attendance\Service\AttendanceService;
use OrangeHRM\Core\Api\V2\Exception\BadRequestException;
use OrangeHRM\Core\Authorization\Manager\UserRoleManagerFactory;
use OrangeHRM\Core\Service\AccessFlowStateMachineService;
use OrangeHRM\Core\Service\ConfigService;
use OrangeHRM\Core\Service\DateTimeHelperService;
use OrangeHRM\Entity\AttendanceRecord;
use OrangeHRM\Entity\WorkflowStateMachine;
use OrangeHRM\Framework\Services;
use OrangeHRM\Pim\Service\EmployeeService;
use OrangeHRM\Tests\Util\EndpointIntegrationTestCase;
use OrangeHRM\Tests\Util\Integration\TestCaseParams;

abstract class PunchInOutIntegrationTestCase extends EndpointIntegrationTestCase
{
    /** Usuários da fixture: o número do usuário é o mesmo do funcionário. */
    public const USUARIO_SEM_REGISTROS = 2;
    public const USUARIO_COM_ENTRADA_ABERTA = 3;
    public const USUARIO_COM_REGISTRO_CONCLUIDO = 4;
    public const USUARIO_SEM_REGISTROS_PARA_SAIDA = 5;

    /** Corpo válido padrão: 10:00 em +05:30 (04:30 UTC). */
    public const DATA = '2026-11-16';
    public const FUSO_OFFSET = 5.5;
    public const FUSO_NOME = 'Asia/Kolkata';

    protected function carregarFixture(): void
    {
        $this->populateFixtures('PunchInOut.yaml', __DIR__ . '/../fixtures', true);
    }

    /**
     * Acrescenta a linha de fluxo que representa a opção "Employee can change current time when punching in/out"
     * (ESS, INITIAL, ação EDIT_PUNCH_TIME), como a própria tela de configuração faz.
     */
    protected function ligarAlterarHoraAtual(): void
    {
        $linha = new WorkflowStateMachine();
        $linha->setWorkflow(WorkflowStateMachine::FLOW_ATTENDANCE);
        $linha->setState(AttendanceRecord::STATE_INITIAL);
        $linha->setRole(AttendanceService::ESS_USER);
        $linha->setAction(WorkflowStateMachine::ATTENDANCE_ACTION_EDIT_PUNCH_TIME);
        $linha->setResultingState(AttendanceRecord::STATE_INITIAL);
        $this->getEntityManager()->persist($linha);
        $this->getEntityManager()->flush($linha);
        AccessFlowStateMachineService::resetWorkflowCache();
    }

    /**
     * Cria o endpoint como o OrangeHRM faz nos próprios testes: kernel com usuário autenticado simulado e os
     * serviços reais usados por MyAttendanceRecordAPI. O relógio é fixo: $agora é a hora de parede no fuso de
     * $corpo['timezoneOffset'] (ou em $fusoDoRelogio, para as chamadas de consulta).
     */
    protected function criarApi(
        int $usuario,
        array $corpo = [],
        string $agora = '2026-11-16 10:00:00',
        array $consulta = [],
        string $classe = MyAttendanceRecordAPI::class
    ) {
        $parametros = new TestCaseParams();
        $parametros->setUserId($usuario);
        $parametros->setBody($corpo);
        $parametros->setQuery($consulta);
        $parametros->setServices([
            'attendance.attendance_service' => AttendanceService::class,
            'core.config_service' => ConfigService::class,
            'admin.user_service' => UserService::class,
            'pim.employee_service' => EmployeeService::class,
        ]);
        $parametros->setFactories([
            'core.authorization.user_role_manager' => [UserRoleManagerFactory::class, 'getNewUserRoleManager'],
        ]);

        $relogio = $this->getMockBuilder(DateTimeHelperService::class)->onlyMethods(['getNow'])->getMock();
        $relogio->method('getNow')
            ->willReturnCallback(fn (?DateTimeZone $fuso = null) => new DateTime($agora, $fuso));

        $this->createKernelWithMockServices([
            Services::AUTH_USER => $this->getMockAuthUser($parametros),
            Services::DATETIME_HELPER_SERVICE => $relogio,
        ]);
        $this->registerServices($parametros);

        return $this->getApiEndpointMock($classe, $parametros);
    }

    /** Corpo válido padrão (10:00 em +05:30), com os campos alterados por $alteracoes. */
    protected static function corpo(array $alteracoes = []): array
    {
        return array_merge([
            'date' => self::DATA,
            'time' => '10:00',
            'timezoneOffset' => self::FUSO_OFFSET,
            'timezoneName' => self::FUSO_NOME,
        ], $alteracoes);
    }

    /** Entrada (POST): valida como o framework faz antes de chamar create(). */
    protected function registrarEntrada(MyAttendanceRecordAPI $api, array $corpo)
    {
        $this->validate($corpo, $api->getValidationRuleForCreate());
        return $api->create();
    }

    /** Saída (PUT): valida como o framework faz antes de chamar update(). */
    protected function registrarSaida(MyAttendanceRecordAPI $api, array $corpo)
    {
        $this->validate($corpo, $api->getValidationRuleForUpdate());
        return $api->update();
    }

    /** Exige que a operação seja recusada com BadRequest e a mensagem documentada. */
    protected function assertRecusada(Closure $operacao, string $mensagem): void
    {
        try {
            $operacao();
        } catch (BadRequestException $e) {
            $this->assertSame($mensagem, $e->getMessage());
            return;
        }
        $this->fail("A operação deveria ser recusada com \"$mensagem\", mas foi aceita.");
    }

    /** @return AttendanceRecord[] registros do funcionário, do mais antigo para o mais novo */
    protected function registrosDoFuncionario(int $empNumber): array
    {
        $this->getEntityManager()->clear();
        return $this->getEntityManager()
            ->getRepository(AttendanceRecord::class)
            ->findBy(['employee' => $empNumber], ['id' => 'ASC']);
    }

    protected static function utc(?DateTime $data): ?string
    {
        return $data?->format('Y-m-d H:i:s');
    }
}
