<?php

/**
 * T2 VERVAL (PUCRS): IT-15 a IT-26 do plano de testes de Claim (seções 4 e 6.2).
 * Entradas da reivindicação, das despesas e do anexo, pela API e com o banco conferido:
 * particionamento em classes de equivalência (V/I da seção 4.2) e valor-limite (seção 4.3).
 */

namespace OrangeHRM\Tests\Claim\T2ClaimIntegracao;

use OrangeHRM\Claim\Api\ClaimAttachmentAPI;
use OrangeHRM\Claim\Api\ClaimExpenseAPI;
use OrangeHRM\Claim\Api\EmployeeClaimRequestAPI;
use OrangeHRM\Claim\Api\MyClaimRequestAPI;

/**
 * @group Claim
 * @group T2Claim
 * @group T2ClaimIntegracao
 */
class InputValidationTest extends ClaimIntegrationTestCase
{
    private function rows(string $table): int
    {
        return (int) $this->getEntityManager()->getConnection()->fetchOne("SELECT COUNT(*) FROM $table");
    }

    private function createClaim(array $body, string $now = '2026-11-10 10:00:00'): array
    {
        return $this->call(self::ESS, MyClaimRequestAPI::class, 'create', [], $body, [], $now);
    }

    private function createExpense(array $override, int $claimId = self::INITIATED): array
    {
        $body = array_filter(
            array_replace(['expenseTypeId' => 1, 'date' => '2026-11-05', 'amount' => 10.5], $override),
            fn ($value) => $value !== null // null = parâmetro ausente
        );
        return $this->call(self::ESS, ClaimExpenseAPI::class, 'create', ['requestId' => $claimId], $body);
    }

    private function assertHttp(int $expected, array $response): void
    {
        $this->assertSame($expected, $response['http'], 'HTTP inesperado' . (isset($response['error']) ? ': ' . $response['error'] : ' (a API aceitou a requisição)'));
    }

    // ---------------------------------------------------------------- IT-15: evento e moeda

    /**
     * @dataProvider eventCurrencyProvider
     */
    public function testIT15EventoEMoeda(?int $eventId, ?string $currency, int $expectedHttp): void
    {
        $before = $this->rows('ohrm_claim_request');
        $body = array_filter(['claimEventId' => $eventId, 'currencyId' => $currency], fn ($v) => $v !== null);

        $response = $this->createClaim($body);

        $this->assertHttp($expectedHttp, $response);
        $this->assertSame($before + ($expectedHttp === 200 ? 1 : 0), $this->rows('ohrm_claim_request'));
    }

    public function eventCurrencyProvider(): array
    {
        return [
            'V1+V2: evento ativo e USD' => [1, 'USD', 200],
            'I1: evento ausente' => [null, 'USD', 422],
            'I2: evento inativo' => [2, 'USD', 422],
            'I3: evento inexistente' => [99, 'USD', 422],
            'I4: evento 0' => [0, 'USD', 422],
            'I5: moeda ausente' => [1, null, 422],
            'I6: moeda inexistente (XYZ)' => [1, 'XYZ', 422],
        ];
    }

    // ---------------------------------------------------------------- IT-16: remarks

    /**
     * @dataProvider remarksProvider
     */
    public function testIT16Remarks(?int $length, int $expectedHttp): void
    {
        $body = ['claimEventId' => 1, 'currencyId' => 'USD'];
        if ($length !== null) {
            $body['remarks'] = str_repeat('r', $length);
        }

        $response = $this->createClaim($body);

        $this->assertHttp($expectedHttp, $response);
        if ($expectedHttp === 200) {
            $saved = $this->getEntityManager()->getConnection()->fetchOne(
                'SELECT description FROM ohrm_claim_request WHERE id = ?',
                [$response['data']['id']]
            );
            $this->assertSame($length === null ? null : $length, $saved === null ? null : strlen($saved));
        }
    }

    public function remarksProvider(): array
    {
        return [
            'V3: ausente' => [null, 200],
            'V4: 1 caractere' => [1, 200],
            'V4: 1000 caracteres' => [1000, 200],
            'I7: 1001 caracteres' => [1001, 422],
        ];
    }

    // ---------------------------------------------------------------- IT-17: valor na criação da despesa

    /**
     * @dataProvider createAmountProvider
     */
    public function testIT17ValorNaCriacao($amount, int $expectedHttp): void
    {
        $before = $this->rows('ohrm_expense');

        $response = $this->createExpense(['amount' => $amount]);

        $this->assertHttp($expectedHttp, $response);
        if ($expectedHttp === 200) {
            $saved = $this->getEntityManager()->getConnection()->fetchOne(
                'SELECT amount FROM ohrm_expense WHERE id = ?',
                [$response['data']['id']]
            );
            $this->assertEqualsWithDelta((float) $amount, (float) $saved, 0.001, 'valor gravado');
        } else {
            $this->assertSame($before, $this->rows('ohrm_expense'), 'nenhuma despesa gravada');
        }
    }

    public function createAmountProvider(): array
    {
        return [
            'I15: -0,01' => [-0.01, 422],
            'I14: 0' => [0, 422],
            'V9: 0,01 (mínimo)' => [0.01, 200],
            'V9: 1 (sem casas)' => [1, 200],
            'V9: 7000,1 (1 casa)' => [7000.1, 200],
            'V9: 7000,12 (2 casas)' => [7000.12, 200],
            'I17: 7000,123 (3 casas)' => [7000.123, 422],
            'V9: 9 999 999 999,99 (máximo)' => [9999999999.99, 200],
            'I16: 10 000 000 000,00' => [10000000000.00, 422],
            'I18: não numérico' => ['abc', 422],
            'I19: ausente' => [null, 422],
        ];
    }

    // ---------------------------------------------------------------- IT-18: valor na edição da despesa

    public function testIT18EdicaoParaUmCentavo(): void
    {
        $response = $this->call(self::ESS, ClaimExpenseAPI::class, 'update', ['requestId' => self::INITIATED, 'id' => 1],
            ['expenseTypeId' => 1, 'date' => '2026-11-05', 'amount' => 0.01]);

        $this->assertHttp(200, $response);
        $this->assertEqualsWithDelta(0.01, (float) $this->getEntityManager()->getConnection()
            ->fetchOne('SELECT amount FROM ohrm_expense WHERE id = 1'), 0.001);
    }

    /**
     * R2: a criação recusa 0 (IT-17), mas a edição só tem BETWEEN [0, ...]. O esperado é a mesma regra.
     *
     * @group defeito-conhecido
     */
    public function testIT18EdicaoParaZeroDeveSerRecusada(): void
    {
        $response = $this->call(self::ESS, ClaimExpenseAPI::class, 'update', ['requestId' => self::INITIATED, 'id' => 1],
            ['expenseTypeId' => 1, 'date' => '2026-11-05', 'amount' => 0]);

        $this->assertHttp(422, $response);
        $this->assertEqualsWithDelta(10.5, (float) $this->getEntityManager()->getConnection()
            ->fetchOne('SELECT amount FROM ohrm_expense WHERE id = 1'), 0.001, 'valor original preservado');
    }

    // ---------------------------------------------------------------- IT-19: tipo de despesa

    /**
     * @dataProvider expenseTypeProvider
     */
    public function testIT19TipoDeDespesa(?int $typeId, int $expectedHttp): void
    {
        $this->assertHttp($expectedHttp, $this->createExpense(['expenseTypeId' => $typeId]));
    }

    public function expenseTypeProvider(): array
    {
        return [
            'V5: ativo' => [1, 200],
            'I8: ausente' => [null, 422],
            'I9: inativo' => [2, 422],
            'I10: inexistente' => [99, 422],
        ];
    }

    // ---------------------------------------------------------------- IT-20: data da despesa

    /**
     * Datas futura e anterior à criação (V7?, V8?) não têm regra: o código aceita e o caso só caracteriza.
     *
     * @dataProvider dateProvider
     */
    public function testIT20DataDaDespesa(?string $date, int $expectedHttp): void
    {
        $this->assertHttp($expectedHttp, $this->createExpense(['date' => $date]));
    }

    public function dateProvider(): array
    {
        return [
            'V6: 2026-02-28' => ['2026-02-28', 200],
            'I13: 2026-02-29 (não bissexto)' => ['2026-02-29', 422],
            'V6: 2028-02-29 (bissexto)' => ['2028-02-29', 200],
            'I12: 17/11/2026' => ['17/11/2026', 422],
            'I11: ausente' => [null, 422],
            'V7? futura: 2027-01-01 (caracteriza)' => ['2027-01-01', 200],
            'V8? anterior à criação: 2020-01-01 (caracteriza)' => ['2020-01-01', 200],
        ];
    }

    // ---------------------------------------------------------------- IT-21: nota

    /**
     * @dataProvider noteProvider
     */
    public function testIT21Nota(int $length, int $expectedHttp): void
    {
        $this->assertHttp($expectedHttp, $this->createExpense(['note' => str_repeat('n', $length)]));
    }

    public function noteProvider(): array
    {
        return ['V11: 1000' => [1000, 200], 'I20: 1001' => [1001, 422]];
    }

    // ---------------------------------------------------------------- IT-22: despesa conforme o estado

    /**
     * E7: a despesa só pode mudar quando o dono ainda pode submeter (INITIATED ou REJECTED).
     * Nas fixtures, a despesa de cada reivindicação tem o mesmo id da reivindicação.
     *
     * @dataProvider expenseByStateProvider
     */
    public function testIT22DespesaConformeOEstado(int $claimId, string $operation, int $expectedHttp): void
    {
        $connection = $this->getEntityManager()->getConnection();
        $snapshot = fn () => $connection->fetchAllAssociative(
            'SELECT id, amount, is_deleted FROM ohrm_expense WHERE request_id = ? ORDER BY id',
            [$claimId]
        );
        $before = $snapshot();

        $attributes = ['requestId' => $claimId];
        $body = ['expenseTypeId' => 1, 'date' => '2026-11-05', 'amount' => 99.99];
        if ($operation === 'update') {
            $attributes['id'] = $claimId;
        }
        if ($operation === 'delete') {
            $body = ['ids' => [$claimId]];
        }
        $response = $this->call(self::ESS, ClaimExpenseAPI::class, $operation, $attributes, $body);

        $this->assertHttp($expectedHttp, $response);
        if ($expectedHttp !== 200) {
            $this->assertSame($before, $snapshot(), 'despesas inalteradas');
        }
    }

    public function expenseByStateProvider(): array
    {
        $cases = [];
        foreach (['SUBMITTED' => self::SUBMITTED, 'APPROVED' => self::APPROVED, 'PAID' => self::PAID,
                     'CANCELLED' => self::CANCELLED, 'REJECTED' => self::REJECTED] as $state => $claimId) {
            foreach (['create', 'update', 'delete'] as $operation) {
                $cases["$state, $operation"] = [$claimId, $operation, $state === 'REJECTED' ? 200 : 403];
            }
        }
        return $cases;
    }

    // ---------------------------------------------------------------- IT-23: submeter sem despesas (R3)

    /**
     * Caracteriza R3: a API aceita submeter uma reivindicação sem despesas? A exigência, se existir, fica na tela (ST-09).
     */
    public function testIT23SubmeterSemDespesas(): void
    {
        $this->assertSame(0, (int) $this->getEntityManager()->getConnection()
            ->fetchOne('SELECT COUNT(*) FROM ohrm_expense WHERE request_id = ?', [self::SEM_DESPESAS]), 'pré-condição');

        $this->assertAction(self::ESS, self::SEM_DESPESAS, 'SUBMIT', 'SUBMITTED');
    }

    // ---------------------------------------------------------------- IT-24: funcionário desligado

    public function testIT24ReivindicacaoParaFuncionarioDesligado(): void
    {
        $before = $this->rows('ohrm_claim_request');

        $response = $this->call(self::ADMIN, EmployeeClaimRequestAPI::class, 'create', ['empNumber' => 5],
            ['claimEventId' => 1, 'currencyId' => 'USD']);

        $this->assertHttp(403, $response);
        $this->assertSame($before, $this->rows('ohrm_claim_request'));
    }

    // ---------------------------------------------------------------- IT-25: descrição do anexo

    /**
     * @dataProvider attachmentDescriptionProvider
     */
    public function testIT25DescricaoDoAnexo(int $length, int $expectedHttp): void
    {
        $png = base64_decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4z8AAAAMBAQDJ/pLvAAAAAElFTkSuQmCC');
        $response = $this->call(self::ESS, ClaimAttachmentAPI::class, 'create', ['requestId' => self::INITIATED], [
            'attachment' => ['name' => 'nota.png', 'type' => 'image/png', 'size' => (string) strlen($png), 'base64' => base64_encode($png)],
            'description' => str_repeat('d', $length),
        ]);

        $this->assertHttp($expectedHttp, $response);
    }

    public function attachmentDescriptionProvider(): array
    {
        return ['V13: 200' => [200, 200], 'I21: 201' => [201, 422]];
    }

    // ---------------------------------------------------------------- IT-26: número de referência

    public function testIT26NumeroDeReferenciaDeDuasReivindicacoesSeguidas(): void
    {
        $first = $this->createClaim(['claimEventId' => 1, 'currencyId' => 'USD'], '2026-11-17 10:00:00');
        $second = $this->createClaim(['claimEventId' => 1, 'currencyId' => 'USD'], '2026-11-17 10:05:00');

        $this->assertHttp(200, $first);
        $this->assertHttp(200, $second);
        // a fixture vai até o id 10; o próximo id é MAX(id) + 1
        $this->assertSame('202611170000011', $first['data']['referenceId']);
        $this->assertSame('202611170000012', $second['data']['referenceId']);
        $this->assertSame('202611170000011', $this->getEntityManager()->getConnection()
            ->fetchOne('SELECT reference_id FROM ohrm_claim_request WHERE id = ?', [$first['data']['id']]));
    }
}
