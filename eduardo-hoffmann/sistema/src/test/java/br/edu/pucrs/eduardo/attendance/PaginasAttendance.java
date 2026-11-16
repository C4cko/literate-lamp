package br.edu.pucrs.eduardo.attendance;

import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page Object das telas de Attendance e da configuração do OrangeHRM 5.9. Os seletores foram conferidos na
 * instância local (formulário "oxd-form"; interruptores "oxd-switch-input").
 */
final class PaginasAttendance {

    private static final By BOTAO_DO_FORMULARIO = By.cssSelector("form button[type='submit']");
    private static final By CAMPO_DATA = By.cssSelector("form .oxd-date-input input");
    private static final By CAMPO_HORA = By.cssSelector("form .oxd-time-input input");
    private static final By CAMPO_NOTA = By.cssSelector("form textarea");
    private static final By MENSAGEM_DE_ERRO = By.cssSelector(".oxd-input-field-error-message");

    private final WebDriver driver;
    private final WebDriverWait espera;

    PaginasAttendance(WebDriver driver) {
        this.driver = driver;
        this.espera = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    // ---------------------------------------------------------------- sessão

    void entrar() {
        driver.get(Ambiente.URL + "/web/index.php/auth/login");
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.name("username"))).sendKeys(Ambiente.USUARIO);
        driver.findElement(By.name("password")).sendKeys(Ambiente.SENHA);
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        espera.until(ExpectedConditions.urlContains("/dashboard/"));
    }

    // ---------------------------------------------------------------- navegação

    void abrirPunchIn() {
        driver.get(Ambiente.URL + "/web/index.php/attendance/punchIn");
        aguardarFormularioCarregado();
    }

    void abrirPunchOut() {
        driver.get(Ambiente.URL + "/web/index.php/attendance/punchOut");
        aguardarFormularioCarregado();
    }

    void abrirMeusRegistros() {
        driver.get(Ambiente.URL + "/web/index.php/attendance/viewMyAttendanceRecord");
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//h5[contains(.,'My Attendance Records')]")));
        // espera o contador ("(1) Record Found", "No Records Found") terminar de carregar
        espera.until(d -> {
            String texto = d.findElement(By.cssSelector(".orangehrm-horizontal-padding, .oxd-layout-context")).getText();
            return texto.contains("Record Found") || texto.contains("Records Found") || texto.contains("No Records Found");
        });
    }

    String urlAtual() {
        return driver.getCurrentUrl();
    }

    /** Espera até a URL terminar com o caminho informado (ex.: "/attendance/punchOut"). */
    void aguardarUrlTerminandoEm(String caminho) {
        espera.until(d -> d.getCurrentUrl().endsWith(caminho));
        aguardarFormularioCarregado();
    }

    /**
     * Espera o formulário de Punch In/Out estar pronto: a página é montada pelo Vue e data e hora só são
     * preenchidas depois da chamada a /current-datetime. Enquanto isso o botão pode estar coberto.
     */
    private void aguardarFormularioCarregado() {
        espera.ignoring(StaleElementReferenceException.class, NoSuchElementException.class).until(d ->
                !d.findElement(CAMPO_DATA).getAttribute("value").isBlank()
                        && !d.findElement(CAMPO_HORA).getAttribute("value").isBlank()
                        && d.findElement(BOTAO_DO_FORMULARIO).isDisplayed());
    }

    // ---------------------------------------------------------------- formulário de Punch In/Out

    void clicarNoBotaoDoFormulario() {
        aguardarFormularioCarregado();
        enviarFormularioComoEsta();
    }

    /** Clica no botão sem exigir data e hora preenchidas (para os casos em que elas são apagadas de propósito). */
    void enviarFormularioComoEsta() {
        espera.ignoring(org.openqa.selenium.ElementClickInterceptedException.class, StaleElementReferenceException.class)
                .until(d -> {
                    d.findElement(BOTAO_DO_FORMULARIO).click();
                    return true;
                });
    }

    String textoDoBotaoDoFormulario() {
        return driver.findElement(BOTAO_DO_FORMULARIO).getText().trim();
    }

    void escreverNota(String nota) {
        WebElement campo = driver.findElement(CAMPO_NOTA);
        campo.clear();
        campo.sendKeys(nota);
    }

    boolean dataEstaHabilitada() {
        return driver.findElement(CAMPO_DATA).isEnabled();
    }

    boolean horaEstaHabilitada() {
        return driver.findElement(CAMPO_HORA).isEnabled();
    }

    void preencherData(String dataAaaaMmDd) {
        substituirTexto(driver.findElement(CAMPO_DATA), dataAaaaMmDd);
    }

    /** Hora no formato de 12 h da tela, por exemplo "09:00 AM". */
    void preencherHora(String hora12h) {
        substituirTexto(driver.findElement(CAMPO_HORA), hora12h);
    }

    void apagarData() {
        substituirTexto(driver.findElement(CAMPO_DATA), "");
    }

    void apagarHora() {
        substituirTexto(driver.findElement(CAMPO_HORA), "");
    }

    private void substituirTexto(WebElement campo, String texto) {
        campo.click();
        campo.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        if (!texto.isEmpty()) {
            campo.sendKeys(texto);
        }
        campo.sendKeys(Keys.TAB);
    }

    /** Espera aparecer alguma mensagem de erro de campo e devolve todas, na ordem da tela. */
    List<String> aguardarMensagensDeErro() {
        espera.until(d -> !d.findElements(MENSAGEM_DE_ERRO).isEmpty());
        return driver.findElements(MENSAGEM_DE_ERRO).stream().map(WebElement::getText).toList();
    }

    /** Mensagem de erro do campo com o rótulo informado ("Date", "Time", "Note"), ou null se não houver. */
    String mensagemDeErroDoCampo(String rotulo) {
        By erro = By.xpath("//form//label[normalize-space()='" + rotulo
                + "']/ancestor::div[contains(concat(' ', normalize-space(@class), ' '), ' oxd-input-group ')][1]"
                + "//*[contains(@class,'oxd-input-field-error-message')]");
        List<WebElement> encontrados = driver.findElements(erro);
        return encontrados.isEmpty() ? null : encontrados.get(0).getText();
    }

    /** Espera o campo mostrar exatamente a mensagem informada. */
    void aguardarMensagemDeErroDoCampo(String rotulo, String mensagem) {
        try {
            espera.until(d -> mensagem.equals(mensagemDeErroDoCampo(rotulo)));
        } catch (org.openqa.selenium.TimeoutException e) {
            throw new AssertionError("Campo " + rotulo + ": esperava \"" + mensagem + "\", mas a tela mostra Date="
                    + mensagemDeErroDoCampo("Date") + ", Time=" + mensagemDeErroDoCampo("Time")
                    + ", Note=" + mensagemDeErroDoCampo("Note")
                    + " | data='" + driver.findElement(CAMPO_DATA).getAttribute("value") + "' hora='"
                    + driver.findElement(CAMPO_HORA).getAttribute("value") + "' url=" + driver.getCurrentUrl()
                    + " erros(css)=" + driver.findElements(MENSAGEM_DE_ERRO).stream().map(WebElement::getText).toList(), e);
        }
    }

    /** Espera uma mensagem de erro específica aparecer (a validação de sobreposição é assíncrona). */
    void aguardarMensagemDeErro(String texto) {
        espera.until(d -> d.findElements(MENSAGEM_DE_ERRO).stream().anyMatch(e -> e.getText().contains(texto)));
    }

    // ---------------------------------------------------------------- Meus registros

    String textoDaPaginaDeRegistros() {
        return driver.findElement(By.cssSelector(".oxd-layout-context")).getText();
    }

    // ---------------------------------------------------------------- Attendance > Configuration

    /** Liga ou desliga "Employee can change current time when punching in/out" e salva. */
    void definirAlterarHoraAtual(boolean ligado) {
        driver.get(Ambiente.URL + "/web/index.php/attendance/configure");
        By interruptor = By.cssSelector("form .oxd-switch-wrapper:first-of-type .oxd-switch-input, form .oxd-switch-input");
        WebElement campo = espera.until(ExpectedConditions.elementToBeClickable(interruptor));
        boolean ligadoAgora = driver.findElement(By.cssSelector("form input[type='checkbox']")).isSelected();
        if (ligadoAgora != ligado) {
            campo.click();
        }
        driver.findElement(BOTAO_DO_FORMULARIO).click();
        espera.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".oxd-toast")));
    }
}
