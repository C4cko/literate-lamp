package pucrs.leonardo;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadEmployeeDataTest {

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeEach
    void abrirNavegador() {
        driver = new ChromeDriver();
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
        inputName.sendKeys("John William Doe");

        By opcaoValida = By.xpath("//div[@role='listbox']//div[@role='option' and not(contains(., 'Searching'))][1]");
        WebElement itemSelecionado = wait.until(ExpectedConditions.elementToBeClickable(opcaoValida));
        itemSelecionado.click();

        By botaoSearch = By.xpath("//button[@type='submit' and normalize-space()='Search']");
        clicarComSeguranca(botaoSearch);

        By botaoDownloadJson = By.xpath("//button[normalize-space()='Download'] | //i[contains(@class, 'bi-download')]");
        WebElement btnDownload = wait.until(ExpectedConditions.elementToBeClickable(botaoDownloadJson));
        btnDownload.click();

        assertTrue(btnDownload.isDisplayed(), "O botão de download de dados do empregado foi acionado com sucesso.");
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
        wait.ignoring(StaleElementReferenceException.class)
                .ignoring(ElementClickInterceptedException.class)
                .until(navegador -> {
                    for (WebElement camada : navegador.findElements(By.cssSelector(".oxd-form-loader"))) {
                        if (camada.isDisplayed()) {
                            return false;
                        }
                    }

                    WebElement botao = navegador.findElement(localizador);
                    if (!botao.isDisplayed() || !botao.isEnabled()) {
                        return false;
                    }

                    botao.click();
                    return true;
                });
    }
}