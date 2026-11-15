<?php

use OrangeHRM\Leave\Service\LeaveRequestService;
use PHPUnit\Framework\TestCase;

final class LeaveRequestStatusTest extends TestCase
{
    public function testConverteVariosStatusMantendoAOrdem(): void
    {
        $service = $this->getMockBuilder(LeaveRequestService::class)
            ->onlyMethods(['getAllLeaveStatusesAssoc'])
            ->getMock();

        $service->expects($this->once())
            ->method('getAllLeaveStatusesAssoc')
            ->willReturn([
                1 => 'PENDING APPROVAL',
                2 => 'SCHEDULED',
            ]);

        $resultado = $service->getLeaveStatusesByNames([
            'SCHEDULED',
            'PENDING APPROVAL',
        ]);

        $this->assertSame([2, 1], $resultado);
    }

    public function testRejeitaNomeDeStatusInexistente(): void
    {
        $service = $this->getMockBuilder(LeaveRequestService::class)
            ->onlyMethods(['getAllLeaveStatusesAssoc'])
            ->getMock();

        $service->expects($this->once())
            ->method('getAllLeaveStatusesAssoc')
            ->willReturn([
                1 => 'PENDING APPROVAL',
                2 => 'SCHEDULED',
            ]);

        $this->expectException(InvalidArgumentException::class);
        $service->getLeaveStatusesByNames(['STATUS INEXISTENTE']);
    }
}
