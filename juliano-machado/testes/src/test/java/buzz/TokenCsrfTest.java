package buzz;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** TU-06: extração do token CSRF da página de login. */
class TokenCsrfTest {

    @Test
    @DisplayName("TU-06 extrai o token da página de login")
    void extraiTokenDaPaginaDeLogin() {
        String html = "<auth-login :login-logo-src=\"&quot;x.png&quot;\" "
                + ":token=\"&quot;a1b2.C3d4-e5&quot;\" :is-demo-mode=\"true\"></auth-login>";
        assertEquals("a1b2.C3d4-e5", TokenCsrf.extrair(html));
    }

    @Test
    @DisplayName("TU-06 erro quando a página não tem token")
    void falhaQuandoNaoHaToken() {
        assertThrows(IllegalArgumentException.class, () -> TokenCsrf.extrair("<html></html>"));
        assertThrows(IllegalArgumentException.class, () -> TokenCsrf.extrair(null));
    }
}
