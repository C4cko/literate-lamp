<?php

/**
 * T2 VERVAL (PUCRS): IT-01 a IT-11, IT-13 e IT-14 do plano de testes de Claim (seções 3 e 6.2).
 * Árvore de transição (TT-01 a TT-09b), transições proibidas (SP-01 a SP-22) e ações disponíveis por papel.
 */

namespace OrangeHRM\Tests\Claim\T2ClaimIntegracao;

/**
 * @group Claim
 * @group T2Claim
 * @group T2ClaimIntegracao
 */
class StateMachineTest extends ClaimIntegrationTestCase
{
    // ------------------------------------------------------------------ árvore de transição (seção 3.2)

    /** IT-01 / TT-01: W2. */
    public function testIT01CancelarAntesDeSubmeter(): void
    {
        $this->assertAction(self::ESS, self::INITIATED, 'CANCEL', 'CANCELLED');
        $this->assertNull($this->dbClaim(self::INITIATED)['submitted_date'], 'nunca foi submetida');
    }

    /** IT-02 / TT-02: W1, W3. A data de submissão é gravada no SUBMIT e preservada no CANCEL. */
    public function testIT02SubmeterECancelar(): void
    {
        $this->assertAction(self::ESS, self::INITIATED, 'SUBMIT', 'SUBMITTED', '2026-11-10 10:00:00');
        $this->assertSame('2026-11-10 10:00:00', $this->dbClaim(self::INITIATED)['submitted_date']);

        $this->assertAction(self::ESS, self::INITIATED, 'CANCEL', 'CANCELLED', '2026-11-11 10:00:00');
        $this->assertSame('2026-11-10 10:00:00', $this->dbClaim(self::INITIATED)['submitted_date']);
    }

    /** IT-03 / TT-03: W1, W7. Caracteriza R1: o Admin aprovando leva direto a PAID. */
    public function testIT03AdminAprovaVaiDiretoParaPago(): void
    {
        $this->assertAction(self::ESS, self::INITIATED, 'SUBMIT', 'SUBMITTED');
        $this->assertAction(self::ADMIN, self::INITIATED, 'APPROVE', 'PAID');
    }

    /** IT-04 / TT-04: W1, W8, W4. A ressubmissão atualiza a data de submissão. */
    public function testIT04RejeitarERessubmeter(): void
    {
        $this->assertAction(self::ESS, self::INITIATED, 'SUBMIT', 'SUBMITTED', '2026-11-10 10:00:00');
        $this->assertAction(self::ADMIN, self::INITIATED, 'REJECT', 'REJECTED', '2026-11-11 10:00:00');
        $this->assertSame(['SUBMIT'], $this->allowedActions(self::ESS, self::INITIATED), 'CA4: o funcionário vê só Submit');

        $this->assertAction(self::ESS, self::INITIATED, 'SUBMIT', 'SUBMITTED', '2026-11-12 10:00:00');
        $this->assertSame('2026-11-12 10:00:00', $this->dbClaim(self::INITIATED)['submitted_date']);
    }

    /** IT-05 / TT-05: W1, W11, W6. */
    public function testIT05SupervisorAprovaEAdminRejeita(): void
    {
        $this->assertAction(self::ESS, self::INITIATED, 'SUBMIT', 'SUBMITTED');
        $this->assertAction(self::SUPERVISOR, self::INITIATED, 'APPROVE', 'APPROVED');
        $this->assertAction(self::ADMIN, self::INITIATED, 'REJECT', 'REJECTED');
    }

    /** IT-06 / TT-06: W1, W11, W9. Fluxo principal; ao final, ninguém tem ações (CA2, CA3). */
    public function testIT06FluxoPrincipalAtePago(): void
    {
        $this->assertAction(self::ESS, self::INITIATED, 'SUBMIT', 'SUBMITTED');
        $this->assertAction(self::SUPERVISOR, self::INITIATED, 'APPROVE', 'APPROVED');
        $this->assertSame([], $this->allowedActions(self::SUPERVISOR, self::INITIATED), 'CA2');

        $this->assertAction(self::ADMIN, self::INITIATED, 'PAY', 'PAID');
        foreach ([self::ESS, self::SUPERVISOR, self::ADMIN] as $user) {
            $this->assertSame([], $this->allowedActions($user, self::INITIATED), "CA3: usuário $user");
        }
    }

    /** IT-07 / TT-07: W1, W12. */
    public function testIT07SupervisorRejeita(): void
    {
        $this->assertAction(self::ESS, self::INITIATED, 'SUBMIT', 'SUBMITTED');
        $this->assertAction(self::SUPERVISOR, self::INITIATED, 'REJECT', 'REJECTED');
    }

    /** IT-08 / TT-08a: W5. Caracteriza R1: a reivindicação atribuída pelo Admin nasce paga. */
    public function testIT08AdminSubmeteAtribuidaPorEle(): void
    {
        $this->assertAction(self::ADMIN, self::ATRIBUIDA_PELO_ADMIN, 'SUBMIT', 'PAID');
    }

    /** IT-09 / TT-08b: W5 sobre o rascunho do funcionário. Caracteriza R7. */
    public function testIT09AdminSubmeteRascunhoDoFuncionario(): void
    {
        $this->assertAction(self::ADMIN, self::INITIATED, 'SUBMIT', 'PAID');
    }

    /** IT-10 / TT-09a: W10. */
    public function testIT10SupervisorSubmeteAtribuidaPorEle(): void
    {
        $this->assertAction(self::SUPERVISOR, self::ATRIBUIDA_PELO_SUPERVISOR, 'SUBMIT', 'APPROVED');
    }

    /** IT-11 / TT-09b: W10 sobre o rascunho do funcionário. Caracteriza R7. */
    public function testIT11SupervisorSubmeteRascunhoDoFuncionario(): void
    {
        $this->assertAction(self::SUPERVISOR, self::INITIATED, 'SUBMIT', 'APPROVED');
    }

    // ------------------------------------------------------------------ transições proibidas (seção 3.3)

    /**
     * IT-13: cada sneak path é recusado, e o banco não muda (estado e data de submissão).
     *
     * @dataProvider sneakPathProvider
     */
    public function testIT13TransicaoProibida(int $claimId, int $userId, string $action, int $expectedHttp): void
    {
        $before = $this->dbClaim($claimId);

        $response = $this->action($userId, $claimId, $action, '2026-11-15 10:00:00');

        $this->assertSame($expectedHttp, $response['http']);
        $this->assertSame($before, $this->dbClaim($claimId), 'o banco não pode mudar');
    }

    public function sneakPathProvider(): array
    {
        return [
            'SP-01 INITIATED, ESS, APPROVE' => [self::INITIATED, self::ESS, 'APPROVE', 403],
            'SP-02 INITIATED, Supervisor, APPROVE' => [self::INITIATED, self::SUPERVISOR, 'APPROVE', 403],
            'SP-03 INITIATED, Admin, PAY' => [self::INITIATED, self::ADMIN, 'PAY', 403],
            'SP-04 SUBMITTED, ESS, APPROVE' => [self::SUBMITTED, self::ESS, 'APPROVE', 403],
            'SP-05 SUBMITTED, ESS, PAY' => [self::SUBMITTED, self::ESS, 'PAY', 403],
            'SP-06 SUBMITTED, Supervisor, PAY' => [self::SUBMITTED, self::SUPERVISOR, 'PAY', 403],
            'SP-07 SUBMITTED, Admin, PAY' => [self::SUBMITTED, self::ADMIN, 'PAY', 403],
            'SP-08 APPROVED, ESS, CANCEL' => [self::APPROVED, self::ESS, 'CANCEL', 403],
            'SP-09 APPROVED, Supervisor, PAY' => [self::APPROVED, self::SUPERVISOR, 'PAY', 403],
            'SP-10 APPROVED, Supervisor, REJECT (R6)' => [self::APPROVED, self::SUPERVISOR, 'REJECT', 403],
            'SP-11 APPROVED, Admin, APPROVE' => [self::APPROVED, self::ADMIN, 'APPROVE', 403],
            'SP-12 REJECTED, ESS, CANCEL' => [self::REJECTED, self::ESS, 'CANCEL', 403],
            'SP-13 REJECTED, Supervisor, APPROVE' => [self::REJECTED, self::SUPERVISOR, 'APPROVE', 403],
            'SP-14 REJECTED, Admin, PAY' => [self::REJECTED, self::ADMIN, 'PAY', 403],
            'SP-15 CANCELLED, ESS, SUBMIT' => [self::CANCELLED, self::ESS, 'SUBMIT', 403],
            'SP-16 CANCELLED, Supervisor, APPROVE' => [self::CANCELLED, self::SUPERVISOR, 'APPROVE', 403],
            'SP-17 CANCELLED, Admin, APPROVE' => [self::CANCELLED, self::ADMIN, 'APPROVE', 403],
            'SP-18 PAID, ESS, SUBMIT' => [self::PAID, self::ESS, 'SUBMIT', 403],
            'SP-19 PAID, Supervisor, REJECT' => [self::PAID, self::SUPERVISOR, 'REJECT', 403],
            'SP-20 PAID, Admin, CANCEL' => [self::PAID, self::ADMIN, 'CANCEL', 403],
            'SP-21 SUBMITTED, Admin, FOO (ação inexistente)' => [self::SUBMITTED, self::ADMIN, 'FOO', 422],
            'SP-22 SUBMITTED, ESS sem vínculo, APPROVE' => [self::SUBMITTED, self::OUTRO, 'APPROVE', 403],
        ];
    }

    // ------------------------------------------------------------------ ações por papel e estado (seção 2.5)

    /**
     * IT-14: meta.allowedActions de cada papel em cada estado, incluindo as combinações "n. o." da seção 2.5.
     *
     * @dataProvider allowedActionsProvider
     */
    public function testIT14AcoesDisponiveis(int $claimId, int $userId, array $expected): void
    {
        $this->assertSame($expected, $this->allowedActions($userId, $claimId));
    }

    public function allowedActionsProvider(): array
    {
        return [
            'INITIATED, ESS' => [self::INITIATED, self::ESS, ['CANCEL', 'SUBMIT']],
            'INITIATED, Supervisor' => [self::INITIATED, self::SUPERVISOR, ['SUBMIT']],
            'INITIATED, Admin' => [self::INITIATED, self::ADMIN, ['SUBMIT']],
            'SUBMITTED, ESS' => [self::SUBMITTED, self::ESS, ['CANCEL']],
            'SUBMITTED, Supervisor' => [self::SUBMITTED, self::SUPERVISOR, ['APPROVE', 'REJECT']],
            'SUBMITTED, Admin' => [self::SUBMITTED, self::ADMIN, ['APPROVE', 'REJECT']],
            'APPROVED, ESS' => [self::APPROVED, self::ESS, []],
            'APPROVED, Supervisor' => [self::APPROVED, self::SUPERVISOR, []],
            'APPROVED, Admin' => [self::APPROVED, self::ADMIN, ['PAY', 'REJECT']],
            'REJECTED, ESS' => [self::REJECTED, self::ESS, ['SUBMIT']],
            'REJECTED, Supervisor' => [self::REJECTED, self::SUPERVISOR, []],
            'REJECTED, Admin' => [self::REJECTED, self::ADMIN, []],
            'CANCELLED, ESS' => [self::CANCELLED, self::ESS, []],
            'CANCELLED, Supervisor' => [self::CANCELLED, self::SUPERVISOR, []],
            'CANCELLED, Admin' => [self::CANCELLED, self::ADMIN, []],
            'PAID, ESS' => [self::PAID, self::ESS, []],
            'PAID, Supervisor' => [self::PAID, self::SUPERVISOR, []],
            'PAID, Admin' => [self::PAID, self::ADMIN, []],
        ];
    }
}
