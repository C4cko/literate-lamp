<?php

/**
 * T2 VERVAL (PUCRS): UT-07 de docs/casos-de-teste.md.
 * IDGeneratorService isolado com o DAO mockado. Cobre o que o IDGeneratorServiceTest original não cobre:
 * getNextID com $update = false (o modo usado pelo formulário), as transições de quantidade de dígitos e
 * o ARGUMENTO gravado por incrementId.
 */

namespace OrangeHRM\Tests\Pim\T2AddEmployee;

use OrangeHRM\Core\Dao\IDGeneratorDao;
use OrangeHRM\Core\Helper\ClassHelper;
use OrangeHRM\Core\Service\IDGeneratorService;
use OrangeHRM\Entity\Employee;
use OrangeHRM\Framework\Services;
use OrangeHRM\Tests\Util\KernelTestCase;

/**
 * @group Pim
 * @group T2AddEmployee
 */
class EmployeeIdGeneratorTest extends KernelTestCase
{
    protected function setUp(): void
    {
        parent::setUp();
        $this->createKernelWithMockServices([Services::CLASS_HELPER => new ClassHelper()]);
    }

    private function serviceWithCurrentId(int $currentId, bool $expectUpdate, ?int $expectedNextId = null): IDGeneratorService
    {
        $dao = $this->getMockBuilder(IDGeneratorDao::class)
            ->onlyMethods(['getCurrentID', 'updateNextId'])
            ->getMock();
        $dao->method('getCurrentID')->willReturn($currentId);
        if ($expectUpdate) {
            $dao->expects($this->once())
                ->method('updateNextId')
                ->with(Employee::class, $expectedNextId)
                ->willReturn(1);
        } else {
            $dao->expects($this->never())->method('updateNextId');
        }

        $service = new IDGeneratorService();
        $service->setIDGeneratorDao($dao);
        return $service;
    }

    /**
     * @dataProvider digitBoundaryProvider
     */
    public function testUT07IdSugeridoTemNoMinimoQuatroDigitos(int $currentId, string $expected): void
    {
        $service = $this->serviceWithCurrentId($currentId, false);
        $this->assertSame($expected, $service->getNextID(Employee::class, false));
    }

    public function digitBoundaryProvider(): array
    {
        return [
            'primeiro funcionário' => [0, '0001'],
            'último com 3 dígitos significativos' => [998, '0999'],
            'transição para 4 dígitos' => [999, '1000'],
            'último com 4 dígitos' => [9998, '9999'],
            'transição para 5 dígitos (passa do mínimo)' => [9999, '10000'],
        ];
    }

    public function testUT07SugerirIdNaoAlteraOContador(): void
    {
        // $update = false: abrir o formulário não pode "gastar" um ID (o mock exige never())
        $service = $this->serviceWithCurrentId(41, false);
        $this->assertSame('0042', $service->getNextID(Employee::class, false));
    }

    public function testUT07IncrementIdGravaOValorAtualMaisUm(): void
    {
        // o teste original só exigia que updateNextId fosse chamado; aqui conferimos o valor gravado
        $service = $this->serviceWithCurrentId(41, true, 42);
        $service->incrementId(Employee::class);
    }
}
