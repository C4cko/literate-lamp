package t2.claim;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import t2.claim.pages.Browser;
import t2.claim.support.Cenario;
import t2.claim.support.Config;

import java.time.LocalDate;

/**
 * Base dos testes de sistema (plano, seção 6.3).
 * - Dados: um Cenario POR TESTE (funcionário, supervisor e usuários com sufixo único), removido no fim do teste.
 *   O servidor de demonstração é reiniciado periodicamente e apaga dados e sessões; com o cenário por classe, um
 *   reinício derrubava todos os testes seguintes (execução de 17/11/2026, 00:58). Por teste, só o que estiver em
 *   andamento é afetado.
 * - Isolamento: um navegador novo por teste; cada teste cria a própria reivindicação; nenhuma ordem entre testes.
 * - Oráculo: status e botões lidos na tela, sempre com assert do JUnit.
 */
abstract class SystemTestBase {
    static final LocalDate DATA_DA_DESPESA = LocalDate.of(2026, 11, 10);

    Cenario cenario;
    Browser browser;

    @BeforeEach
    void prepararCenarioENavegador() {
        cenario = Cenario.criar();
        browser = new Browser();
    }

    @AfterEach
    void fecharNavegadorERemoverCenario() {
        browser.close();
        cenario.remover();
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
