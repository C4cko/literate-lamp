<?php

/**
 * T2 VERVAL (PUCRS): UT-01 a UT-04 de docs/casos-de-teste.md.
 * Regras de validação de POST /api/v2/pim/employees (EmployeeAPI::getValidationRuleForCreate),
 * projetadas com particionamento em classes de equivalência e análise de valor-limite.
 */

namespace OrangeHRM\Tests\Pim\T2AddEmployee;

/**
 * @group Pim
 * @group T2AddEmployee
 */
class EmployeeCreateValidationTest extends AddEmployeeApiTestCase
{
    protected function setUp(): void
    {
        parent::setUp();
        $this->loadFixtures();
    }

    private static function validBody(array $override = []): array
    {
        return array_merge(['firstName' => 'Ana', 'lastName' => 'Silva'], $override);
    }

    // ---------------------------------------------------------------- UT-01: tamanho dos nomes (AVL)

    /**
     * @dataProvider nameBoundaryProvider
     */
    public function testUT01NomeNoLimiteEhAceito(string $field, int $length): void
    {
        $this->assertTrue($this->validateCreate(self::validBody([$field => str_repeat('a', $length)])));
    }

    public function nameBoundaryProvider(): array
    {
        return [
            'firstName com 1 caractere' => ['firstName', 1],
            'firstName com 30 caracteres (máx.)' => ['firstName', 30],
            'middleName com 30 caracteres (máx.)' => ['middleName', 30],
            'lastName com 1 caractere' => ['lastName', 1],
            'lastName com 30 caracteres (máx.)' => ['lastName', 30],
        ];
    }

    /**
     * @dataProvider nameOverLimitProvider
     */
    public function testUT01NomeAcimaDoLimiteEhRejeitado(string $field): void
    {
        $this->assertCreateInvalid(self::validBody([$field => str_repeat('a', 31)]), [$field]);
    }

    public function nameOverLimitProvider(): array
    {
        return [
            'firstName com 31 caracteres' => ['firstName'],
            'middleName com 31 caracteres' => ['middleName'],
            'lastName com 31 caracteres' => ['lastName'],
        ];
    }

    // ---------------------------------------------------------------- UT-02: obrigatoriedade (PE)

    /**
     * @dataProvider missingNameProvider
     */
    public function testUT02NomeObrigatorioAusenteEhRejeitado(array $body, array $invalid): void
    {
        $this->assertCreateInvalid($body, $invalid);
    }

    public function missingNameProvider(): array
    {
        return [
            'sem lastName' => [['firstName' => 'Ana'], ['lastName']],
            'sem firstName' => [['lastName' => 'Silva'], ['firstName']],
            'firstName vazio' => [['firstName' => '', 'lastName' => 'Silva'], ['firstName']],
            'sem nenhum nome' => [[], ['firstName', 'lastName']],
        ];
    }

    public function testUT02MiddleNameEhOpcional(): void
    {
        $this->assertTrue($this->validateCreate(self::validBody()));
    }

    // ---------------------------------------------------------------- UT-03: Employee Id (AVL + PE)

    /**
     * @dataProvider validEmployeeIdProvider
     */
    public function testUT03EmployeeIdValidoEhAceito(string $employeeId): void
    {
        $this->assertTrue($this->validateCreate(self::validBody(['employeeId' => $employeeId])));
    }

    public function validEmployeeIdProvider(): array
    {
        return [
            'novo, 1 caractere' => ['9'],
            'novo, 10 caracteres (máx.)' => ['ABCDE12345'],
            // R4: a unicidade ignora funcionários expurgados (fixture: 0004 com purged_at)
            'ID de funcionário expurgado pode ser reutilizado' => ['0004'],
        ];
    }

    /**
     * @dataProvider invalidEmployeeIdProvider
     */
    public function testUT03EmployeeIdInvalidoEhRejeitado(string $employeeId): void
    {
        $this->assertCreateInvalid(self::validBody(['employeeId' => $employeeId]), ['employeeId']);
    }

    public function invalidEmployeeIdProvider(): array
    {
        return [
            '11 caracteres' => ['ABCDE123456'],
            'duplicado de funcionário ativo' => ['0002'],
            'duplicado de funcionário desligado (terminated)' => ['0006'],
        ];
    }

    // ---------------------------------------------------------------- UT-04: foto (PE + AVL no nome)

    /**
     * @dataProvider validPictureProvider
     */
    public function testUT04FotoValidaEhAceita(string $name, string $type): void
    {
        $body = self::validBody(['empPicture' => self::pngAttachment($name, $type)]);
        $this->assertTrue($this->validateCreate($body));
    }

    public function validPictureProvider(): array
    {
        return [
            'png' => ['foto.png', 'image/png'],
            'jpg declarado como image/jpeg' => ['foto.jpg', 'image/jpeg'],
            'nome com 100 caracteres (máx.)' => [str_repeat('a', 96) . '.png', 'image/png'],
        ];
    }

    /**
     * @dataProvider invalidPictureProvider
     */
    public function testUT04FotoInvalidaEhRejeitada(string $name, string $type): void
    {
        $body = self::validBody(['empPicture' => self::pngAttachment($name, $type)]);
        $this->assertCreateInvalid($body, ['empPicture']);
    }

    public function invalidPictureProvider(): array
    {
        return [
            'tipo MIME não permitido (pdf)' => ['foto.pdf', 'application/pdf'],
            'extensão não permitida (bmp)' => ['foto.bmp', 'image/bmp'],
            'MIME de imagem com extensão errada' => ['foto.exe', 'image/png'],
            'nome com 101 caracteres' => [str_repeat('a', 97) . '.png', 'image/png'],
        ];
    }
}
