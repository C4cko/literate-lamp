package br.edu.pucrs.vv.recrutamento.sistema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * U34 - teste unitário (sem tag) da expressão usada para achar rótulos na tela. A expressão roda no
 * navegador, então não pode usar a sintaxe \Q...\E, que só existe no Java (defeito achado na 1ª execução).
 */
class TextoExatoTest {

    @DisplayName("U34 - expressão não usa \\Q...\\E e casa só o texto exato")
    @ParameterizedTest
    @ValueSource(strings = {"Email", "Contact Number", "Vacancy", "Mark Interview Passed"})
    void textoExato(String rotulo) {
        Pattern p = Tela.textoExato(rotulo);
        assertFalse(p.pattern().contains("\\Q"), p.pattern());
        assertTrue(p.matcher(rotulo).matches());
        assertTrue(p.matcher("  " + rotulo + " ").matches());
        assertFalse(p.matcher(rotulo + " Extra").matches());
    }

    @Test
    @DisplayName("U34 - caracteres especiais são escapados")
    void escapaCaracteresEspeciais() {
        Pattern p = Tela.textoExato("Allows numbers and only + - / ( )");
        assertEquals("^\\s*Allows numbers and only \\+ - \\/ \\( \\)\\s*$", p.pattern());
        assertTrue(p.matcher("Allows numbers and only + - / ( )").matches());
    }
}
