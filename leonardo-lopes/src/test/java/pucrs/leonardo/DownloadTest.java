package pucrs.leonardo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadEmployeeDataTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private Path pastaDownload;

    @BeforeEach
    void abrirNavegador() throws IOException {
        pastaDownload = Files.createTempDirectory("orangehrm-downloads");

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", pastaDownload.toString());
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("safebrowsing.enabled", true);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");
        options.setExperimentalOption("prefs", prefs);

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @AfterEach
    void fecharNavegador() {
        if (driver != null) {
            try {
                System.out.println("URL ao finalizar: " + driver.getCurrentUrl());
            } finally {
                driver.quit();
            }
        }
    }

    @Test
    void devePesquisarEBaixarDadosEmpregado() {
        autenticarEAcessarMaintenance();

        By campoEmployeeName = By.xpath("//div[contains(@class,'oxd-autocomplete-text-input')]//input");
        WebElement inputName = wait.until(ExpectedConditions.visibilityOfElementLocated(campoEmployeeName));

        inputName.click();
        inputName.clear();
        inputName.sendKeys("John");

        By opcaoValida = By.xpath(
                "//div[@role='listbox']//div[@role='option']//span[not(contains(text(), 'Searching'))]");
        clicarComSeguranca(opcaoValida);

        By botaoSearch = By.xpath("//button[@type='submit' and normalize-space()='Search']");
        clicarComSeguranca(botaoSearch);

        By labelEmployeeId = By.xpath("//label[normalize-space()='Employee Id']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(labelEmployeeId));

        By botaoDownload = By.xpath("//button[normalize-space()='Download']");
        clicarComSeguranca(botaoDownload);

        boolean baixou = new WebDriverWait(driver, Duration.ofSeconds(30)).until(d -> {
            String[] arquivos = pastaDownload.toFile().list(
                    (dir, nome) -> !nome.endsWith(".crdownload") && !nome.endsWith(".tmp"));
            return arquivos != null && arquivos.length > 0;
        });

        assertTrue(baixou, "O arquivo com os dados do empregado deveria ter sido baixado.");
    }

    @Test
    void deveExibirErroQuandoEmpregadoNaoExiste() {
        autenticarEAcessarMaintenance();

        By campoEmployeeName = By.xpath("//div[contains(@class,'oxd-autocomplete-text-input')]//input");
        WebElement inputName = wait.until(ExpectedConditions.visibilityOfElementLocated(campoEmployeeName));
        inputName.sendKeys("Usuario Inexistente XYZ 9999");

        By semRegistros = By.xpath("//div[@role='listbox']//div[contains(., 'No Records Found')]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(semRegistros));

        By botaoSearch = By.xpath("//button[@type='submit' and normalize-space()='Search']");
        clicarComSeguranca(botaoSearch);

        By mensagemErro = By.xpath(
                "//div[contains(@class,'oxd-input-group')]"
                + "[.//label[normalize-space()='Employee Name']]"
                + "//span[contains(@class,'oxd-input-field-error-message')]"
        );

        WebElement spanErro = wait.until(ExpectedConditions.visibilityOfElementLocated(mensagemErro));
        assertEquals("Invalid", spanErro.getText());
    }

    private void autenticarEAcessarMaintenance() {
        driver.get("https://opensource-demo.orangehrmlive.com/web/index.php/auth/login");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("username"))).sendKeys("Admin");
        driver.findElement(By.name("password")).sendKeys("admin123");

        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']"))).click();
        wait.until(ExpectedConditions.urlContains("/dashboard/"));

        driver.get("https://opensource-demo.orangehrmlive.com/web/index.php/maintenance/accessEmployeeData");

        By campoPasswordMaintenance = By.name("password");
        wait.until(ExpectedConditions.visibilityOfElementLocated(campoPasswordMaintenance)).sendKeys("admin123");

        By botaoConfirmar = By.xpath("//button[@type='submit'][normalize-space()='Confirm']");
        wait.until(ExpectedConditions.elementToBeClickable(botaoConfirmar)).click();
    }

    private void clicarComSeguranca(By localizador) {
        new WebDriverWait(driver, Duration.ofSeconds(20))
                .ignoring(StaleElementReferenceException.class)
                .ignoring(ElementClickInterceptedException.class)
                .until(navegador -> {
                    for (WebElement camada : navegador.findElements(By.cssSelector(".oxd-form-loader"))) {
                        if (camada.isDisplayed()) {
                            return false;
                        }
                    }

                    WebElement elemento = navegador.findElement(localizador);
                    if (!elemento.isDisplayed() || !elemento.isEnabled()) {
                        return false;
                    }

                    elemento.click();
                    return true;
                });
    }
}