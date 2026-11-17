<?php

/**
 * Trabalho 2 - Teste do OrangeHRM - Jornada "Autenticação (Login)"
 *
 * TESTES DE INTEGRAÇÃO
 * Integram, SEM dublês, as camadas reais da autenticação local:
 *   LocalAuthProvider -> AuthenticationService -> UserService -> UserDao
 *   -> Doctrine ORM -> banco MySQL/MariaDB de teste (test_orangehrm_mysql)
 *   + ConfigService (política de senha) + PasswordStrengthService + sessão.
 *
 * Técnicas: classes de equivalência sobre as credenciais e os estados da conta,
 * teste baseado em estados (hash legado -> hash bcrypt) e teste de robustez
 * (entradas maliciosas/vazias). Casos IT-01..IT-15 no Plano de Testes (seção 5).
 *
 * Massa de dados: fixtures/JornadaLogin.yml
 *
 * Uso de IA: escrito com Claude (Anthropic, Claude Opus 5.5), revisado pelo autor.
 */

namespace OrangeHRM\Tests\Authentication\JornadaLogin\Integration;

use OrangeHRM\Admin\Service\UserService;
use OrangeHRM\Authentication\Auth\LocalAuthProvider;
use OrangeHRM\Authentication\Auth\User as AuthUser;
use OrangeHRM\Authentication\Dto\AuthParams;
use OrangeHRM\Authentication\Dto\UserCredential;
use OrangeHRM\Authentication\Exception\AuthenticationException;
use OrangeHRM\Authentication\Exception\PasswordEnforceException;
use OrangeHRM\Authentication\Service\AuthenticationService;
use OrangeHRM\Authentication\Service\PasswordStrengthService;
use OrangeHRM\Config\Config;
use OrangeHRM\Core\Helper\ClassHelper;
use OrangeHRM\Core\Service\ConfigService;
use OrangeHRM\Core\Service\DateTimeHelperService;
use OrangeHRM\Core\Service\TextHelperService;
use OrangeHRM\Entity\EnforcePasswordRequest;
use OrangeHRM\Entity\User;
use OrangeHRM\Framework\Http\Session\Session;
use OrangeHRM\Framework\Services;
use OrangeHRM\I18N\Service\I18NHelper;
use OrangeHRM\I18N\Service\I18NService;
use OrangeHRM\Tests\Util\KernelTestCase;
use OrangeHRM\Tests\Util\Mock\MockCacheService;
use OrangeHRM\Tests\Util\Mock\MockUserRoleManager;
use OrangeHRM\Tests\Util\TestDataService;
use Symfony\Component\HttpFoundation\Session\Storage\MockArraySessionStorage;

/**
 * @group JornadaLogin
 * @group Integration
 */
class LoginIntegrationTest extends KernelTestCase
{
    private Session $session;

    protected function setUp(): void
    {
        TestDataService::populate(
            Config::get(Config::PLUGINS_DIR)
            . '/orangehrmAuthenticationPlugin/test/JornadaLogin/fixtures/JornadaLogin.yml'
        );

        // Sessão HTTP real do Symfony, mas com armazenamento em memória.
        $this->session = new Session(new MockArraySessionStorage());

        $i18nHelper = $this->getMockBuilder(I18NHelper::class)
            ->onlyMethods(['transBySource', 'trans'])
            ->getMock();
        $i18nHelper->method('transBySource')->willReturnArgument(0);
        $i18nHelper->method('trans')->willReturnArgument(0);

        $this->createKernelWithMockServices([
            Services::SESSION => $this->session,
            Services::AUTH_USER => AuthUser::getInstance(),
            Services::CONFIG_SERVICE => new ConfigService(),
            Services::USER_SERVICE => new UserService(),
            Services::PASSWORD_STRENGTH_SERVICE => new PasswordStrengthService(),
            Services::TEXT_HELPER_SERVICE => new TextHelperService(),
            Services::DATETIME_HELPER_SERVICE => new DateTimeHelperService(),
            Services::CLASS_HELPER => new ClassHelper(),
            Services::I18N_SERVICE => new I18NService(),
            Services::I18N_HELPER => $i18nHelper,
            Services::CACHE => MockCacheService::getCache(),
        ]);
    }

    private function login(?string $username, ?string $password): bool
    {
        return (new AuthenticationService())->setCredentials(new UserCredential($username, $password));
    }

    private function expectAuthError(string $username, string $password, string $expectedError): void
    {
        try {
            $this->login($username, $password);
            $this->fail("Era esperada AuthenticationException '$expectedError'");
        } catch (AuthenticationException $e) {
            $this->assertSame($expectedError, $e->normalize()['error']);
            $this->assertFalse($this->session->has(AuthUser::USER_ID), 'Sessão não deve conter usuário');
        }
    }

    private function setEnforcePasswordStrength(string $value): void
    {
        $this->getContainer()->get(Services::CONFIG_SERVICE)->getConfigDao()
            ->setValue(ConfigService::KEY_ENFORCE_PASSWORD_STRENGTH, $value);
    }

    // ---------------------------------------------------------------- credenciais

    /** IT-01: credenciais válidas -> autentica e grava identidade na sessão */
    public function testLoginValidoGravaIdentidadeNaSessao(): void
    {
        $this->assertTrue($this->login('ana', 'Ana@Segura#2026'));
        $this->assertSame(1, $this->session->get(AuthUser::USER_ID));
        $this->assertSame('Admin', $this->session->get(AuthUser::USER_ROLE_NAME));
        $this->assertSame(1, $this->session->get(AuthUser::USER_EMPLOYEE_NUMBER));
    }

    /** IT-02: senha incorreta -> false, sessão vazia */
    public function testSenhaIncorretaNaoAutentica(): void
    {
        $this->assertFalse($this->login('ana', 'SenhaErrada@1'));
        $this->assertFalse($this->session->has(AuthUser::USER_ID));
    }

    /** IT-03: usuário inexistente -> false */
    public function testUsuarioInexistenteNaoAutentica(): void
    {
        $this->assertFalse($this->login('naoexiste', 'Ana@Segura#2026'));
    }

    /** IT-04: senha é sensível a maiúsculas/minúsculas */
    public function testSenhaSensivelAMaiusculas(): void
    {
        $this->assertFalse($this->login('ana', 'ana@segura#2026'));
    }

    /**
     * IT-05: nome de usuário NÃO é sensível a maiúsculas/minúsculas
     * (comparação feita pela collation do banco). Comportamento documentado
     * no relatório; consistente com o servidor de demonstração.
     */
    public function testNomeDeUsuarioNaoSensivelAMaiusculas(): void
    {
        $this->assertTrue($this->login('ANA', 'Ana@Segura#2026'));
    }

    // ---------------------------------------------------------------- estado da conta

    /** IT-06: conta desabilitada */
    public function testContaDesabilitadaEhBloqueada(): void
    {
        $this->expectAuthError('bruno', 'Bruno@Segura#2026', AuthenticationException::USER_DISABLED);
    }

    /** IT-07: funcionário desligado */
    public function testFuncionarioDesligadoEhBloqueado(): void
    {
        $this->expectAuthError('carla', 'Carla@Segura#2026', AuthenticationException::EMPLOYEE_TERMINATED);
    }

    /** IT-08: usuário sem funcionário vinculado */
    public function testUsuarioSemFuncionarioEhBloqueado(): void
    {
        $this->expectAuthError('diego', 'Diego@Segura#2026', AuthenticationException::EMPLOYEE_NOT_ASSIGNED);
    }

    /** IT-09: usuário excluído é tratado como inexistente (não revela que existiu) */
    public function testUsuarioExcluidoNaoAutentica(): void
    {
        $this->assertFalse($this->login('eva', 'Eva@Segura#2026'));
    }

    // ---------------------------------------------------------------- transição de estado

    /** IT-10: hash MD5 legado -> login aceito e hash migrado para bcrypt */
    public function testHashLegadoEhMigradoNoLogin(): void
    {
        $this->assertTrue($this->login('gil', 'Gil@Legado#2020'));

        $this->getEntityManager()->clear();
        $stored = $this->getEntityManager()->getRepository(User::class)->find(7)->getUserPassword();
        $this->assertNotSame(md5('Gil@Legado#2020'), $stored, 'Hash MD5 deveria ter sido substituído');
        $this->assertTrue(password_verify('Gil@Legado#2020', $stored), 'Novo hash deve validar a mesma senha');

        // segundo login, agora com o hash novo
        $this->assertTrue($this->login('gil', 'Gil@Legado#2020'));
    }

    // ---------------------------------------------------------------- política de senha no login

    /** IT-11: política ligada + senha fraca -> PasswordEnforceException e registro de troca obrigatória */
    public function testPoliticaLigadaComSenhaFracaExigeTroca(): void
    {
        $this->setEnforcePasswordStrength('on');
        $userRoleManager = $this->getMockBuilder(MockUserRoleManager::class)
            ->onlyMethods(['getUser'])
            ->getMock();
        $userRoleManager->method('getUser')->willReturn($this->getEntityReference(User::class, 6));
        $this->getContainer()->set(Services::USER_ROLE_MANAGER, $userRoleManager);

        $repo = $this->getEntityManager()->getRepository(EnforcePasswordRequest::class);
        $before = count($repo->findBy(['user' => 6])); // tabela não é limpa pela fixture: mede a diferença

        $provider = new LocalAuthProvider();
        try {
            $provider->authenticate(new AuthParams(new UserCredential('fabio', 'fabio123')));
            $this->fail('Era esperada PasswordEnforceException');
        } catch (PasswordEnforceException $e) {
            $this->assertSame(AuthenticationException::PASSWORD_NOT_STRONG, $e->normalize()['error']);
            $this->assertStringStartsWith('changeWeakPassword/resetCode/', $e->getRedirectUrl());
        }

        $requests = $repo->findBy(['user' => 6], ['id' => 'DESC']);
        $this->assertCount($before + 1, $requests, 'Deve ser criado exatamente 1 pedido de troca de senha');
        $this->assertFalse($requests[0]->isExpired());
    }

    /** IT-12: política ligada + senha forte -> autentica normalmente */
    public function testPoliticaLigadaComSenhaForteAutentica(): void
    {
        $this->setEnforcePasswordStrength('on');
        $provider = new LocalAuthProvider();
        $this->assertTrue($provider->authenticate(new AuthParams(new UserCredential('ana', 'Ana@Segura#2026'))));
    }

    /** IT-13: política desligada + senha fraca -> autentica normalmente */
    public function testPoliticaDesligadaComSenhaFracaAutentica(): void
    {
        $this->setEnforcePasswordStrength('off');
        $provider = new LocalAuthProvider();
        $this->assertTrue($provider->authenticate(new AuthParams(new UserCredential('fabio', 'fabio123'))));
    }

    // ---------------------------------------------------------------- robustez

    /** IT-14: tentativa de SQL injection no usuário e na senha */
    public function testSqlInjectionNaoAutentica(): void
    {
        $this->assertFalse($this->login("' OR '1'='1", "' OR '1'='1"));
        $this->assertFalse($this->login("ana' -- ", 'qualquer'));
    }

    /** IT-15: usuário e senha vazios */
    public function testCredenciaisVaziasNaoAutenticam(): void
    {
        $this->assertFalse($this->login('', ''));
        $this->assertFalse($this->login('ana', ''));
    }
}
