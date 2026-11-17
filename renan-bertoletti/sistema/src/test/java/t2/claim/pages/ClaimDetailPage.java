package t2.claim.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Tela de detalhes de uma reivindicação: dados (número de referência, status), despesas e botões de ação.
 * O dono a abre em /claim/submitClaim/id/{id}; o gestor, em /claim/assignClaim/id/{id}.
 * Componentes: SubmitClaim.vue, AssignClaim.vue, ClaimExpenses.vue, AddExpenseModal.vue e ClaimActionButtons.vue.
 */
public final class ClaimDetailPage {
    private static final By STATUS = By.xpath(Browser.group("Status") + "//input");
    private static final By ACTION_BUTTONS = By.xpath("//div[contains(@class,'orangehrm-action-button')]//button");
    private static final By TOTAL = By.xpath("//div[contains(@class,'orangehrm-bottom-container')]//p[contains(.,'Total Amount')]");
    private static final String DIALOG = "//div[contains(@class,'oxd-dialog-sheet')]";

    private final Browser b;

    private ClaimDetailPage(Browser b, String path) {
        this.b = b;
        b.open(path);
        // a tela carrega os dados e as ações numa única requisição: status preenchido = tela pronta
        b.wait.until(d -> !d.findElement(STATUS).getAttribute("value").isEmpty());
    }

    public static ClaimDetailPage asOwner(Browser b, int claimId) {
        return new ClaimDetailPage(b, "/claim/submitClaim/id/" + claimId);
    }

    public static ClaimDetailPage asManager(Browser b, int claimId) {
        return new ClaimDetailPage(b, "/claim/assignClaim/id/" + claimId);
    }

    public String status() {
        return b.driver.findElement(STATUS).getAttribute("value");
    }

    public String referenceId() {
        return b.input("Reference Id").getAttribute("value");
    }

    /** Rótulos dos botões de ação exibidos (sem o Back, que sempre aparece), em ordem alfabética. */
    public List<String> actions() {
        b.visible(By.xpath("//div[contains(@class,'orangehrm-action-button')]//button[normalize-space()='Back']"));
        return b.driver.findElements(ACTION_BUTTONS).stream()
                .map(WebElement::getText)
                .map(String::trim)
                .filter(label -> !label.equals("Back"))
                .sorted()
                .toList();
    }

    /** Clica no botão da ação e espera a tela recarregar com o novo status. */
    public ClaimDetailPage act(String button, String expectedStatus) {
        b.click(By.xpath("//div[contains(@class,'orangehrm-action-button')]//button[normalize-space()='" + button + "']"));
        b.waitSuccessToast();
        b.wait.until(ExpectedConditions.attributeToBe(STATUS, "value", expectedStatus));
        return this;
    }

    // ------------------------------------------------------------------ despesas

    public String total() {
        return b.visible(TOTAL).getText();
    }

    /** Abre o modal Add Expense e preenche tipo, data (no formato do usuário) e valor; não salva. */
    public ClaimDetailPage fillExpense(String type, LocalDate date, String amount) {
        b.click(By.xpath("//h6[normalize-space()='Expenses']/ancestor::div[contains(@class,'orangehrm-action-header')]//button[normalize-space()='Add']"));
        b.visible(By.xpath(DIALOG));
        b.select("Expense Type", type);
        WebElement dateInput = b.input("Date");
        // o formato de data é configurável no OrangeHRM; o placeholder diz qual é (ex.: yyyy-dd-mm)
        String pattern = dateInput.getAttribute("placeholder").replace("mm", "MM");
        dateInput.sendKeys(date.format(DateTimeFormatter.ofPattern(pattern)));
        b.input("Amount").sendKeys(amount);
        return this;
    }

    public void saveExpense() {
        b.click(By.xpath(DIALOG + "//button[@type='submit']"));
    }

    /** Adiciona uma despesa válida e espera o total refletir o novo valor. */
    public ClaimDetailPage addExpense(String type, LocalDate date, String amount, String expectedTotal) {
        fillExpense(type, date, amount).saveExpense();
        b.waitSuccessToast();
        b.wait.until(ExpectedConditions.textToBePresentInElementLocated(TOTAL, expectedTotal));
        return this;
    }

    /** Espera o fim de uma tentativa de gravação recusada: modal fechado ou aviso exibido. */
    public void waitSaveAttemptFinished() {
        b.wait.until(ExpectedConditions.or(
                ExpectedConditions.invisibilityOfElementLocated(By.xpath(DIALOG)),
                ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(@class,'oxd-toast')]"))));
    }

    public String expenseFieldError(String label) {
        return b.fieldError(label);
    }
}
