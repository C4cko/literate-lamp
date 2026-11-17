<?php

/**
 * Trabalho 2 - Teste do OrangeHRM - Jornada "Autenticação (Login)"
 *
 * TESTE UNITÁRIO - PasswordStrengthService::isValidPassword()
 * Técnicas: Particionamento em Classes de Equivalência e Análise de Valor Limite.
 *
 * Esta regra é aplicada no login quando "Enforce password strength" está ligado
 * (LocalAuthProvider). A configuração é injetada por um dublê (stub) que
 * sobrescreve os getters protegidos, isolando o teste do banco de dados.
 *
 * Política usada (igual ao padrão de instalação do OrangeHRM):
 *   comprimento 8..64, >=1 maiúscula, >=1 minúscula, >=1 número,
 *   >=1 caractere especial, espaços proibidos, força mínima "strong" (3).
 * Casos UT-PW-01..20 no Plano de Testes (seção 4.2).
 *
 * Uso de IA: escrito com Claude (Anthropic, Claude Opus 5.5), revisado pelo autor.
 */

namespace OrangeHRM\Tests\Authentication\JornadaLogin\Unit;

use OrangeHRM\Authentication\Dto\UserCredential;
use OrangeHRM\Authentication\Service\PasswordStrengthService;
use OrangeHRM\Core\Service\TextHelperService;
use OrangeHRM\Framework\Services;
use OrangeHRM\Tests\Util\KernelTestCase;

/**
 * @group JornadaLogin
 * @group Unit
 */
class PasswordPolicyBoundaryTest extends KernelTestCase
{
    private const STRONG = 3;

    protected function setUp(): void
    {
        $this->createKernelWithMockServices([
            Services::TEXT_HELPER_SERVICE => new TextHelperService(),
        ]);
    }

    /**
     * Stub do serviço com política configurável (sem acesso ao banco).
     */
    private function service(array $policy = []): PasswordStrengthService
    {
        $policy = array_merge([
            'min' => 8, 'max' => 64, 'lower' => 1, 'upper' => 1,
            'numbers' => 1, 'special' => 1, 'spaces' => 'false', 'strength' => 'strong',
        ], $policy);

        return new class ($policy) extends PasswordStrengthService {
            private array $p;

            public function __construct(array $p)
            {
                $this->p = $p;
            }

            protected function getMinLength(): int
            {
                return $this->p['min'];
            }

            protected function getMaxLength(): int
            {
                return $this->p['max'];
            }

            protected function getMinNoOfLowercaseLetters(): int
            {
                return $this->p['lower'];
            }

            protected function getMinNoOfUppercaseLetters(): int
            {
                return $this->p['upper'];
            }

            protected function getMinNoOfNumbers(): int
            {
                return $this->p['numbers'];
            }

            protected function getMinNoOfSpecialCharacters(): int
            {
                return $this->p['special'];
            }

            protected function getIsSpacesAllowed(): string
            {
                return $this->p['spaces'];
            }

            protected function getDefaultPasswordStrength(): string
            {
                return $this->p['strength'];
            }
        };
    }

    private function isValid(string $password, int $strength = self::STRONG, array $policy = []): bool
    {
        return $this->service($policy)->isValidPassword(new UserCredential('maria', $password), $strength);
    }

    /**
     * Valores-limite do comprimento (min = 8, max = 64).
     * Base "Aa1@" garante as demais regras; o restante é preenchido com 'x'.
     */
    public function dataComprimento(): array
    {
        $build = fn (int $len) => 'Aa1@' . str_repeat('x', $len - 4);
        return [
            'UT-PW-01 7 caracteres (min-1) inválido' => [$build(7), false],
            'UT-PW-02 8 caracteres (min) válido' => [$build(8), true],
            'UT-PW-03 9 caracteres (min+1) válido' => [$build(9), true],
            'UT-PW-04 63 caracteres (max-1) válido' => [$build(63), true],
            'UT-PW-05 64 caracteres (max) válido' => [$build(64), true],
            'UT-PW-06 65 caracteres (max+1) inválido' => [$build(65), false],
            'UT-PW-07 vazia inválida' => ['', false],
        ];
    }

    /**
     * @dataProvider dataComprimento
     */
    public function testLimitesDeComprimento(string $password, bool $expected): void
    {
        $this->assertSame($expected, $this->isValid($password));
    }

    /**
     * Classes de equivalência: uma classe inválida por vez (as demais válidas).
     */
    public function dataClasses(): array
    {
        return [
            'UT-PW-08 classe válida (todas as regras)' => ['Senha@Forte1', true],
            'UT-PW-09 sem maiúscula' => ['senha@forte1', false],
            'UT-PW-10 sem minúscula' => ['SENHA@FORTE1', false],
            'UT-PW-11 sem número' => ['Senha@Forte', false],
            'UT-PW-12 sem caractere especial' => ['SenhaForte12', false],
            'UT-PW-13 com espaço (proibido)' => ['Senha @Forte1', false],
            'UT-PW-14 caracteres acentuados não contam como especiais' => ['SenhaçãoFor1', false],
        ];
    }

    /**
     * @dataProvider dataClasses
     */
    public function testClassesDeEquivalencia(string $password, bool $expected): void
    {
        $this->assertSame($expected, $this->isValid($password));
    }

    /**
     * Valor-limite da força exigida ("strong" = 3): 2 rejeita, 3 e 4 aceitam.
     */
    public function dataForca(): array
    {
        return [
            'UT-PW-15 força 2 (abaixo do limite)' => [2, false],
            'UT-PW-16 força 3 (no limite)' => [3, true],
            'UT-PW-17 força 4 (acima do limite)' => [4, true],
        ];
    }

    /**
     * @dataProvider dataForca
     */
    public function testLimiteDeForca(int $strength, bool $expected): void
    {
        $this->assertSame($expected, $this->isValid('Senha@Forte1', $strength));
    }

    /** UT-PW-18: com espaços permitidos, a mesma senha com espaço passa a ser válida */
    public function testEspacosPermitidosPorConfiguracao(): void
    {
        $this->assertTrue($this->isValid('Senha @Forte1', self::STRONG, ['spaces' => 'true']));
    }

    /** UT-PW-19: mínimos configurados com valor negativo são tratados como 0 */
    public function testMinimosNegativosSaoTratadosComoZero(): void
    {
        $policy = ['min' => -1, 'lower' => -1, 'upper' => -1, 'numbers' => -1, 'special' => -1];
        $this->assertTrue($this->isValid('abc', self::STRONG, $policy));
    }

    /** UT-PW-20: força mínima "veryWeak" aceita qualquer pontuação (inclusive 0) */
    public function testForcaMinimaVeryWeakAceitaQualquerPontuacao(): void
    {
        $this->assertTrue($this->isValid('Senha@Forte1', 0, ['strength' => 'veryWeak']));
    }
}
