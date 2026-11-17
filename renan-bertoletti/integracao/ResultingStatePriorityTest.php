<?php

/**
 * T2 VERVAL (PUCRS): IT-12 do plano de testes de Claim (seção 6.2), risco R8.
 *
 * Quando o Admin submete a PRÓPRIA reivindicação, duas transições casam com SUBMIT em INITIATED:
 * ESS USER → SUBMITTED (priority 10) e ADMIN → PAID (priority 0). O plano supôs que o resultado dependia da ordem
 * das linhas; a leitura de BasicUserRoleManager::getUniqueActionsBasedOnPriority() mostrou que o empate é resolvido
 * pela coluna priority. O teste verifica as duas ordens de linha: se o resultado for SUBMITTED nas duas, R8 é
 * descartado.
 */

namespace OrangeHRM\Tests\Claim\T2ClaimIntegracao;

use OrangeHRM\Config\Config;
use OrangeHRM\Tests\Util\TestDataService;

/**
 * @group Claim
 * @group T2Claim
 * @group T2ClaimIntegracao
 */
class ResultingStatePriorityTest extends ClaimIntegrationTestCase
{
    protected function tearDown(): void
    {
        // a tabela de transições é compartilhada pelas outras classes: volta ao estado da migração
        TestDataService::populate(Config::get(Config::TEST_DIR) . '/phpunit/fixtures/WorkflowStateMachine.yaml', true);
        parent::tearDown();
    }

    /** Ordem das duas linhas candidatas como o banco as devolve (por id). */
    private function candidateRows(): array
    {
        return $this->getEntityManager()->getConnection()->fetchAllAssociative(
            "SELECT id, role, resulting_state, priority FROM ohrm_workflow_state_machine
             WHERE workflow = '7' AND state = 'INITIATED' AND action = '1' AND role IN ('ESS USER', 'ADMIN')
             ORDER BY id"
        );
    }

    public function testIT12AdminSubmeteAPropriaNaOrdemPadrao(): void
    {
        $this->assertSame(['ESS USER', 'ADMIN'], array_column($this->candidateRows(), 'role'), 'pré-condição');

        $this->assertAction(self::ADMIN, self::DO_PROPRIO_ADMIN, 'SUBMIT', 'SUBMITTED');
    }

    public function testIT12AdminSubmeteAPropriaComAsLinhasEmOrdemInversa(): void
    {
        // recria a linha do ESS com id maior: agora a linha do ADMIN vem primeiro
        $connection = $this->getEntityManager()->getConnection();
        $connection->executeStatement(
            "UPDATE ohrm_workflow_state_machine SET id = 1000
             WHERE workflow = '7' AND state = 'INITIATED' AND action = '1' AND role = 'ESS USER'"
        );
        $this->assertSame(['ADMIN', 'ESS USER'], array_column($this->candidateRows(), 'role'), 'pré-condição');

        $this->assertAction(self::ADMIN, self::DO_PROPRIO_ADMIN, 'SUBMIT', 'SUBMITTED');
    }
}
