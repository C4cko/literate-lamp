package buzz;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TU-01 a TU-03: valor-limite e particionamento do tamanho do texto. */
class RegraTextoTest {

    // TU-01: limites 0, 1, 65529, 65530 e 65531
    @DisplayName("TU-01 limites 0, 1, 65529, 65530 e 65531")
    @ParameterizedTest(name = "{0} caracteres -> {1}")
    @CsvSource({
            "0, false",
            "1, true",
            "65529, true",
            "65530, true",
            "65531, false"
    })
    void tamanhoDoPost(int tamanho, boolean esperado) {
        assertEquals(esperado, RegraTexto.postValido("a".repeat(tamanho)));
    }

    // TU-02: partição inválida "texto ausente"
    @DisplayName("TU-02 texto nulo (partição texto ausente)")
    @Test
    void postNuloEhInvalido() {
        assertFalse(RegraTexto.postValido(null));
    }

    // TU-03: caractere com acento ou emoji conta como 1, não pelo número de bytes
    @DisplayName("TU-03 caractere com acento ou emoji conta como 1, não pelo número de bytes")
    @Test
    void contaCaracteresENaoBytes() {
        String emoji = "😀"; // 1 caractere, 2 unidades UTF-16
        assertEquals(1, RegraTexto.tamanho(emoji));
        assertTrue(RegraTexto.postValido("ç".repeat(65530)));
        assertTrue(RegraTexto.postValido(emoji.repeat(65530)));
        assertFalse(RegraTexto.postValido(emoji.repeat(65531)));
    }
}
