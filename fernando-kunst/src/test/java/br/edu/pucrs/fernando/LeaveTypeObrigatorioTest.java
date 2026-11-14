package br.edu.pucrs.fernando;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeaveTypeObrigatorioTest {

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
                System.out.println("Conteudo da tela: " + driver.findElement(By.tagName("body")).getText());
            } finally {
                driver.quit();
            }
        }
    }

    @Test
    void deveExigirTipoDeLicenca() {
        driver.get(
                "https://opensource-demo.orangehrmlive.com/web/index.php/auth/login"
        );

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.name("username")
        )).sendKeys("Admin");

        driver.findElement(By.name("password")).sendKeys("admin123");

        wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button[type='submit']")
        )).click();

        wait.until(ExpectedConditions.urlContains("/dashboard/"));

        driver.get(
                "https://opensource-demo.orangehrmlive.com/web/index.php/leave/applyLeave"
        );

        By campoTipo = By.xpath(
                "//div[contains(@class,'oxd-input-group')]"
                        + "[.//label[normalize-space()='Leave Type']]"
        );

        By dataInicial = By.xpath(
                "//div[contains(@class,'oxd-input-group')]"
                        + "[.//label[normalize-space()='From Date']]//input"
        );

        By dataFinal = By.xpath(
                "//div[contains(@class,'oxd-input-group')]"
                        + "[.//label[normalize-space()='To Date']]//input"
        );

        wait.until(ExpectedConditions.visibilityOfElementLocated(campoTipo));

        String formato = wait.until(
                ExpectedConditions.visibilityOfElementLocated(dataInicial)
        ).getAttribute("placeholder");

        DateTimeFormatter formatador = DateTimeFormatter.ofPattern(
                formato.replace("yyyy", "uuuu").replace("mm", "MM")
        );

        LocalDate data = LocalDate.now()
                .plusWeeks(2)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        String dataFormatada = data.format(formatador);

        preencherData(dataInicial, dataFormatada);
        preencherData(dataFinal, dataFormatada);

        By botaoApply = By.cssSelector("button[type='submit']");

        // O formulário pode recriar o botão após a alteração das datas.
        wait.ignoring(StaleElementReferenceException.class)
                .until(navegador -> {
                    WebElement botao = navegador.findElement(botaoApply);

                    if (!botao.isDisplayed() || !botao.isEnabled()) {
                        return false;
                    }

                    botao.click();
                    return true;
                });

        By mensagemObrigatoria = By.xpath(
                "//div[contains(@class,'oxd-input-group')]"
                        + "[.//label[normalize-space()='Leave Type']]"
                        + "//span[normalize-space()='Required']"
        );

        WebElement mensagem = wait.until(
                ExpectedConditions.visibilityOfElementLocated(mensagemObrigatoria)
        );

        assertEquals("Required", mensagem.getText());

        wait.until(ExpectedConditions.visibilityOfElementLocated(dataInicial));
    }

    private void preencherData(By localizador, String valor) {
        Keys modificador = Keys.CONTROL;

        if (System.getProperty("os.name").startsWith("Mac")) {
            modificador = Keys.COMMAND;
        }

        Keys teclaSelecao = modificador;

        wait.ignoring(StaleElementReferenceException.class)
                .until(navegador -> {
                    WebElement campo = navegador.findElement(localizador);

                    if (!campo.isDisplayed() || !campo.isEnabled()) {
                        return false;
                    }

                    campo.sendKeys(Keys.chord(teclaSelecao, "a"));
                    campo.sendKeys(valor);
                    campo.sendKeys(Keys.TAB);
                    return true;
                });
    }
}