package t2.claim.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import t2.claim.support.Config;

/**
 * Navegador de um teste, com esperas explícitas (nunca Thread.sleep) e localizadores por rótulo e texto
 * (nunca XPath absoluto). O Selenium Manager baixa o ChromeDriver compatível com o Chrome instalado.
 */
public final class Browser implements AutoCloseable {
    final WebDriver driver;
    final WebDriverWait wait;

    public Browser() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1400,1000");
        if (Config.HEADLESS) {
            options.addArguments("--headless=new");
        }
        driver = new ChromeDriver(options);
        // o padrão do ChromeDriver é 300 s; com o servidor de demonstração lento, uma página presa travava o teste
        // por 5 minutos (execução de 17/11/2026, 01:28). 90 s ainda tolera a lentidão normal da demo.
        driver.manage().timeouts().pageLoadTimeout(java.time.Duration.ofSeconds(90));
        wait = new WebDriverWait(driver, Config.TIMEOUT);
        wait.ignoring(StaleElementReferenceException.class);
    }

    void open(String path) {
        driver.get(Config.BASE_URL + path);
    }

    /**
     * Troca de usuário: encerra a sessão no servidor (/auth/logout) e faz login pela tela.
     * A 1ª versão apagava os cookies com o dashboard ainda carregando; uma requisição em andamento devolvia o cookie
     * da sessão antiga, o token CSRF da página de login deixava de valer e o login falhava de forma intermitente.
     */
    public void loginAs(String user, String password) {
        open("/auth/logout"); // sem sessão, redireciona direto para a página de login
        wait.until(ExpectedConditions.urlContains("/auth/login"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("username"))).sendKeys(user);
        driver.findElement(By.name("password")).sendKeys(password);
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        new WebDriverWait(driver, Config.TIMEOUT) // espera própria: withMessage altera o objeto
                .withMessage(() -> "login de " + user + " não chegou ao dashboard; aviso na tela: " + loginAlert())
                .until(ExpectedConditions.urlContains("/dashboard"));
    }

    private String loginAlert() {
        return driver.findElements(By.cssSelector(".oxd-alert-content-text")).stream()
                .map(WebElement::getText)
                .findFirst()
                .orElse("(nenhum)");
    }

    // ------------------------------------------------------------------ campos do OXD (design system do OrangeHRM)

    /** Grupo de um campo de formulário pelo texto do rótulo. */
    static String group(String label) {
        return "//label[normalize-space()='" + label + "']/ancestor::div[contains(@class,'oxd-input-group')]";
    }

    WebElement visible(By by) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    void click(By by) {
        wait.until(ExpectedConditions.elementToBeClickable(by)).click();
    }

    WebElement input(String label) {
        return visible(By.xpath(group(label) + "//input"));
    }

    WebElement textarea(String label) {
        return visible(By.xpath(group(label) + "//textarea"));
    }

    void select(String label, String option) {
        click(By.xpath(group(label) + "//div[contains(@class,'oxd-select-text')]"));
        click(By.xpath("//div[@role='listbox']//*[@role='option' and normalize-space()='" + option + "']"));
    }

    /** Mensagem de validação exibida sob o campo. */
    public String fieldError(String label) {
        return visible(By.xpath(group(label) + "//span[contains(@class,'oxd-input-field-error-message')]")).getText();
    }

    /** Espera o aviso (toast) de sucesso que a tela mostra depois de gravar. */
    void waitSuccessToast() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'oxd-toast') and contains(.,'Success')]")));
    }

    @Override
    public void close() {
        driver.quit();
    }
}
