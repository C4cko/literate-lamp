package br.edu.pucrs.fernando;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolicitacaoLicencaTest {

    private static final String BASE_URL =
            "https://opensource-demo.orangehrmlive.com/web/index.php";

    private static final String TIPO_LICENCA = "CAN - Personal";

    private WebDriver driver;
    private WebDriverWait wait;

    private final LocalDate dataPedido = LocalDate.parse(
            System.getProperty("leave.date", "2026-11-30")
    );

    private String dataExibida;
    private final String comentario = "CT-SIS-02 " + UUID.randomUUID();


    @BeforeEach
    void abrirNavegador() {
        driver = new ChromeDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(25));
    }

    @AfterEach
    void fecharNavegador() {
        if (driver != null) {
            try {
                System.out.println("URL final: " + driver.getCurrentUrl());
                System.out.println("Tela final: " + driver.findElement(By.tagName("body")).getText());
            } finally {
                driver.quit();
            }
        }
    }

    @Test
    void deveSolicitarLicencaEConsultarPedido() {
        fazerLogin();

        driver.get(BASE_URL + "/leave/applyLeave");

        By seletorTipo = By.xpath(
                "//div[contains(@class,'oxd-input-group')]"
                        + "[.//label[normalize-space()='Leave Type']]"
                        + "//div[contains(@class,'oxd-select-text-input')]"
        );

        wait.until(ExpectedConditions.elementToBeClickable(seletorTipo))
                .click();

        wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
                "//*[@role='option'][normalize-space()='"
                        + TIPO_LICENCA + "']"
        ))).click();

        By campoInicial = campoData("From Date");
        By campoFinal = campoData("To Date");

        String formato = wait.until(
                ExpectedConditions.visibilityOfElementLocated(campoInicial)
        ).getAttribute("placeholder");

        DateTimeFormatter formatador = DateTimeFormatter.ofPattern(
                formato.replace("yyyy", "uuuu").replace("mm", "MM")
        );

        dataExibida = dataPedido.format(formatador);

        preencherData(campoInicial, dataExibida);
        preencherData(campoFinal, dataExibida);

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.tagName("textarea")
        )).sendKeys(comentario);

        By botaoApply = By.xpath(
                "//button[@type='submit'][normalize-space()='Apply']"
        );

        wait.until(ExpectedConditions.elementToBeClickable(botaoApply))
                .click();

        aguardarResultadoEnvio();

        abrirMinhaLista();

        By pedidoPendente = localizarPedido("Pending Approval");

        WebElement pedido = wait.until(
                ExpectedConditions.visibilityOfElementLocated(pedidoPendente)
        );

        assertEquals(
                1,
                driver.findElements(pedidoPendente).size(),
                "Deve existir apenas um pedido pendente com esses dados."
        );

        try {
            String dados = pedido.getText();

            assertTrue(dados.contains(dataExibida));
            assertTrue(dados.contains(TIPO_LICENCA));

            List<WebElement> celulas = pedido.findElements(
                    By.cssSelector(".oxd-table-cell")
            );

            WebElement duracao = celulas.get(4);

            assertEquals(
                    "1.00",
                    duracao.getText().trim(),
                    "A duração do pedido deve ser de um dia."
            );
        } finally {
            cancelarPedido(pedidoPendente);
        }
    }

    private void fazerLogin() {
        driver.get(BASE_URL + "/auth/login");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.name("username")
        )).sendKeys("Admin");

        driver.findElement(By.name("password")).sendKeys("admin123");

        wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button[type='submit']")
        )).click();

        wait.until(ExpectedConditions.urlContains("/dashboard/"));
    }

    private void abrirMinhaLista() {
        driver.get(BASE_URL + "/leave/viewMyLeaveList");

        preencherData(campoData("From Date"), dataExibida);
        preencherData(campoData("To Date"), dataExibida);

        wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
                "//button[@type='submit'][normalize-space()='Search']"
        ))).click();
    }

    private By campoData(String nome) {
        return By.xpath(
                "//div[contains(@class,'oxd-input-group')]"
                        + "[.//label[normalize-space()='" + nome + "']]//input"
        );
    }

    private void preencherData(By localizador, String valor) {
        Keys modificador = Keys.CONTROL;

        if (System.getProperty("os.name").startsWith("Mac")) {
            modificador = Keys.COMMAND;
        }

        final Keys teclaSelecao = modificador;

        wait.ignoring(StaleElementReferenceException.class)
                .until(navegador -> {
                    WebElement campo = navegador.findElement(localizador);
                    campo.sendKeys(Keys.chord(teclaSelecao, "a"));
                    campo.sendKeys(valor);
                    campo.sendKeys(Keys.TAB);
                    return valor.equals(campo.getAttribute("value"));
                });
    }

    private By localizarPedido(String status) {
        return By.xpath(
                "//div[contains(@class,'oxd-table-card')]"
                        + "[contains(.,'" + dataExibida + "')]"
                        + "[contains(.,'" + TIPO_LICENCA + "')]"
                        + "[contains(.,'" + status + "')]"
                        + "[contains(.,'" + comentario + "')]"
        );
    }

    private void aguardarResultadoEnvio() {
        try {
            String resultado = wait.until(navegador -> {
                List<WebElement> mensagens = navegador.findElements(
                        By.cssSelector(
                                ".oxd-toast--success, "
                                        + ".oxd-toast--error, "
                                        + ".oxd-input-field-error-message"
                        )
                );

                for (WebElement mensagem : mensagens) {
                    try {
                        if (mensagem.isDisplayed()) {
                            String classes = mensagem.getAttribute("class");

                            if (classes.contains("oxd-toast--success")) {
                                return "SUCESSO";
                            }

                            String texto = mensagem.getText().trim();

                            if (!texto.isEmpty()) {
                                return "Solicitação recusada: " + texto;
                            }
                        }
                    } catch (StaleElementReferenceException elementoAtualizado) {
                        // A próxima consulta lê novamente as mensagens do formulário.
                    }
                }

                return null;
            });

            assertEquals("SUCESSO", resultado);
        } catch (org.openqa.selenium.TimeoutException erro) {
            throw new AssertionError(
                    "O envio ficou sem confirmação. URL: "
                            + driver.getCurrentUrl()
                            + "\nTela: "
                            + driver.findElement(By.tagName("body")).getText(),
                    erro
            );
        }

    }

    private void cancelarPedido(By localizador) {
        wait.ignoring(StaleElementReferenceException.class)
                .until(navegador -> {
                    WebElement pedido = navegador.findElement(localizador);
                    List<WebElement> botoes = pedido.findElements(By.tagName("button"));
                    botoes.get(botoes.size() - 1).click();
                    return true;
                });

        By detalhes = By.xpath("//*[normalize-space()='View Leave Details']");
        wait.ignoring(StaleElementReferenceException.class)
                .until(navegador -> {
                    navegador.findElement(detalhes).click();
                    return true;
                });

        wait.until(ExpectedConditions.urlMatches(
                ".*/leave/viewLeaveRequest/[0-9]+.*"
        ));
        String enderecoPedido = driver.getCurrentUrl();
        System.out.println("Pedido desta execução: " + enderecoPedido);

        abrirMinhaLista();
        wait.ignoring(StaleElementReferenceException.class)
                .until(navegador -> {
                    navegador.findElement(localizador).findElement(By.xpath(
                            ".//button[normalize-space()='Cancel']"
                    )).click();
                    return true;
                });

        wait.until(ExpectedConditions.invisibilityOfElementLocated(localizador));
        driver.get(enderecoPedido);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
                "//*[normalize-space()='Cancelled']"
        )));
        assertEquals(enderecoPedido, driver.getCurrentUrl());
    }
}
