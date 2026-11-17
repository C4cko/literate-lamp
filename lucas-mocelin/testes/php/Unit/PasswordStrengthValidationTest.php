<?php

/**
 * Trabalho 2 - Teste do OrangeHRM - Jornada "Autenticação (Login)"
 *
 * TESTE UNITÁRIO - PasswordStrengthValidation::checkPasswordStrength()
 * Técnica: Particionamento em Classes de Equivalência (saída: 0..4).
 * Casos UT-SV-01..05 no Plano de Testes (seção 4.3).
 *
 * Uso de IA: escrito com Claude (Anthropic, Claude Opus 5.5), revisado pelo autor.
 */

namespace OrangeHRM\Tests\Authentication\JornadaLogin\Unit;

use OrangeHRM\Authentication\Dto\UserCredential;
use OrangeHRM\Authentication\Utility\PasswordStrengthValidation;
use OrangeHRM\Tests\Util\TestCase;

/**
 * @group JornadaLogin
 * @group Unit
 */
class PasswordStrengthValidationTest extends TestCase
{
    private function strength(?string $password): int
    {
        return (new PasswordStrengthValidation())->checkPasswordStrength(new UserCredential('maria', $password));
    }

    /** UT-SV-01: senha vazia -> VERY_WEAK */
    public function testSenhaVaziaEhMuitoFraca(): void
    {
        $this->assertSame(PasswordStrengthValidation::VERY_WEAK, $this->strength(''));
    }

    /** UT-SV-02: senha de dicionário -> VERY_WEAK */
    public function testSenhaDeDicionarioEhMuitoFraca(): void
    {
        $this->assertSame(PasswordStrengthValidation::VERY_WEAK, $this->strength('password'));
    }

    /** UT-SV-03: senha padrão do servidor de demonstração (admin123) -> abaixo de "strong" */
    public function testSenhaDaDemoEhFraca(): void
    {
        $this->assertLessThan(PasswordStrengthValidation::STRONG, $this->strength('admin123'));
    }

    /** UT-SV-04: frase-senha longa e aleatória -> STRONGEST */
    public function testFraseSenhaLongaEhFortissima(): void
    {
        $this->assertSame(PasswordStrengthValidation::STRONGEST, $this->strength('Tq9#vL2!pX7$wZ4&mR8'));
    }

    /** UT-SV-05: senha nula (entrada inválida) não lança erro e retorna VERY_WEAK */
    public function testSenhaNulaRetornaMuitoFracaSemErro(): void
    {
        $this->assertSame(PasswordStrengthValidation::VERY_WEAK, $this->strength(null));
    }
}
