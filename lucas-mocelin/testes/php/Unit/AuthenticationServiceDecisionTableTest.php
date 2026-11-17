<?php

/**
 * Trabalho 2 - Teste do OrangeHRM - Jornada "Autenticação (Login)"
 *
 * TESTE UNITÁRIO - AuthenticationService::setCredentialsForUser()
 * Técnica: Tabela de Decisão (caixa-preta) + cobertura de decisões (caixa-branca).
 *
 * O método decide se um usuário encontrado pode iniciar sessão. As condições
 * (C1..C5) e ações esperadas estão no Plano de Testes (seção 4.1, casos UT-AS-01..07).
 *
 * Diferente dos testes originais (AuthenticationServiceTest), aqui as exceções
 * são verificadas com expectException(), de modo que o teste FALHA caso nenhuma
 * exceção seja lançada (os testes originais usam try/catch sem $this->fail()).
 *
 * Uso de IA: escrito com Claude (Anthropic, Claude Opus 5.5), revisado pelo autor.
 */

namespace OrangeHRM\Tests\Authentication\JornadaLogin\Unit;

use OrangeHRM\Authentication\Exception\AuthenticationException;
use OrangeHRM\Authentication\Service\AuthenticationService;
use OrangeHRM\Entity\Employee;
use OrangeHRM\Entity\EmployeeTerminationRecord;
use OrangeHRM\Entity\User;
use OrangeHRM\Entity\UserRole;
use OrangeHRM\Framework\Services;
use OrangeHRM\I18N\Service\I18NHelper;
use OrangeHRM\Tests\Util\KernelTestCase;

/**
 * @group JornadaLogin
 * @group Unit
 */
class AuthenticationServiceDecisionTableTest extends KernelTestCase
{
    protected function setUp(): void
    {
        // Dublê do tradutor: devolve a mensagem original (isola o teste do i18n/banco)
        $i18n = $this->getMockBuilder(I18NHelper::class)
            ->onlyMethods(['transBySource'])
            ->getMock();
        $i18n->method('transBySource')->willReturnArgument(0);
        $this->createKernelWithMockServices([Services::I18N_HELPER => $i18n]);
    }

    /**
     * Cria um usuário de teste com as condições da tabela de decisão.
     */
    private function buildUser(
        bool $deleted = false,
        bool $active = true,
        bool $withEmployee = true,
        bool $terminated = false
    ): User {
        $role = new UserRole();
        $role->setId(1);
        $role->setName('Admin');

        $user = new User();
        $user->setId(10);
        $user->setUserName('maria');
        $user->setUserRole($role);
        $user->setDeleted($deleted);
        $user->setStatus($active);

        if ($withEmployee) {
            $employee = new Employee();
            $employee->setEmpNumber(7);
            if ($terminated) {
                $employee->setEmployeeTerminationRecord(new EmployeeTerminationRecord());
            }
            $user->setEmployee($employee);
        }
        return $user;
    }

    /**
     * Serviço com setUserAttributes() isolado (dublê do tipo "spy"),
     * para não depender de sessão HTTP.
     */
    private function buildService(int $expectedAttributeCalls): AuthenticationService
    {
        $service = $this->getMockBuilder(AuthenticationService::class)
            ->onlyMethods(['setUserAttributes'])
            ->getMock();
        $service->expects($this->exactly($expectedAttributeCalls))
            ->method('setUserAttributes');
        return $service;
    }

    /** UT-AS-01: nenhum usuário encontrado (credencial inválida) -> false */
    public function testUsuarioInexistenteRetornaFalse(): void
    {
        $service = $this->buildService(0);
        $this->assertFalse($service->setCredentialsForUser(null));
    }

    /** UT-AS-02: usuário excluído logicamente -> false (mesmo com senha correta) */
    public function testUsuarioExcluidoRetornaFalse(): void
    {
        $service = $this->buildService(0);
        $this->assertFalse($service->setCredentialsForUser($this->buildUser(true)));
    }

    /** UT-AS-03: conta desabilitada -> exceção USER_DISABLED */
    public function testUsuarioDesabilitadoLancaExcecao(): void
    {
        $service = $this->buildService(0);
        $this->expectException(AuthenticationException::class);
        $this->expectExceptionMessage('Account disabled');
        $service->setCredentialsForUser($this->buildUser(false, false));
    }

    /** UT-AS-04: usuário sem funcionário vinculado -> exceção EMPLOYEE_NOT_ASSIGNED */
    public function testUsuarioSemFuncionarioLancaExcecao(): void
    {
        $service = $this->buildService(0);
        try {
            $service->setCredentialsForUser($this->buildUser(false, true, false));
            $this->fail('Era esperada AuthenticationException (employee not assigned)');
        } catch (AuthenticationException $e) {
            $this->assertSame(AuthenticationException::EMPLOYEE_NOT_ASSIGNED, $e->normalize()['error']);
        }
    }

    /** UT-AS-05: funcionário desligado -> exceção EMPLOYEE_TERMINATED */
    public function testFuncionarioDesligadoLancaExcecao(): void
    {
        $service = $this->buildService(0);
        try {
            $service->setCredentialsForUser($this->buildUser(false, true, true, true));
            $this->fail('Era esperada AuthenticationException (employee terminated)');
        } catch (AuthenticationException $e) {
            $this->assertSame(AuthenticationException::EMPLOYEE_TERMINATED, $e->normalize()['error']);
        }
    }

    /** UT-AS-06: usuário ativo, com funcionário ativo -> true e grava atributos uma vez */
    public function testUsuarioValidoRetornaTrueEGravaAtributos(): void
    {
        $service = $this->buildService(1);
        $this->assertTrue($service->setCredentialsForUser($this->buildUser()));
    }

    /**
     * UT-AS-07: precedência das regras. Usuário desabilitado E sem funcionário
     * E desligado: a primeira regra avaliada (conta desabilitada) deve prevalecer.
     */
    public function testPrecedenciaContaDesabilitadaSobreDemaisRegras(): void
    {
        $service = $this->buildService(0);
        try {
            $service->setCredentialsForUser($this->buildUser(false, false, false));
            $this->fail('Era esperada AuthenticationException');
        } catch (AuthenticationException $e) {
            $this->assertSame(AuthenticationException::USER_DISABLED, $e->normalize()['error']);
        }
    }
}
