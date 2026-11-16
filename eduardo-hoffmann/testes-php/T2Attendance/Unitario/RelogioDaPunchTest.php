<?php

/**
 * T2 VERVAL (PUCRS): UT-01 e UT-02 de docs/02-casos-de-teste.md.
 * Regra da hora informada no Punch In/Out de MyAttendanceRecordAPI quando a configuração
 * "Employee can change current time when punching in/out" está desligada (margem de 180 segundos).
 */

namespace OrangeHRM\Tests\Attendance\T2Attendance\Unitario;

use DateTime;
use DateTimeZone;
use OrangeHRM\Attendance\Api\MyAttendanceRecordAPI;
use OrangeHRM\Attendance\Exception\AttendanceServiceException;
use OrangeHRM\Attendance\Service\AttendanceService;
use OrangeHRM\Core\Service\DateTimeHelperService;
use OrangeHRM\Framework\Services;
use OrangeHRM\Tests\Util\EndpointTestCase;
use ReflectionMethod;

/**
 * @group Attendance
 * @group T2Attendance
 */
class RelogioDaPunchTest extends EndpointTestCase
{
    /** Hora do servidor simulada (no fuso informado pelo teste). */
    private const AGORA = '2026-11-16 10:00:00';

    /**
     * Cria a API com o relógio fixo e a configuração simulada, sem tocar no banco.
     */
    private function criarApi(bool $usuarioPodeAlterarHora): MyAttendanceRecordAPI
    {
        $relogio = $this->getMockBuilder(DateTimeHelperService::class)->onlyMethods(['getNow'])->getMock();
        $relogio->method('getNow')
            ->willReturnCallback(fn (?DateTimeZone $fuso = null) => new DateTime(self::AGORA, $fuso));

        $servico = $this->getMockBuilder(AttendanceService::class)
            ->onlyMethods(['canUserChangeCurrentTime'])
            ->getMock();
        $servico->method('canUserChangeCurrentTime')->willReturn($usuarioPodeAlterarHora);

        $this->createKernelWithMockServices([
            Services::DATETIME_HELPER_SERVICE => $relogio,
            Services::ATTENDANCE_SERVICE => $servico,
        ]);

        return new MyAttendanceRecordAPI($this->getRequest());
    }

    /** Chama um método protegido da API. */
    private function chamar(object $objeto, string $metodo, array $argumentos)
    {
        return (new ReflectionMethod($objeto, $metodo))->invoke($objeto, ...$argumentos);
    }

    // ---------------------------------------------------------------- UT-01: margem de 180 s (AVL)

    /**
     * @dataProvider margemProvider
     */
    public function testUT01MargemDe180Segundos(int $deslocamentoEmSegundos, bool $esperado): void
    {
        $api = $this->criarApi(false);
        $fuso = new DateTimeZone('+0530');
        $informada = (new DateTime(self::AGORA, $fuso))
            ->modify(sprintf('%+d seconds', $deslocamentoEmSegundos))
            ->format('Y-m-d H:i:s');

        $this->assertSame($esperado, $this->chamar($api, 'isCurrantDateTimeValid', [$informada, $fuso]));
    }

    public function margemProvider(): array
    {
        return [
            'igual à hora do servidor (0 s)' => [0, true],
            '179 s depois' => [179, true],
            '179 s antes' => [-179, true],
            '180 s depois (limite)' => [180, false],
            '180 s antes (limite)' => [-180, false],
        ];
    }

    // ---------------------------------------------------------------- UT-02: configuração x hora (TD)

    public function testUT02ComConfiguracaoLigadaQualquerHoraEhAceita(): void
    {
        $api = $this->criarApi(true);

        $resultado = $this->chamar($api, 'extractPunchDateTime', ['2026-11-01 03:00', 5.5]);

        $this->assertSame('2026-11-01 03:00', $resultado->format('Y-m-d H:i'));
        $this->assertSame('+05:30', $resultado->format('P'));
    }

    public function testUT02ComConfiguracaoDesligadaHoraDentroDaMargemEhAceita(): void
    {
        $api = $this->criarApi(false);

        // a hora do servidor é 10:00:00, então 10:02 está a 120 s de distância
        $resultado = $this->chamar($api, 'extractPunchDateTime', ['2026-11-16 10:02', 5.5]);

        $this->assertSame('2026-11-16 10:02', $resultado->format('Y-m-d H:i'));
    }

    public function testUT02ComConfiguracaoDesligadaHoraForaDaMargemEhRecusada(): void
    {
        $api = $this->criarApi(false);

        $this->expectException(AttendanceServiceException::class);
        $this->expectExceptionMessage('Provided Date And Time Invalid');

        // 10:03 está a exatamente 180 s do servidor (10:00:00)
        $this->chamar($api, 'extractPunchDateTime', ['2026-11-16 10:03', 5.5]);
    }
}
