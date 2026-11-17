package br.edu.pucrs.vv.recrutamento.sistema;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import br.edu.pucrs.vv.recrutamento.cliente.Ambiente;
import br.edu.pucrs.vv.recrutamento.cliente.OrangeHrmApi;

/**
 * Base dos testes de sistema. A vaga é criada pela API (pré-condição, não é o que está sendo testado);
 * todo o resto acontece no navegador. No final, candidatos, vaga e funcionários criados são apagados.
 */
abstract class SistemaBase {

    protected static Playwright playwright;
    protected static Browser browser;
    protected static OrangeHrmApi api;
    protected static String nomeVaga;
    protected static long vaga;
    protected static String entrevistador;

    private static final List<Long> candidatos = new ArrayList<>();
    private static final List<String> contratados = new ArrayList<>();

    protected BrowserContext contexto;
    protected Page page;
    protected String url;

    @BeforeAll
    static void prepararAmbiente() {
        api = OrangeHrmApi.entrarComoAdmin();
        int gerente = api.funcionarios(1).get(0);
        entrevistador = api.funcionario(gerente).get("firstName").asText();
        nomeVaga = "Vaga UI " + sufixo();
        vaga = api.criarVaga(nomeVaga, api.primeiroCargo(), gerente);

        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(!Ambiente.navegadorVisivel())
                .setSlowMo(Ambiente.navegadorVisivel() ? 300 : 0));
    }

    @BeforeEach
    void abrirNavegador() {
        url = Ambiente.url();
        contexto = browser.newContext(new Browser.NewContextOptions().setViewportSize(1366, 900));
        contexto.setDefaultTimeout(30_000);
        page = contexto.newPage();
        new TelaLogin(page, url).entrar(Ambiente.usuario(), Ambiente.senha());
    }

    @AfterEach
    void fecharNavegador() {
        if (contexto != null) {
            contexto.close();
        }
    }

    @AfterAll
    static void limpar() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        if (api != null) {
            api.apagarCandidatos(List.copyOf(candidatos));
            api.apagarVagas(List.of(vaga));
            List<Integer> emps = new ArrayList<>();
            for (String sobrenome : contratados) {
                api.buscarFuncionarios(sobrenome).forEach(f -> emps.add(f.get("empNumber").asInt()));
            }
            api.apagarFuncionarios(emps);
        }
        candidatos.clear();
        contratados.clear();
    }

    protected static String sufixo() {
        return "VV" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    protected static void registrarCandidato(long id) {
        candidatos.add(id);
    }

    protected static void registrarContratacao(String sobrenome) {
        contratados.add(sobrenome);
    }

    protected TelaCandidato telaCandidato() {
        return new TelaCandidato(page, url);
    }

    protected TelaAcao telaAcao() {
        return new TelaAcao(page, url);
    }
}
