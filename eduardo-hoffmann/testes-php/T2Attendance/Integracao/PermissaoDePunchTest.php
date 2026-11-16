<?php

/**
 * T2 VERVAL (PUCRS): IT-10 de docs/02-casos-de-teste.md (risco R1).
 *
 * Em MyAttendanceRecordAPI, userAllowedPunchInActions() calcula $allowed com in_array(), mas não usa o resultado
 * nem lança exceção. A hipótese é que a checagem de permissão não tem efeito nesse endpoint. Este teste retira do
 * banco as ações de Attendance do papel ESS e tenta registrar entrada: o esperado, para um papel sem a ação
 * Punch In, é Forbidden (como faz EmployeeAttendanceRecordAPI::userAllowedPunchInActions).
 */

namespace OrangeHRM\Tests\Attendance\T2Attendance\Integracao;

use OrangeHRM\Core\Api\V2\Exception\ForbiddenException;
use OrangeHRM\Core\Service\AccessFlowStateMachineService;
use OrangeHRM\Entity\WorkflowStateMachine;
use OrangeHRM\Tests\Util\TestDataService;

/**
 * @group Attendance
 * @group T2Attendance
 * @group defeito-conhecido
 */
class PermissaoDePunchTest extends PunchInOutIntegrationTestCase
{
    protected function setUp(): void
    {
        parent::setUp();
        $this->carregarFixture();
    }

    public function testIT10EntradaSemAcaoPunchInNoPapelDeveSerProibida(): void
    {
        TestDataService::truncateSpecificTables([WorkflowStateMachine::class]);
        AccessFlowStateMachineService::resetWorkflowCache();

        $corpo = self::corpo();
        $api = $this->criarApi(self::USUARIO_SEM_REGISTROS, $corpo, '2026-11-16 10:00:00');

        try {
            $this->registrarEntrada($api, $corpo);
        } catch (ForbiddenException $e) {
            $this->assertCount(0, $this->registrosDoFuncionario(2));
            return;
        }

        $this->fail(
            'R1: o papel ESS não tem a ação Punch In no banco, mas a entrada foi aceita e gravou '
            . count($this->registrosDoFuncionario(2)) . ' registro(s).'
        );
    }
}
