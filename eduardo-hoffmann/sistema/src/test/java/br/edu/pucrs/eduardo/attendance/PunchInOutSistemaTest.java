package br.edu.pucrs.eduardo.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * T2 VERVAL (PUCRS): ST-01 a ST-09 de docs/02-casos-de-teste.md.
 * Jornada "funcionário registra entrada e saída" pela interface do OrangeHRM 5.9 LOCAL (ver Ambiente).
 * Cada teste começa sem registros de ponto e com a opção "Employee can change current time" desligada.
 */
class PunchInOutSistemaTest {

    private static final DateTimeFormatter DATA_DA_TELA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HORA_DA_TELA = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

    private WebDriver driver;
    private PaginasAttendance paginas;

    @BeforeEach
    void abrirNavegadorEPrepararEstado() {
        BancoDeTeste.restaurarEstadoInicial();

        ChromeOptions opcoes = new ChromeOptions();
        if (Ambiente.SEM_JANELA) {
            opcoes.addArguments("--headless=new");
        }
        opcoes.addArguments("--window-size=1280,900");
        driver = new ChromeDriver(opcoes);
        paginas = new PaginasAttendance(driver);
        paginas.entrar();
    }

    @AfterEach
    void fecharNavegadorELimpar() {
        if (driver != null) {
            driver.quit();
        }
        BancoDeTeste.restaurarEstadoInicial();
    }

    // ---------------------------------------------------------------- ST-01: T1, fluxo principal (entrada)

    @Test
    void st01EntradaLevaParaATelaDePunchOut() {
        paginas.abrirPunchIn();
        assertEquals("In", paginas.textoDoBotaoDoFormulario());

        paginas.clicarNoBotaoDoFormulario();

        paginas.aguardarUrlTerminandoEm("/attendance/punchOut");
        assertEquals("Out", paginas.textoDoBotaoDoFormulario());
        assertEquals(1, BancoDeTeste.contarRegistrosComEstado("PUNCHED IN"));
    }

    // ---------------------------------------------------------------- ST-02: T2, fluxo principal (saída)

    @Test
    void st02SaidaAparecemEmMeusRegistros() {
        paginas.abrirPunchIn();
        paginas.clicarNoBotaoDoFormulario();
        paginas.aguardarUrlTerminandoEm("/attendance/punchOut");

        paginas.clicarNoBotaoDoFormulario();
        paginas.aguardarUrlTerminandoEm("/attendance/punchIn");

        assertEquals(1, BancoDeTeste.contarRegistrosComEstado("PUNCHED OUT"));
        paginas.abrirMeusRegistros();
        String pagina = paginas.textoDaPaginaDeRegistros();
        assertTrue(pagina.contains("(1) Record Found"), pagina);
        assertTrue(pagina.contains("Punch In"), pagina);
        assertTrue(pagina.contains("Punch Out"), pagina);
    }

    // ---------------------------------------------------------------- ST-03: nota de 250 e de 251 caracteres

    @Test
    void st03NotaDe250CaracteresEhAceita() {
        paginas.abrirPunchIn();
        paginas.escreverNota("a".repeat(250));

        paginas.clicarNoBotaoDoFormulario();

        paginas.aguardarUrlTerminandoEm("/attendance/punchOut");
        assertEquals(1, BancoDeTeste.contarRegistros());
    }

    @Test
    void st03NotaDe251CaracteresEhRejeitadaESemGravacao() {
        paginas.abrirPunchIn();
        paginas.escreverNota("a".repeat(251));

        paginas.clicarNoBotaoDoFormulario();

        List<String> mensagens = paginas.aguardarMensagensDeErro();
        assertFalse(mensagens.isEmpty());
        assertTrue(paginas.urlAtual().endsWith("/attendance/punchIn"), paginas.urlAtual());
        assertEquals(0, BancoDeTeste.contarRegistros());
        System.out.println("ST-03 mensagem de limite da nota: " + mensagens);
    }

    // ---------------------------------------------------------------- ST-04: campos obrigatórios (opção ligada)

    @Test
    void st04DataEHoraVaziasMostramRequiredEImpedemOEnvio() {
        paginas.definirAlterarHoraAtual(true);
        paginas.abrirPunchIn();

        paginas.apagarData();
        paginas.apagarHora();
        paginas.enviarFormularioComoEsta();

        paginas.aguardarMensagemDeErroDoCampo("Date", "Required");
        paginas.aguardarMensagemDeErroDoCampo("Time", "Required");
        assertTrue(paginas.urlAtual().endsWith("/attendance/punchIn"), paginas.urlAtual());
        assertEquals(0, BancoDeTeste.contarRegistros());
    }

    // ---------------------------------------------------------------- ST-05: sobreposição (opção ligada)

    @Test
    void st05HoraDentroDeRegistroConcluidoMostraOverlappingRecordsFound() {
        LocalDate ontem = LocalDate.now().minusDays(1);
        BancoDeTeste.semearRegistroConcluido(ontem.atTime(10, 0), ontem.atTime(12, 0));
        paginas.definirAlterarHoraAtual(true);
        paginas.abrirPunchIn();

        paginas.preencherData(ontem.format(DATA_DA_TELA));
        paginas.preencherHora(LocalTime.of(11, 0).format(HORA_DA_TELA));

        paginas.aguardarMensagemDeErro("Overlapping Records Found");
        paginas.clicarNoBotaoDoFormulario();
        assertTrue(paginas.urlAtual().endsWith("/attendance/punchIn"), paginas.urlAtual());
        assertEquals(1, BancoDeTeste.contarRegistros());
    }

    // ---------------------------------------------------------------- ST-06: T3, novo ciclo cria outro registro

    @Test
    void st06NovaEntradaDepoisDaSaidaCriaOutroRegistro() {
        for (int ciclo = 1; ciclo <= 2; ciclo++) {
            paginas.abrirPunchIn();
            paginas.clicarNoBotaoDoFormulario();
            paginas.aguardarUrlTerminandoEm("/attendance/punchOut");
            paginas.clicarNoBotaoDoFormulario();
            paginas.aguardarUrlTerminandoEm("/attendance/punchIn");
        }

        assertEquals(2, BancoDeTeste.contarRegistrosComEstado("PUNCHED OUT"));
        paginas.abrirMeusRegistros();
        String pagina = paginas.textoDaPaginaDeRegistros();
        assertTrue(pagina.contains("(2) Records Found"), pagina);
    }

    // ---------------------------------------------------------------- ST-07: A1, hora retroativa (opção ligada)

    @Test
    void st07EntradaRetroativaGravaOHorarioInformado() {
        paginas.definirAlterarHoraAtual(true);
        paginas.abrirPunchIn();
        LocalDate anteontem = LocalDate.now().minusDays(2);

        paginas.preencherData(anteontem.format(DATA_DA_TELA));
        paginas.preencherHora(LocalTime.of(9, 0).format(HORA_DA_TELA));
        paginas.clicarNoBotaoDoFormulario();

        paginas.aguardarUrlTerminandoEm("/attendance/punchOut");
        String gravado = BancoDeTeste.horaDeEntradaDoUsuario();
        assertNotNull(gravado);
        assertEquals(LocalDateTime.of(anteontem, LocalTime.of(9, 0)).format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), gravado);
    }

    // ---------------------------------------------------------------- ST-08: CA8, opção desligada

    @Test
    void st08ComAOpcaoDesligadaDataEHoraFicamDesabilitadas() {
        paginas.abrirPunchIn();

        assertFalse(paginas.dataEstaHabilitada());
        assertFalse(paginas.horaEstaHabilitada());
    }

    @Test
    void st08ComAOpcaoLigadaDataEHoraFicamHabilitadas() {
        paginas.definirAlterarHoraAtual(true);
        paginas.abrirPunchIn();

        assertTrue(paginas.dataEstaHabilitada());
        assertTrue(paginas.horaEstaHabilitada());
    }

    // ---------------------------------------------------------------- ST-09: CA4 pela interface (redirecionamento)

    @Test
    void st09PunchInComEntradaAbertaRedirecionaParaPunchOut() {
        paginas.abrirPunchIn();
        paginas.clicarNoBotaoDoFormulario();
        paginas.aguardarUrlTerminandoEm("/attendance/punchOut");

        paginas.abrirPunchIn();

        paginas.aguardarUrlTerminandoEm("/attendance/punchOut");
        assertEquals("Out", paginas.textoDoBotaoDoFormulario());
        assertEquals(1, BancoDeTeste.contarRegistros());
    }
}
