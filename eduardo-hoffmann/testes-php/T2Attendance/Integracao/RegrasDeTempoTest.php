<?php

/**
 * T2 VERVAL (PUCRS): IT-06, IT-07, IT-09 e IT-11 de docs/02-casos-de-teste.md.
 * Regras de tempo do Punch In/Out com banco real: sobreposição de registros (fronteiras do intervalo),
 * saída anterior à entrada e margem de 180 segundos da hora informada.
 */

namespace OrangeHRM\Tests\Attendance\T2Attendance\Integracao;

use OrangeHRM\Attendance\Api\AttendancePunchInRecordOverlapAPI;

/**
 * @group Attendance
 * @group T2Attendance
 */
class RegrasDeTempoTest extends PunchInOutIntegrationTestCase
{
    protected function setUp(): void
    {
        parent::setUp();
        $this->carregarFixture();
    }

    /**
     * Registro concluído do funcionário 4: [2026-11-15 03:30, 2026-11-15 11:30] em UTC.
     * Nova entrada sobrepõe quando entrada <= nova < saída.
     */
    public function fronteirasDoIntervaloProvider(): array
    {
        return [
            'um minuto antes da entrada (03:29)' => ['03:29', false],
            'igual à entrada (03:30)' => ['03:30', true],
            'um minuto depois da entrada (03:31)' => ['03:31', true],
            'um minuto antes da saída (11:29)' => ['11:29', true],
            'igual à saída (11:30)' => ['11:30', false],
        ];
    }

    // ---------------------------------------------------------------- IT-06: sobreposição na entrada (AVL)

    /**
     * @dataProvider fronteirasDoIntervaloProvider
     */
    public function testIT06FronteirasDaSobreposicaoNaEntrada(string $hora, bool $sobrepoe): void
    {
        $this->ligarAlterarHoraAtual();
        $corpo = self::corpo(['date' => '2026-11-15', 'time' => $hora, 'timezoneOffset' => 0, 'timezoneName' => 'UTC']);
        $api = $this->criarApi(self::USUARIO_COM_REGISTRO_CONCLUIDO, $corpo);

        if ($sobrepoe) {
            $this->assertRecusada(fn () => $this->registrarEntrada($api, $corpo), 'Punch-In Overlap Found');
            $this->assertCount(1, $this->registrosDoFuncionario(4));
            return;
        }

        $this->registrarEntrada($api, $corpo);
        $registros = $this->registrosDoFuncionario(4);
        $this->assertCount(2, $registros);
        $this->assertSame('2026-11-15 ' . $hora . ':00', self::utc($registros[1]->getPunchInUtcTime()));
        $this->assertSame('PUNCHED IN', $registros[1]->getState());
    }

    // ---------------------------------------------------------------- IT-11: o que a tela consulta

    /**
     * @dataProvider fronteirasDoIntervaloProvider
     */
    public function testIT11ConsultaDeSobreposicaoDaEntradaDevolveValid(string $hora, bool $sobrepoe): void
    {
        $consulta = ['date' => '2026-11-15', 'time' => $hora, 'timezoneOffset' => '0'];
        $api = $this->criarApi(
            self::USUARIO_COM_REGISTRO_CONCLUIDO,
            [],
            '2026-11-16 10:00:00',
            $consulta,
            AttendancePunchInRecordOverlapAPI::class
        );

        $this->validate($consulta, $api->getValidationRuleForGetOne());
        $resultado = $api->getOne()->normalize();

        $this->assertSame(['valid' => !$sobrepoe], $resultado);
    }

    // ---------------------------------------------------------------- IT-07: saída antes da entrada (AVL)

    /**
     * A entrada aberta do funcionário 3 é 10:00 em +05:30. O relógio é fixado na própria hora informada
     * para que a regra de 180 s não interfira.
     */
    public function testIT07SaidaAnteriorAEntradaEhRecusada(): void
    {
        $corpo = self::corpo(['time' => '09:59']);
        $api = $this->criarApi(self::USUARIO_COM_ENTRADA_ABERTA, $corpo, '2026-11-16 09:59:00');

        $this->assertRecusada(
            fn () => $this->registrarSaida($api, $corpo),
            'Punch Out Time Should Be Later Than Punch In Time'
        );

        $registros = $this->registrosDoFuncionario(3);
        $this->assertSame('PUNCHED IN', $registros[0]->getState());
        $this->assertNull($registros[0]->getPunchOutUtcTime());
    }

    public function testIT07SaidaUmMinutoDepoisDaEntradaEhAceita(): void
    {
        $corpo = self::corpo(['time' => '10:01']);
        $api = $this->criarApi(self::USUARIO_COM_ENTRADA_ABERTA, $corpo, '2026-11-16 10:01:00');

        $this->registrarSaida($api, $corpo);

        $registros = $this->registrosDoFuncionario(3);
        $this->assertSame('PUNCHED OUT', $registros[0]->getState());
        $this->assertSame('2026-11-16 04:31:00', self::utc($registros[0]->getPunchOutUtcTime()));
    }

    /**
     * Caso exploratório (R3): a regra só rejeita entrada POSTERIOR à saída, então uma saída igual à entrada
     * não cai nela. O comportamento observado na execução está registrado no relatório da #17.
     */
    public function testIT07SaidaIgualAEntradaComportamentoObservado(): void
    {
        $corpo = self::corpo(['time' => '10:00']);
        $api = $this->criarApi(self::USUARIO_COM_ENTRADA_ABERTA, $corpo, '2026-11-16 10:00:00');

        $this->registrarSaida($api, $corpo);

        $registros = $this->registrosDoFuncionario(3);
        $this->assertSame('PUNCHED OUT', $registros[0]->getState());
        $this->assertSame('2026-11-16 04:30:00', self::utc($registros[0]->getPunchOutUtcTime()));
    }

    // ---------------------------------------------------------------- IT-09: margem de 180 s (AVL), opção desligada

    /**
     * @dataProvider margemProvider
     */
    public function testIT09MargemDe180SegundosComAOpcaoDesligada(string $agora, string $hora, bool $aceita): void
    {
        $corpo = self::corpo(['time' => $hora]);
        $api = $this->criarApi(self::USUARIO_SEM_REGISTROS, $corpo, $agora);

        if (!$aceita) {
            $this->assertRecusada(fn () => $this->registrarEntrada($api, $corpo), 'Provided Date And Time Invalid');
            $this->assertCount(0, $this->registrosDoFuncionario(2));
            return;
        }

        $this->registrarEntrada($api, $corpo);
        $registros = $this->registrosDoFuncionario(2);
        $this->assertCount(1, $registros);
        $this->assertSame('PUNCHED IN', $registros[0]->getState());
    }

    public function margemProvider(): array
    {
        return [
            'hora informada 179 s depois do servidor' => ['2026-11-16 10:00:01', '10:03', true],
            'hora informada 180 s depois do servidor' => ['2026-11-16 10:00:00', '10:03', false],
            'hora informada 179 s antes do servidor' => ['2026-11-16 10:02:59', '10:00', true],
            'hora informada 180 s antes do servidor' => ['2026-11-16 10:03:00', '10:00', false],
        ];
    }
}
