<?php

/**
 * T2 VERVAL (PUCRS): UT-03 a UT-07 do plano de testes de Claim (renan-bertoletti/plano, seção 6.1).
 * Regras de validação das APIs de Claim, aplicadas pelo mesmo Validator que o framework usa antes de chamar
 * create()/update(). Sem banco: o ConfigService (lido pela regra de anexo) é simulado.
 * Técnicas: particionamento em classes de equivalência e valor-limite (plano, seções 4.2 e 4.3).
 */

namespace OrangeHRM\Tests\Claim\T2ClaimUnitario;

use OrangeHRM\Claim\Api\ClaimAttachmentAPI;
use OrangeHRM\Claim\Api\ClaimExpenseAPI;
use OrangeHRM\Claim\Api\ClaimRequestActionAPI;
use OrangeHRM\Claim\Api\MyClaimRequestAPI;
use OrangeHRM\Core\Api\V2\Validator\ParamRuleCollection;
use OrangeHRM\Core\Service\ConfigService;
use OrangeHRM\Framework\Services;
use OrangeHRM\Tests\Util\EndpointTestCase;

/**
 * @group Claim
 * @group T2Claim
 * @group T2ClaimUnitario
 */
class ClaimValidationRulesTest extends EndpointTestCase
{
    protected function setUp(): void
    {
        parent::setUp();
        $config = $this->getMockBuilder(ConfigService::class)
            ->onlyMethods(['getAllowedFileTypes', 'getAllowedFileExtensions', 'getMaxAttachmentSize'])
            ->getMock();
        $config->method('getAllowedFileTypes')->willReturn(['image/png']);
        $config->method('getAllowedFileExtensions')->willReturn(['png']);
        $config->method('getMaxAttachmentSize')->willReturn(1048576);
        $this->createKernelWithMockServices([Services::CONFIG_SERVICE => $config]);
    }

    /** @param class-string $apiClass */
    private function rules(string $apiClass, string $operation): ParamRuleCollection
    {
        $api = $this->getApiEndpointMockBuilder($apiClass)->onlyMethods([])->getMock();
        return $operation === 'create' ? $api->getValidationRuleForCreate() : $api->getValidationRuleForUpdate();
    }

    /** @param class-string $apiClass */
    private function assertValid(string $apiClass, string $operation, array $values): void
    {
        $this->assertTrue($this->validate($values, $this->rules($apiClass, $operation)));
    }

    /**
     * Inválido exatamente no parâmetro $param: garante que a recusa vem do valor testado, e não de outro campo.
     *
     * @param class-string $apiClass
     */
    private function assertInvalid(string $apiClass, string $operation, array $values, string $param): void
    {
        $rules = $this->rules($apiClass, $operation);
        $this->assertInvalidParamException(fn () => $this->validate($values, $rules), [$param]);
    }

    private static function expenseCreate(array $override = []): array
    {
        return array_replace(
            ['requestId' => 1, 'expenseTypeId' => 1, 'amount' => 10.5, 'date' => '2026-11-10'],
            $override
        );
    }

    private static function expenseUpdate(array $override = []): array
    {
        return array_replace(self::expenseCreate(['id' => 1]), $override);
    }

    // ---------------------------------------------------------------- UT-03: valor na criação da despesa

    /**
     * @dataProvider createAmountProvider
     */
    public function testUT03ValorNaCriacao($amount, bool $valid): void
    {
        $values = self::expenseCreate(['amount' => $amount]);
        $valid
            ? $this->assertValid(ClaimExpenseAPI::class, 'create', $values)
            : $this->assertInvalid(ClaimExpenseAPI::class, 'create', $values, 'amount');
    }

    public function createAmountProvider(): array
    {
        return [
            'negativo (-0,01)' => [-0.01, false],
            'zero' => [0, false],
            'mínimo (0,01)' => [0.01, true],
            'máximo (9 999 999 999,99)' => [9999999999.99, true],
            'máximo + 0,01 (10 000 000 000,00)' => [10000000000.00, false],
        ];
    }

    // ---------------------------------------------------------------- UT-04: valor na edição da despesa

    /**
     * @dataProvider updateAmountProvider
     */
    public function testUT04ValorNaEdicao($amount, bool $valid): void
    {
        $values = self::expenseUpdate(['amount' => $amount]);
        $valid
            ? $this->assertValid(ClaimExpenseAPI::class, 'update', $values)
            : $this->assertInvalid(ClaimExpenseAPI::class, 'update', $values, 'amount');
    }

    public function updateAmountProvider(): array
    {
        return [
            'negativo (-0,01)' => [-0.01, false],
            'mínimo (0,01)' => [0.01, true],
        ];
    }

    /**
     * UT-04, valor 0: na criação é recusado (POSITIVE); na edição só há BETWEEN [0, ...], que o aceita (R2).
     * O esperado é a mesma regra nos dois caminhos.
     *
     * @group defeito-conhecido
     */
    public function testUT04ZeroNaEdicaoDeveSerRecusadoComoNaCriacao(): void
    {
        $this->assertInvalid(ClaimExpenseAPI::class, 'update', self::expenseUpdate(['amount' => 0]), 'amount');
    }

    // ---------------------------------------------------------------- UT-05: limites de tamanho dos textos

    /**
     * @dataProvider textLengthProvider
     */
    public function testUT05LimitesDeTamanho(string $apiClass, array $base, string $param, int $length, bool $valid): void
    {
        $values = array_replace($base, [$param => str_repeat('a', $length)]);
        $valid
            ? $this->assertValid($apiClass, 'create', $values)
            : $this->assertInvalid($apiClass, 'create', $values, $param);
    }

    public function textLengthProvider(): array
    {
        $claim = ['claimEventId' => 1, 'currencyId' => 'USD'];
        $expense = self::expenseCreate();
        $attachment = ['requestId' => 1, 'attachment' => self::pngAttachment()];
        return [
            'remarks com 1000' => [MyClaimRequestAPI::class, $claim, 'remarks', 1000, true],
            'remarks com 1001' => [MyClaimRequestAPI::class, $claim, 'remarks', 1001, false],
            'note com 1000' => [ClaimExpenseAPI::class, $expense, 'note', 1000, true],
            'note com 1001' => [ClaimExpenseAPI::class, $expense, 'note', 1001, false],
            'descrição do anexo com 200' => [ClaimAttachmentAPI::class, $attachment, 'description', 200, true],
            'descrição do anexo com 201' => [ClaimAttachmentAPI::class, $attachment, 'description', 201, false],
        ];
    }

    // ---------------------------------------------------------------- UT-06: parâmetro action

    /**
     * @dataProvider actionProvider
     */
    public function testUT06Acao(string $action, bool $valid): void
    {
        $values = ['requestId' => 1, 'action' => $action];
        $valid
            ? $this->assertValid(ClaimRequestActionAPI::class, 'update', $values)
            : $this->assertInvalid(ClaimRequestActionAPI::class, 'update', $values, 'action');
    }

    public function actionProvider(): array
    {
        return [
            'SUBMIT' => ['SUBMIT', true],
            'APPROVE' => ['APPROVE', true],
            'REJECT' => ['REJECT', true],
            'PAY' => ['PAY', true],
            'CANCEL' => ['CANCEL', true],
            'inexistente (FOO, SP-21)' => ['FOO', false],
            'minúsculas (submit)' => ['submit', false],
            'vazio' => ['', false],
        ];
    }

    // ---------------------------------------------------------------- UT-07: data da despesa

    /**
     * @dataProvider dateProvider
     */
    public function testUT07DataDaDespesa(string $date, bool $valid): void
    {
        $values = self::expenseCreate(['date' => $date]);
        $valid
            ? $this->assertValid(ClaimExpenseAPI::class, 'create', $values)
            : $this->assertInvalid(ClaimExpenseAPI::class, 'create', $values, 'date');
    }

    public function dateProvider(): array
    {
        return [
            '28/02 de ano não bissexto' => ['2026-02-28', true],
            '29/02 de ano não bissexto' => ['2026-02-29', false],
            '29/02 de ano bissexto' => ['2028-02-29', true],
            'formato dd/mm/aaaa' => ['17/11/2026', false],
            'vazia' => ['', false],
        ];
    }

    /** PNG 1x1 válido no formato Base64Attachment (mesma imagem usada por Mateus Neubarth em #10). */
    private static function pngAttachment(): array
    {
        $png = base64_decode(
            'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4z8AAAAMBAQDJ/pLvAAAAAElFTkSuQmCC'
        );
        return ['name' => 'nota.png', 'type' => 'image/png', 'size' => (string) strlen($png), 'base64' => base64_encode($png)];
    }
}
