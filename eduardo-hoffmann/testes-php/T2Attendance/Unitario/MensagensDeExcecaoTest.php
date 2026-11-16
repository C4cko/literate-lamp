<?php

/**
 * T2 VERVAL (PUCRS): UT-05 de docs/02-casos-de-teste.md.
 * Cada método de fábrica de AttendanceServiceException produz exatamente a mensagem documentada na jornada.
 */

namespace OrangeHRM\Tests\Attendance\T2Attendance\Unitario;

use OrangeHRM\Attendance\Exception\AttendanceServiceException;
use PHPUnit\Framework\TestCase;

/**
 * @group Attendance
 * @group T2Attendance
 */
class MensagensDeExcecaoTest extends TestCase
{
    /**
     * @dataProvider mensagensProvider
     */
    public function testUT05MensagemDaExcecao(string $metodo, string $mensagem): void
    {
        $excecao = AttendanceServiceException::$metodo();

        $this->assertInstanceOf(AttendanceServiceException::class, $excecao);
        $this->assertSame($mensagem, $excecao->getMessage());
    }

    public function mensagensProvider(): array
    {
        return [
            'saída sem entrada aberta' => ['punchOutAlreadyExist', 'Cannot Proceed Punch Out Employee Already Punched Out'],
            'entrada com entrada aberta' => ['punchInAlreadyExist', 'Cannot Proceed Punch In Employee Already Punched In'],
            'saída antes da entrada' => ['punchOutTimeBehindThanPunchInTime', 'Punch Out Time Should Be Later Than Punch In Time'],
            'sobreposição na entrada' => ['punchInOverlapFound', 'Punch-In Overlap Found'],
            'sobreposição na saída' => ['punchOutOverlapFound', 'Punch-Out Overlap Found'],
            'data e hora inválidas' => ['invalidDateTime', 'Provided Date And Time Invalid'],
            'saída sem data e hora' => ['punchOutDateTimeNull', 'Punch Out Date And Time Should Not Be Null'],
            'nenhum id para excluir' => ['deletableAttendanceRecordIdsEmpty', 'No IDs Found'],
            'fuso inválido' => ['invalidTimezoneDetails', 'Valid Timezone Offset and Timezone Name Must Be Provided'],
        ];
    }
}
