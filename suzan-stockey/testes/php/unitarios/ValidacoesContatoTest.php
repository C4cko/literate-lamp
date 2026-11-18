<?php
namespace Suzan\EmergencyContacts;

use OrangeHRM\Core\Api\V2\Validator\Rules;
use OrangeHRM\Core\Api\V2\Validator\Rules\Required;
use OrangeHRM\Core\Api\V2\Validator\Rules\NotBlankStringType;
use OrangeHRM\Core\Api\V2\Validator\Rules\Phone;
use OrangeHRM\Pim\Api\EmpEmergencyContactAPI;
use PHPUnit\Framework\TestCase;

class ValidacoesContatoTest extends TestCase
{
    /** @dataProvider obrigatorios */
    public function testUT01Obrigatorios($value, bool $required, bool $string, bool $combined): void
    {
        $r = new Required();
        $s = new NotBlankStringType();
        self::assertSame($required, $r->validate($value));
        self::assertSame($string, $s->validate($value));
        self::assertSame($combined, $r->validate($value) && $s->validate($value));
    }

    public function obrigatorios(): array
    {
        return [
            'vazio' => ['', false, true, false],
            'espacos' => ['   ', true, false, false],
            'texto' => ['Irma', true, true, true],
        ];
    }

    /** @dataProvider telefones */
    public function testUT02FormatoOriginal(string $value, bool $expected): void
    {
        self::assertSame($expected, (new Phone())->validate($value));
    }

    public function telefones(): array
    {
        return [
            'numero' => ['51999990000', true],
            'letras' => ['abc', false],
            'arroba' => ['123@456', false],
            'sinais-caracterizacao' => ['+++++++++', true],
        ];
    }

    /** @dataProvider limites */
    public function testUT03LimiteOriginal(string $constant, int $size, bool $expected): void
    {
        $max = constant(EmpEmergencyContactAPI::class . '::' . $constant);
        $ruleClass = Rules::LENGTH;
        $rule = new $ruleClass(null, $max);
        $value = substr(str_repeat('1234567890', 11), 0, $size);
        self::assertSame($expected, $rule->validate($value));
    }

    public function limites(): array
    {
        $rows = [];
        // Nome e parentesco compartilham a constante de limite no endpoint.
        foreach (['name' => 'PARAM_RULE_MAX_LENGTH', 'relationship' => 'PARAM_RULE_MAX_LENGTH',
            'homePhone' => 'PARAM_RULE_HOME_PHONE_MAX_LENGTH',
            'mobilePhone' => 'PARAM_RULE_MOBILE_PHONE_MAX_LENGTH',
            'officePhone' => 'PARAM_RULE_OFFICE_PHONE_MAX_LENGTH'] as $field => $constant) {
            $max = $constant === 'PARAM_RULE_MAX_LENGTH' ? 100 : 30;
            foreach ([-1, 0, 1] as $offset) {
                $rows[$field . '-' . ($max + $offset)] = [$constant, $max + $offset, $offset <= 0];
            }
        }
        return $rows;
    }
}
