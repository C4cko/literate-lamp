package br.edu.pucrs.vv.recrutamento.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TokenCsrfTest {

    @Test
    @DisplayName("U30 - extrai o token da propriedade :token do componente de login")
    void extraiToken() {
        String html = """
                <div id="app">
                  <auth-login
                    :token="&quot;4f2a1c.Xy_9-k&quot;"
                    :login-logo-src="&quot;\\/web\\/images\\/ohrm_logo.png&quot;"
                  ></auth-login>
                </div>""";
        assertEquals("4f2a1c.Xy_9-k", TokenCsrf.extrair(html));
    }

    @Test
    @DisplayName("U30 - desfaz o escape de barra do json_encode")
    void desfazEscapeDeBarra() {
        assertEquals("a/b", TokenCsrf.extrair("<x :token=\"&quot;a\\/b&quot;\"></x>"));
    }

    @DisplayName("U31 - página sem token (ou token vazio) gera erro")
    @ParameterizedTest
    @ValueSource(strings = {"<html><body>Login</body></html>", "<x :token=\"&quot;&quot;\">", ""})
    void semToken(String html) {
        assertThrows(IllegalStateException.class, () -> TokenCsrf.extrair(html));
    }

    @Test
    @DisplayName("U31 - página nula gera erro")
    void paginaNula() {
        assertThrows(IllegalStateException.class, () -> TokenCsrf.extrair(null));
    }
}
