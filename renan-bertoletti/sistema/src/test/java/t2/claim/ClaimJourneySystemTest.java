package t2.claim;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import t2.claim.pages.ClaimDetailPage;
import t2.claim.pages.ClaimFormPage;
import t2.claim.support.Config;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ST-01 a ST-07 e ST-12 do plano de testes de Claim (seção 6.3): jornada, fluxos alternativos e botões por papel.
 * Técnicas: teste baseado em casos de uso (FP, A1–A8) e em estados (transições W1–W12 e tabela de ações, seção 2.5).
 */
class ClaimJourneySystemTest extends SystemTestBase {

    @Test
    @DisplayName("ST-01 jornada completa: funcionário submete, supervisor aprova, Admin paga")
    void st01JornadaCompleta() {
        entrarComoEss();
        int id = ClaimFormPage.submitClaim(browser).defaults().remarks("ST-01 " + cenario.sufixo).create();
        ClaimDetailPage ess = ClaimDetailPage.asOwner(browser, id);
        assertEquals("Initiated", ess.status());
        assertTrue(ess.referenceId().matches("\\d{15}"), "AAAAMMDD + id com 7 dígitos: " + ess.referenceId());
        ess.addExpense(Config.EXPENSE_TYPE, DATA_DA_DESPESA, "10.50", "10.50");
        ess.act("Submit", "Submitted");

        entrarComoSupervisor();
        ClaimDetailPage sup = ClaimDetailPage.asManager(browser, id).act("Approve", "Approved");
        assertEquals(List.of(), sup.actions(), "CA2: o supervisor não tem mais ações");

        entrarComoAdmin();
        ClaimDetailPage admin = ClaimDetailPage.asManager(browser, id).act("Pay", "Paid");
        assertEquals(List.of(), admin.actions(), "CA3");

        entrarComoEss();
        ClaimDetailPage fim = ClaimDetailPage.asOwner(browser, id);
        assertEquals("Paid", fim.status());
        assertEquals(List.of(), fim.actions(), "CA3");
        assertTrue(fim.total().contains("10.50"), fim.total());
    }

    @Test
    @DisplayName("ST-02 rejeição pelo supervisor e ressubmissão pelo funcionário (A3)")
    void st02RejeicaoERessubmissao() {
        int id = cenario.reivindicacaoDoEss("SUBMIT");

        entrarComoSupervisor();
        ClaimDetailPage.asManager(browser, id).act("Reject", "Rejected");

        entrarComoEss();
        ClaimDetailPage ess = ClaimDetailPage.asOwner(browser, id);
        assertEquals(List.of("Submit"), ess.actions(), "CA4: o funcionário vê só Submit");
        ess.addExpense(Config.EXPENSE_TYPE, DATA_DA_DESPESA, "5.00", "15.50"); // corrige: inclui a despesa que faltava
        ess.act("Submit", "Submitted");
    }

    @Test
    @DisplayName("ST-03 Admin aprova uma reivindicação submetida e ela vai direto para Paid (W7, R1)")
    void st03AdminAprovaDireto() {
        int id = cenario.reivindicacaoDoEss("SUBMIT");

        entrarComoAdmin();
        ClaimDetailPage.asManager(browser, id).act("Approve", "Paid");
    }

    @Test
    @DisplayName("ST-04 cancelamento antes e depois de submeter (A1, A2)")
    void st04Cancelamento() {
        int iniciada = cenario.reivindicacaoDoEss();
        int submetida = cenario.reivindicacaoDoEss("SUBMIT");

        entrarComoEss();
        for (int id : new int[] {iniciada, submetida}) {
            ClaimDetailPage ess = ClaimDetailPage.asOwner(browser, id).act("Cancel", "Cancelled");
            assertEquals(List.of(), ess.actions(), "CA5: nenhuma ação depois de cancelar");
        }
    }

    @Test
    @DisplayName("ST-05 Admin atribui e submete: a reivindicação nasce Paid (A6, W5, R1)")
    void st05AtribuicaoPeloAdmin() {
        entrarComoAdmin();
        int id = ClaimFormPage.assignClaim(browser).employee(cenario.essNome).defaults()
                .remarks("ST-05 " + cenario.sufixo).create();
        ClaimDetailPage admin = ClaimDetailPage.asManager(browser, id);
        assertEquals("Initiated", admin.status());
        admin.addExpense(Config.EXPENSE_TYPE, DATA_DA_DESPESA, "10.50", "10.50");
        admin.act("Submit", "Paid");
    }

    @Test
    @DisplayName("ST-06 supervisor atribui e submete: a reivindicação nasce Approved (A7, W10)")
    void st06AtribuicaoPeloSupervisor() {
        entrarComoSupervisor();
        int id = ClaimFormPage.assignClaim(browser).employee(cenario.essNome).defaults()
                .remarks("ST-06 " + cenario.sufixo).create();
        ClaimDetailPage sup = ClaimDetailPage.asManager(browser, id);
        sup.addExpense(Config.EXPENSE_TYPE, DATA_DA_DESPESA, "10.50", "10.50");
        sup.act("Submit", "Approved");
    }

    @Test
    @DisplayName("ST-07 botões por papel em SUBMITTED e APPROVED (seção 2.5, R6)")
    void st07BotoesPorPapel() {
        int id = cenario.reivindicacaoDoEss("SUBMIT");

        entrarComoEss();
        assertEquals(List.of("Cancel"), ClaimDetailPage.asOwner(browser, id).actions(), "SUBMITTED, ESS");
        entrarComoSupervisor();
        assertEquals(List.of("Approve", "Reject"), ClaimDetailPage.asManager(browser, id).actions(), "SUBMITTED, supervisor");
        entrarComoAdmin();
        assertEquals(List.of("Approve", "Reject"), ClaimDetailPage.asManager(browser, id).actions(), "SUBMITTED, Admin");

        cenario.supervisor().action(id, "APPROVE"); // muda o estado de partida pela API

        entrarComoEss();
        assertEquals(List.of(), ClaimDetailPage.asOwner(browser, id).actions(), "APPROVED, ESS");
        entrarComoSupervisor();
        assertEquals(List.of(), ClaimDetailPage.asManager(browser, id).actions(), "APPROVED, supervisor (R6)");
        entrarComoAdmin();
        assertEquals(List.of("Pay", "Reject"), ClaimDetailPage.asManager(browser, id).actions(), "APPROVED, Admin");
    }

    @Test
    @DisplayName("ST-12 reivindicação atribuída pelo Admin e submetida pelo próprio funcionário (A8)")
    void st12AtribuidaPeloAdminSubmetidaPeloFuncionario() {
        int id = cenario.admin.assignClaim(cenario.essEmp, cenario.eventId, "ST-12 " + cenario.sufixo);
        cenario.admin.addExpense(id, cenario.expenseTypeId, 10.5);

        entrarComoEss();
        ClaimDetailPage.asOwner(browser, id).act("Submit", "Submitted");

        entrarComoSupervisor();
        assertEquals(List.of("Approve", "Reject"), ClaimDetailPage.asManager(browser, id).actions());
    }
}
