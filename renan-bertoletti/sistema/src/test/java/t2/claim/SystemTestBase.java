package t2.claim;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import t2.claim.pages.Browser;
import t2.claim.support.Cenario;
import t2.claim.support.Config;

import java.time.LocalDate;

/**
 * Base dos testes de sistema (plano, seção 6.3).
 * - Dados: um Cenario por classe (funcionário, supervisor e usuários com sufixo único), removido no fim.
 * - Isolamento: um navegador novo por teste; cada teste cria a própria reivindicação; nenhuma ordem entre testes.
 * - Oráculo: status e botões lidos na tela, sempre com assert do JUnit.
 */
abstract class SystemTestBase {
    static final LocalDate DATA_DA_DESPESA = LocalDate.of(2026, 11, 10);

    static Cenario cenario;
    Browser browser;

    @BeforeAll
    static void criarCenario() {
        cenario = Cenario.criar();
    }

    @AfterAll
    static void removerCenario() {
        if (cenario != null) {
            cenario.remover();
        }
    }

    @BeforeEach
    void abrirNavegador() {
        browser = new Browser();
    }

    @AfterEach
    void fecharNavegador() {
        browser.close();
    }

    void entrarComoEss() {
        browser.loginAs(cenario.essUser, Cenario.SENHA);
    }

    void entrarComoSupervisor() {
        browser.loginAs(cenario.supUser, Cenario.SENHA);
    }

    void entrarComoAdmin() {
        browser.loginAs(Config.ADMIN_USER, Config.ADMIN_PASSWORD);
    }
}
