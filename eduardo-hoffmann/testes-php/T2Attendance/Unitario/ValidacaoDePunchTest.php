<?php

/**
 * T2 VERVAL (PUCRS): UT-03 e UT-04 de docs/02-casos-de-teste.md.
 * Regras de validação de POST /api/v2/attendance/records (MyAttendanceRecordAPI::getValidationRuleForCreate),
 * projetadas com particionamento em classes de equivalência e análise de valor-limite.
 */

namespace OrangeHRM\Tests\Attendance\T2Attendance\Unitario;

use OrangeHRM\Attendance\Api\MyAttendanceRecordAPI;
use OrangeHRM\Tests\Util\EndpointTestCase;

/**
 * @group Attendance
 * @group T2Attendance
 */
class ValidacaoDePunchTest extends EndpointTestCase
{
    private static function corpo(array $alteracoes = []): array
    {
        return array_merge([
            'date' => '2026-11-16',
            'time' => '10:00',
            'timezoneOffset' => 5.5,
            'timezoneName' => 'Asia/Kolkata',
        ], $alteracoes);
    }

    private function validar(array $corpo): bool
    {
        $this->createKernelWithMockServices();
        $api = new MyAttendanceRecordAPI($this->getRequest());
        return $this->validate($corpo, $api->getValidationRuleForCreate());
    }

    private function assertInvalido(array $corpo, array $camposInvalidos): void
    {
        $this->assertInvalidParamException(fn () => $this->validar($corpo), $camposInvalidos);
    }

    // ---------------------------------------------------------------- UT-03: nota, data e hora

    public function testUT03CorpoValidoMinimoEhAceito(): void
    {
        $this->assertTrue($this->validar(self::corpo()));
    }

    /**
     * @dataProvider notaAceitaProvider
     */
    public function testUT03NotaAceita(?string $nota): void
    {
        $this->assertTrue($this->validar(self::corpo(['note' => $nota])));
    }

    public function notaAceitaProvider(): array
    {
        return [
            'nota com 1 caractere' => [str_repeat('a', 1)],
            'nota com 250 caracteres (máx.)' => [str_repeat('a', 250)],
            'nota vazia' => [''],
            'nota nula' => [null],
        ];
    }

    public function testUT03NotaComMaisDe250CaracteresEhRejeitada(): void
    {
        $this->assertInvalido(self::corpo(['note' => str_repeat('a', 251)]), ['note']);
    }

    /**
     * @dataProvider horaAceitaProvider
     */
    public function testUT03HoraAceita(string $hora): void
    {
        $this->assertTrue($this->validar(self::corpo(['time' => $hora])));
    }

    public function horaAceitaProvider(): array
    {
        return [
            'meia-noite' => ['00:00'],
            'último minuto do dia' => ['23:59'],
        ];
    }

    /**
     * @dataProvider horaRejeitadaProvider
     */
    public function testUT03HoraRejeitada(string $hora): void
    {
        $this->assertInvalido(self::corpo(['time' => $hora]), ['time']);
    }

    public function horaRejeitadaProvider(): array
    {
        return [
            'hora 25' => ['25:00'],
            'minuto 60' => ['10:60'],
            'texto' => ['abc'],
        ];
    }

    /**
     * @dataProvider dataRejeitadaProvider
     */
    public function testUT03DataRejeitada(string $data): void
    {
        $this->assertInvalido(self::corpo(['date' => $data]), ['date']);
    }

    public function dataRejeitadaProvider(): array
    {
        return [
            'dia inexistente (30/02)' => ['2026-02-30'],
            'mês 13' => ['2026-13-01'],
            'texto' => ['hoje'],
        ];
    }

    // ---------------------------------------------------------------- UT-04: fuso horário

    /**
     * @dataProvider offsetAceitoProvider
     */
    public function testUT04OffsetAceito($offset): void
    {
        $this->assertTrue($this->validar(self::corpo(['timezoneOffset' => $offset])));
    }

    public function offsetAceitoProvider(): array
    {
        return [
            'limite inferior -12' => [-12],
            'zero' => [0],
            'meio fuso 5,5' => [5.5],
            'limite superior 14' => [14],
        ];
    }

    /**
     * @dataProvider offsetRejeitadoProvider
     */
    public function testUT04OffsetRejeitado($offset): void
    {
        $this->assertInvalido(self::corpo(['timezoneOffset' => $offset]), ['timezoneOffset']);
    }

    public function offsetRejeitadoProvider(): array
    {
        return [
            'abaixo do limite (-12,01)' => [-12.01],
            'acima do limite (14,01)' => [14.01],
            'texto' => ['abc'],
        ];
    }

    public function testUT04NomeDeFusoValidoEhAceito(): void
    {
        $this->assertTrue($this->validar(self::corpo(['timezoneName' => 'America/Sao_Paulo'])));
    }

    public function testUT04NomeDeFusoInexistenteEhRejeitado(): void
    {
        $this->assertInvalido(self::corpo(['timezoneName' => 'Foo/Bar']), ['timezoneName']);
    }
}
