<?php

use OrangeHRM\Entity\LeaveEntitlement;
use OrangeHRM\Leave\Dao\LeaveEntitlementDao;
use PHPUnit\Framework\TestCase;

final class LeaveEntitlementDaoIntegrationTest extends TestCase
{
    public function testFiltraPorColaboradorTipoEPeriodo(): void
    {
        $em = require __DIR__ . '/bootstrap-integration.php';
        $connection = $em->getConnection();

        try {
            $this->assertSame(
                'orangehrm_fernando_test',
                $connection->fetchOne('SELECT DATABASE()')
            );

            // Tabela temporária: existe apenas nesta conexão.
            // Contém os campos utilizados pelo mapeamento da entidade.
            $connection->executeStatement('
                CREATE TEMPORARY TABLE ohrm_leave_entitlement (
                    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                    emp_number INT NOT NULL,
                    leave_type_id INT NOT NULL,
                    no_of_days DECIMAL(19,15) NOT NULL,
                    days_used DECIMAL(8,4) NOT NULL DEFAULT 0,
                    from_date DATETIME NOT NULL,
                    to_date DATETIME NOT NULL,
                    credited_date DATETIME NULL,
                    note VARCHAR(255) NOT NULL,
                    entitlement_type INT NULL,
                    deleted TINYINT(1) NOT NULL DEFAULT 0,
                    created_by_id INT NULL
                )
            ');

            // ID, colaborador, tipo, início e fim da validade.
            $dados = [
                [1, 101, 1, '2026-01-01', '2026-12-31'],
                [2, 102, 1, '2026-01-01', '2026-12-31'],
                [3, 101, 2, '2026-01-01', '2026-12-31'],
                [4, 101, 1, '2025-01-01', '2025-12-31'],
            ];

            foreach ($dados as [$id, $colaborador, $tipo, $inicio, $fim]) {
                $connection->insert('ohrm_leave_entitlement', [
                    'id' => $id,
                    'emp_number' => $colaborador,
                    'leave_type_id' => $tipo,
                    'no_of_days' => 2,
                    'days_used' => 0,
                    'from_date' => $inicio,
                    'to_date' => $fim,
                    'credited_date' => $inicio,
                    'note' => 'Fixture CT-INT-01',
                    'deleted' => 0,
                ]);
            }

            $dao = new LeaveEntitlementDao();

            $resultados = $dao->getValidLeaveEntitlements(
                101,
                1,
                new DateTime('2026-11-30'),
                new DateTime('2026-11-30'),
                'le.id',
                'ASC'
            );

            $ids = array_map(
                static fn (LeaveEntitlement $entitlement): int =>
                    $entitlement->getId(),
                $resultados
            );

            // Somente o registro do colaborador, tipo e período corretos.
            $this->assertSame([1], $ids);
        } finally {
            $em->clear();
            $connection->close();
        }
    }
}