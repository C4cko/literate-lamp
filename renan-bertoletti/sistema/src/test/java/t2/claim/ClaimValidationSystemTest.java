package t2.claim;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import t2.claim.pages.ClaimDetailPage;
import t2.claim.pages.ClaimFormPage;
import t2.claim.support.Config;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ST-08 a ST-11 do plano de testes de Claim (seção 6.3): validações pela tela.
 * Técnicas: particionamento em equivalência e valor-limite (plano, seções 4.2 e 4.3); teste guiado por risco (R3).
 * Mensagens esperadas: lang-strings do OrangeHRM (installer/Migration/V5_0_0/lang-string/general.yaml e
 * V5_5_0/lang-string/claim.yaml).
 */
class ClaimValidationSystemTest extends SystemTestBase {

    private int totalDeReivindicacoesDoEss() {
        return cenario.ess().get("claim/requests").at("/meta/total").asInt();
    }

    @ParameterizedTest(name = "ST-08 campo obrigatório vazio: {0}")
    @CsvSource({"Event", "Currency"})
    void st08CamposObrigatorios(String campoVazio) {
        int antes = totalDeReivindicacoesDoEss();
        entrarComoEss();
        ClaimFormPage form = ClaimFormPage.submitClaim(browser);
        if (campoVazio.equals("Event")) {
            form.currency(Config.CURRENCY_NAME);
        } else {
            form.event(Config.EVENT);
        }

        form.createExpectingValidationError();

        assertEquals("Required", browser.fieldError(campoVazio));
        assertTrue(form.currentUrl().endsWith("/claim/submitClaim"), "continua no formulário: " + form.currentUrl());
        assertEquals(antes, totalDeReivindicacoesDoEss(), "nada criado");
    }

    @Test
    @DisplayName("ST-09 submeter reivindicação sem despesas (R3: caracteriza o comportamento da tela)")
    void st09SubmeterSemDespesas() {
        int id = cenario.ess().createOwnClaim(cenario.eventId, "ST-09 " + cenario.sufixo);

        entrarComoEss();
        ClaimDetailPage ess = ClaimDetailPage.asOwner(browser, id);
        assertTrue(ess.total().contains("0.00"), ess.total());
        assertEquals(List.of("Cancel", "Submit"), ess.actions(), "a tela oferece Submit mesmo sem despesas");
        ess.act("Submit", "Submitted"); // a tela não exige despesa: R3 também na interface
    }

    @ParameterizedTest(name = "ST-10 valor inválido {0}: \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "7000.123       | Should be a valid number (xxx.xx)",
            "10000000000.00 | Should be less than 10,000,000,000",
    })
    void st10ValorRecusadoPelaTela(String valor, String mensagem) {
        int id = cenario.ess().createOwnClaim(cenario.eventId, "ST-10 " + cenario.sufixo);

        entrarComoEss();
        ClaimDetailPage ess = ClaimDetailPage.asOwner(browser, id);
        ess.fillExpense(Config.EXPENSE_TYPE, DATA_DA_DESPESA, valor).saveExpense();

        assertEquals(mensagem, ess.expenseFieldError("Amount"));
        assertEquals(0, cenario.ess().expenses(id).size(), "nenhuma despesa gravada");
    }

    @Test
    @DisplayName("ST-10 valor 0: a tela envia, o servidor recusa e nada é gravado")
    void st10ValorZero() {
        int id = cenario.ess().createOwnClaim(cenario.eventId, "ST-10 zero " + cenario.sufixo);

        entrarComoEss();
        ClaimDetailPage ess = ClaimDetailPage.asOwner(browser, id);
        ess.fillExpense(Config.EXPENSE_TYPE, DATA_DA_DESPESA, "0").saveExpense();
        ess.waitSaveAttemptFinished();

        assertEquals(0, cenario.ess().expenses(id).size(), "despesa de valor 0 não pode ser gravada");
        ess = ClaimDetailPage.asOwner(browser, id);
        assertTrue(ess.total().contains("0.00"), ess.total());
    }

    @Test
    @DisplayName("ST-10 valor máximo 9 999 999 999,99 é aceito")
    void st10ValorMaximo() {
        int id = cenario.ess().createOwnClaim(cenario.eventId, "ST-10 max " + cenario.sufixo);

        entrarComoEss();
        ClaimDetailPage.asOwner(browser, id)
                .addExpense(Config.EXPENSE_TYPE, DATA_DA_DESPESA, "9999999999.99", "9,999,999,999.99");
        assertEquals(1, cenario.ess().expenses(id).size());
    }

    @Test
    @DisplayName("ST-11 Remarks com 1000 caracteres é aceito")
    void st11Remarks1000() {
        entrarComoEss();
        int id = ClaimFormPage.submitClaim(browser).defaults().remarks("r".repeat(1000)).create();
        assertEquals("Initiated", ClaimDetailPage.asOwner(browser, id).status());
    }

    @Test
    @DisplayName("ST-11 Remarks com 1001 caracteres é recusado")
    void st11Remarks1001() {
        int antes = totalDeReivindicacoesDoEss();
        entrarComoEss();
        ClaimFormPage form = ClaimFormPage.submitClaim(browser).defaults().remarks("r".repeat(1001));

        form.createExpectingValidationError();

        assertEquals("Should not exceed 1000 characters", browser.fieldError("Remarks"));
        assertEquals(antes, totalDeReivindicacoesDoEss(), "nada criado");
    }
}
