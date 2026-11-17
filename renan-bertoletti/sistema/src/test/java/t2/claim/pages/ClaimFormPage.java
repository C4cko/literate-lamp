package t2.claim.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import t2.claim.support.Config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formulários de criação: Claim › Submit Claim (pelo próprio funcionário) e Claim › Assign Claim (pelo gestor).
 * Campos de src/client/src/orangehrmClaimPlugin/pages/{submitClaim/SubmitClaimRequest,assignClaim/AssignClaimRequest}.vue.
 */
public final class ClaimFormPage {
    private static final Pattern ID_NA_URL = Pattern.compile("/id/(\\d+)");
    private static final By CREATE = By.xpath("//button[@type='submit' and normalize-space()='Create']");

    private final Browser b;

    private ClaimFormPage(Browser b, String path) {
        this.b = b;
        b.open(path);
        b.visible(CREATE);
    }

    public static ClaimFormPage submitClaim(Browser b) {
        return new ClaimFormPage(b, "/claim/submitClaim");
    }

    public static ClaimFormPage assignClaim(Browser b) {
        return new ClaimFormPage(b, "/claim/assignClaim");
    }

    public ClaimFormPage employee(String name) {
        b.input("Employee Name").sendKeys(name);
        b.click(By.xpath("//div[@role='listbox']//*[@role='option' and contains(normalize-space(),'" + name + "')]"));
        return this;
    }

    public ClaimFormPage event(String name) {
        b.select("Event", name);
        return this;
    }

    public ClaimFormPage currency(String name) {
        b.select("Currency", name);
        return this;
    }

    public ClaimFormPage remarks(String text) {
        b.textarea("Remarks").sendKeys(text);
        return this;
    }

    /** Preenche evento e moeda com os valores padrão (Config). */
    public ClaimFormPage defaults() {
        return event(Config.EVENT).currency(Config.CURRENCY_NAME);
    }

    /** Clica em Create e espera a tela de detalhes; devolve o id da reivindicação criada. */
    public int create() {
        b.click(CREATE);
        b.wait.until(ExpectedConditions.urlMatches(".*/id/\\d+$"));
        Matcher id = ID_NA_URL.matcher(b.driver.getCurrentUrl());
        if (!id.find()) {
            throw new IllegalStateException("id não encontrado na URL " + b.driver.getCurrentUrl());
        }
        return Integer.parseInt(id.group(1));
    }

    /** Clica em Create esperando que a validação da tela impeça o envio. */
    public void createExpectingValidationError() {
        b.click(CREATE);
    }

    public String currentUrl() {
        return b.driver.getCurrentUrl();
    }
}
