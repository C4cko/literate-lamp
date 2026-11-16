<?php

/**
 * T2 VERVAL (PUCRS): IT-01 a IT-05 e IT-08 de docs/02-casos-de-teste.md.
 * Fluxo de entrada e saída pela API v2 (POST e PUT /api/v2/attendance/records), com banco real.
 * Teste de transição de estados: T1 a T6. O oráculo confere o registro gravado, não só a resposta.
 */

namespace OrangeHRM\Tests\Attendance\T2Attendance\Integracao;

/**
 * @group Attendance
 * @group T2Attendance
 */
class FluxoDePunchTest extends PunchInOutIntegrationTestCase
{
    protected function setUp(): void
    {
        parent::setUp();
        $this->carregarFixture();
    }

    // ---------------------------------------------------------------- IT-01: T1, INITIAL -> PUNCHED IN

    public function testIT01PrimeiraEntradaCriaRegistroPunchedIn(): void
    {
        $corpo = self::corpo(['note' => 'entrada']);
        $api = $this->criarApi(self::USUARIO_SEM_REGISTROS, $corpo, '2026-11-16 10:00:00');

        $this->registrarEntrada($api, $corpo);

        $registros = $this->registrosDoFuncionario(2);
        $this->assertCount(1, $registros);
        $registro = $registros[0];
        $this->assertSame('PUNCHED IN', $registro->getState());
        $this->assertSame('2026-11-16 04:30:00', self::utc($registro->getPunchInUtcTime()));
        $this->assertSame('2026-11-16 10:00:00', self::utc($registro->getPunchInUserTime()));
        $this->assertSame('entrada', $registro->getPunchInNote());
        $this->assertSame('5.5', $registro->getPunchInTimeOffset());
        $this->assertSame('Asia/Kolkata', $registro->getPunchInTimezoneName());
        $this->assertNull($registro->getPunchOutUtcTime());
    }

    // ---------------------------------------------------------------- IT-02: T2, PUNCHED IN -> PUNCHED OUT

    public function testIT02SaidaConcluiOMesmoRegistro(): void
    {
        $corpo = self::corpo(['time' => '12:00', 'note' => 'saida']);
        $api = $this->criarApi(self::USUARIO_COM_ENTRADA_ABERTA, $corpo, '2026-11-16 12:00:00');

        $this->registrarSaida($api, $corpo);

        $registros = $this->registrosDoFuncionario(3);
        $this->assertCount(1, $registros);
        $registro = $registros[0];
        $this->assertSame('PUNCHED OUT', $registro->getState());
        $this->assertSame('2026-11-16 06:30:00', self::utc($registro->getPunchOutUtcTime()));
        $this->assertSame('2026-11-16 12:00:00', self::utc($registro->getPunchOutUserTime()));
        $this->assertSame('saida', $registro->getPunchOutNote());
        // a entrada não muda
        $this->assertSame('2026-11-16 04:30:00', self::utc($registro->getPunchInUtcTime()));
        $this->assertSame('entrada aberta', $registro->getPunchInNote());
    }

    // ---------------------------------------------------------------- IT-03: T3, PUNCHED OUT -> novo PUNCHED IN

    public function testIT03NovaEntradaDepoisDeSaidaCriaOutroRegistro(): void
    {
        $corpo = self::corpo();
        $api = $this->criarApi(self::USUARIO_COM_REGISTRO_CONCLUIDO, $corpo, '2026-11-16 10:00:00');

        $this->registrarEntrada($api, $corpo);

        $registros = $this->registrosDoFuncionario(4);
        $this->assertCount(2, $registros);
        // o registro antigo continua igual
        $this->assertSame('PUNCHED OUT', $registros[0]->getState());
        $this->assertSame('2026-11-15 11:30:00', self::utc($registros[0]->getPunchOutUtcTime()));
        // o novo está aberto
        $this->assertSame('PUNCHED IN', $registros[1]->getState());
        $this->assertSame('2026-11-16 04:30:00', self::utc($registros[1]->getPunchInUtcTime()));
        $this->assertNull($registros[1]->getPunchOutUtcTime());
    }

    // ---------------------------------------------------------------- IT-04: T4, entrada com registro aberto

    public function testIT04EntradaComRegistroAbertoEhRecusadaESemEfeito(): void
    {
        $corpo = self::corpo();
        $api = $this->criarApi(self::USUARIO_COM_ENTRADA_ABERTA, $corpo, '2026-11-16 10:00:00');

        $this->assertRecusada(
            fn () => $this->registrarEntrada($api, $corpo),
            'Cannot Proceed Punch In Employee Already Punched In'
        );

        $registros = $this->registrosDoFuncionario(3);
        $this->assertCount(1, $registros);
        $this->assertSame('PUNCHED IN', $registros[0]->getState());
    }

    // ---------------------------------------------------------------- IT-05: T5 e T6, saída sem entrada aberta

    /**
     * @dataProvider saidaSemEntradaAbertaProvider
     */
    public function testIT05SaidaSemEntradaAbertaEhRecusadaESemEfeito(int $usuario, int $registrosEsperados): void
    {
        $corpo = self::corpo(['time' => '12:00']);
        $api = $this->criarApi($usuario, $corpo, '2026-11-16 12:00:00');

        $this->assertRecusada(
            fn () => $this->registrarSaida($api, $corpo),
            'Cannot Proceed Punch Out Employee Already Punched Out'
        );

        $registros = $this->registrosDoFuncionario($usuario);
        $this->assertCount($registrosEsperados, $registros);
        foreach ($registros as $registro) {
            $this->assertSame('PUNCHED OUT', $registro->getState());
            $this->assertSame('2026-11-15 11:30:00', self::utc($registro->getPunchOutUtcTime()));
        }
    }

    public function saidaSemEntradaAbertaProvider(): array
    {
        return [
            'T6: funcionário sem nenhum registro (INITIAL)' => [self::USUARIO_SEM_REGISTROS_PARA_SAIDA, 0],
            'T5: último registro já está PUNCHED OUT' => [self::USUARIO_COM_REGISTRO_CONCLUIDO, 1],
        ];
    }

    // ---------------------------------------------------------------- IT-08: A1 e A2, hora retroativa e fusos

    /**
     * @dataProvider fusosProvider
     */
    public function testIT08HoraRetroativaEFusosGravamUtcCorreto(
        float $offset,
        string $nomeDoFuso,
        string $utcEsperado
    ): void {
        $this->ligarAlterarHoraAtual();
        $corpo = self::corpo([
            'date' => '2026-11-10',
            'time' => '10:00',
            'timezoneOffset' => $offset,
            'timezoneName' => $nomeDoFuso,
        ]);
        $api = $this->criarApi(self::USUARIO_SEM_REGISTROS, $corpo, '2026-11-16 10:00:00');

        $this->registrarEntrada($api, $corpo);

        $registros = $this->registrosDoFuncionario(2);
        $this->assertCount(1, $registros);
        $this->assertSame('2026-11-10 10:00:00', self::utc($registros[0]->getPunchInUserTime()));
        $this->assertSame($utcEsperado, self::utc($registros[0]->getPunchInUtcTime()));
        $this->assertSame($nomeDoFuso, $registros[0]->getPunchInTimezoneName());
    }

    public function fusosProvider(): array
    {
        return [
            'fuso -12 (limite inferior)' => [-12.0, 'Etc/GMT+12', '2026-11-10 22:00:00'],
            'fuso +5,5' => [5.5, 'Asia/Kolkata', '2026-11-10 04:30:00'],
            'fuso +14 (limite superior)' => [14.0, 'Pacific/Kiritimati', '2026-11-09 20:00:00'],
        ];
    }
}
