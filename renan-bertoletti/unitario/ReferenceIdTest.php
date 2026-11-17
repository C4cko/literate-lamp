<?php

/**
 * T2 VERVAL (PUCRS): UT-01 e UT-02 do plano de testes de Claim (renan-bertoletti/plano, seção 6.1).
 * ClaimService::getReferenceId() isolado: DAO e relógio simulados, sem banco.
 * O OrangeHRM não tem nenhum teste para este método (análise #5, C4).
 */

namespace OrangeHRM\Tests\Claim\T2ClaimUnitario;

use DateTime;
use OrangeHRM\Claim\Dao\ClaimDao;
use OrangeHRM\Claim\Service\ClaimService;
use OrangeHRM\Core\Service\DateTimeHelperService;
use OrangeHRM\Framework\Services;
use OrangeHRM\Tests\Util\KernelTestCase;

/**
 * @group Claim
 * @group T2Claim
 * @group T2ClaimUnitario
 */
class ReferenceIdTest extends KernelTestCase
{
    private function referenceIdFor(int $nextId, string $now): string
    {
        $clock = $this->getMockBuilder(DateTimeHelperService::class)->onlyMethods(['getNow'])->getMock();
        $clock->method('getNow')->willReturn(new DateTime($now));
        $this->createKernelWithMockServices([Services::DATETIME_HELPER_SERVICE => $clock]);

        $dao = $this->getMockBuilder(ClaimDao::class)->onlyMethods(['getNextId'])->getMock();
        $dao->expects($this->once())->method('getNextId')->willReturn($nextId);

        $service = $this->getMockBuilder(ClaimService::class)->onlyMethods(['getClaimDao'])->getMock();
        $service->method('getClaimDao')->willReturn($dao);

        return $service->getReferenceId();
    }

    /**
     * UT-01: valor-limite do id (7 dígitos com zeros à esquerda). Caracteriza R4: a partir de 10 000 000 o
     * str_pad deixa de preencher e o número passa de 15 para 16 caracteres.
     *
     * @dataProvider idBoundaryProvider
     */
    public function testUT01IdComSeteDigitos(int $nextId, string $expected): void
    {
        $referenceId = $this->referenceIdFor($nextId, '2026-11-17 10:00:00');

        $this->assertSame($expected, $referenceId);
        $this->assertSame(strlen($expected), strlen($referenceId));
    }

    public function idBoundaryProvider(): array
    {
        return [
            'primeiro id' => [1, '202611170000001'],
            'último id com 6 dígitos' => [999999, '202611170999999'],
            'primeiro id com 7 dígitos' => [1000000, '202611171000000'],
            'último id com 7 dígitos (15 caracteres)' => [9999999, '202611179999999'],
            'primeiro id com 8 dígitos (16 caracteres, R4)' => [10000000, '2026111710000000'],
        ];
    }

    /**
     * UT-02: o prefixo é a data do DateTimeHelper (no servidor, UTC: R5), testada na virada de ano.
     *
     * @dataProvider dateBoundaryProvider
     */
    public function testUT02PrefixoSegueADataDoServidor(string $now, string $expected): void
    {
        $this->assertSame($expected, $this->referenceIdFor(7, $now));
    }

    public function dateBoundaryProvider(): array
    {
        return [
            'último segundo do ano' => ['2026-12-31 23:59:59', '202612310000007'],
            'primeiro segundo do ano seguinte' => ['2027-01-01 00:00:00', '202701010000007'],
            '29 de fevereiro (ano bissexto)' => ['2028-02-29 12:00:00', '202802290000007'],
        ];
    }
}
