<?php

/**
 * T2 VERVAL (PUCRS): UT-08 de docs/casos-de-teste.md.
 * Política de senha usada no passo "Create Login Details" (A2/E6), com a configuração padrão do
 * OrangeHRM 5.4+: mínimo 8, máximo 64, 1 maiúscula, 1 minúscula, 1 número, 1 especial, sem espaços.
 * Uma senha por classe de equivalência inválida; o oráculo é a LISTA COMPLETA de mensagens
 * (o PasswordStrengthServiceTest original confere só a primeira).
 */

namespace OrangeHRM\Tests\Pim\T2AddEmployee;

use OrangeHRM\Admin\Service\UserService;
use OrangeHRM\Authentication\Dto\UserCredential;
use OrangeHRM\Authentication\Service\PasswordStrengthService;
use OrangeHRM\Authentication\Utility\PasswordStrengthValidation;
use OrangeHRM\Config\Config;
use OrangeHRM\Core\Service\ConfigService;
use OrangeHRM\Core\Service\DateTimeHelperService;
use OrangeHRM\Framework\Services;
use OrangeHRM\I18N\Service\I18NService;
use OrangeHRM\Tests\Util\KernelTestCase;
use OrangeHRM\Tests\Util\Mock\MockCacheService;
use OrangeHRM\Tests\Util\TestDataService;

/**
 * @group Pim
 * @group T2AddEmployee
 */
class PasswordPolicyTest extends KernelTestCase
{
    private const MIN_8 = 'Should have at least 8 characters';
    private const MAX_64 = 'Should not exceed 64 characters';
    private const UPPER = 'Your password must contain minimum 1 upper-case letter';
    private const LOWER = 'Your password must contain minimum 1 lower-case letter';
    private const NUMBER = 'Your password must contain minimum 1 number';
    private const SPECIAL = 'Your password must contain minimum 1 special character';
    private const SPACES = 'Your password should not contain spaces';
    private const GUESSABLE = 'Your password meets the minimum requirements, but it could be guessable';

    private PasswordStrengthService $service;

    protected function setUp(): void
    {
        parent::setUp();
        // mesma fixture do PasswordStrengthServiceTest original: grava a política padrão (auth.password_policy.*)
        TestDataService::populate(
            Config::get(Config::PLUGINS_DIR) . '/orangehrmAuthenticationPlugin/test/fixtures/PasswordStrengthValidation.yml'
        );
        $this->createKernelWithMockServices([
            Services::CONFIG_SERVICE => new ConfigService(),
            Services::DATETIME_HELPER_SERVICE => new DateTimeHelperService(),
            Services::USER_SERVICE => new UserService(),
            Services::I18N_SERVICE => new I18NService(),
            Services::CACHE => MockCacheService::getCache(),
        ]);
        $this->service = new PasswordStrengthService();
    }

    /** Mesmo cálculo da regra Rules\Password usada pela API de usuários. */
    private function messagesFor(string $password): array
    {
        $credential = new UserCredential(null, $password);
        $strength = (new PasswordStrengthValidation())->checkPasswordStrength($credential);
        return $this->service->checkPasswordPolicies($credential, $strength);
    }

    /**
     * @dataProvider invalidPasswordProvider
     */
    public function testUT08SenhaForaDaPoliticaRetornaTodasAsMensagens(string $password, array $expected): void
    {
        $this->assertSame($expected, $this->messagesFor($password));
    }

    public function invalidPasswordProvider(): array
    {
        return [
            'I13: 7 caracteres (limite inferior - 1)' => ['Abc12#x', [self::MIN_8]],
            'I14: sem maiúscula' => ['abcdef12#', [self::UPPER]],
            'I15: sem minúscula' => ['ABCDEF12#', [self::LOWER]],
            'I16: sem número' => ['Abcdefgh#', [self::NUMBER]],
            'I17: sem caractere especial' => ['Abcdefg12', [self::SPECIAL]],
            'I18: com espaço' => ['Abc 12#xyz', [self::SPACES]],
            'I19: 65 caracteres (limite superior + 1)' => [str_repeat('Ab1#', 16) . 'A', [self::MAX_64]],
            'várias classes ao mesmo tempo: "abc"' => ['abc', [self::MIN_8, self::UPPER, self::NUMBER, self::SPECIAL]],
            // I20: cumpre as regras explícitas, mas a força (zxcvbn) fica abaixo de "strong" (score < 3)
            'I20: previsível, "Password1!"' => ['Password1!', [self::GUESSABLE]],
        ];
    }

    /**
     * @dataProvider validPasswordProvider
     */
    public function testUT08SenhaDentroDaPoliticaNaoTemMensagens(string $password): void
    {
        $this->assertSame([], $this->messagesFor($password));
    }

    public function validPasswordProvider(): array
    {
        return [
            '9 caracteres, aleatória' => ['t7#Rw!9Zp'],
            '64 caracteres (limite superior)' => [str_repeat('Qz7#', 15) . 'mX2k'],
        ];
    }

    /**
     * UT-08 (valor-limite inferior declarado): uma senha de 8 caracteres que cumpre TODAS as regras
     * explícitas deveria ser aceita, já que o sistema anuncia "Should have at least 8 characters".
     * FALHA no commit 56e23b3b: defeito D2 (docs/defeitos.md). Com a força mínima padrão "strong", o zxcvbn
     * nunca dá score 3 a uma senha de 8 caracteres (2.000 senhas aleatórias testadas: todas com score 2),
     * então o mínimo efetivo é 9.
     *
     * @group defeito-conhecido
     */
    public function testUT08SenhaDe8CaracteresQueCumpreAsRegrasEhAceita(): void
    {
        $this->assertSame(
            [],
            $this->messagesFor('Qz7#mX2k'),
            'Defeito D2: o limite inferior anunciado (8) é inatingível com a configuração padrão'
        );
    }
}
