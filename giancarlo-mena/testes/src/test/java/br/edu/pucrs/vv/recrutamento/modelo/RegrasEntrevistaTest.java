package br.edu.pucrs.vv.recrutamento.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RegrasEntrevistaTest {

    private final Entrevista base = Entrevista.valida(List.of(7));

    private static List<Integer> entrevistadores(int quantidade) {
        return IntStream.rangeClosed(1, quantidade).boxed().toList();
    }

    @Test
    @DisplayName("Entrevista válida não tem erros")
    void entrevistaValida() {
        assertTrue(RegrasEntrevista.valida(base));
    }

    @DisplayName("U20 - valor-limite do número de entrevistadores")
    @ParameterizedTest(name = "{0} entrevistador(es) -> válido = {1}")
    @CsvSource({"0, false", "1, true", "5, true", "6, false"})
    void limiteEntrevistadores(int quantidade, boolean valido) {
        assertEquals(valido, RegrasEntrevista.valida(base.comEntrevistadores(entrevistadores(quantidade))));
    }

    @Test
    @DisplayName("U20 - lista de entrevistadores ausente é inválida")
    void semListaDeEntrevistadores() {
        assertEquals(Set.of("interviewerEmpNumbers"), RegrasEntrevista.camposInvalidos(base.comEntrevistadores(null)));
    }

    @DisplayName("U21 - valor-limite do título")
    @ParameterizedTest(name = "{0} caracteres -> válido = {1}")
    @CsvSource({"0, false", "1, true", "100, true", "101, false"})
    void limiteTitulo(int tamanho, boolean valido) {
        assertEquals(valido, RegrasEntrevista.valida(base.comTitulo("t".repeat(tamanho))));
    }

    @DisplayName("U22 - partição do horário: válidos")
    @ParameterizedTest
    @ValueSource(strings = {"00:00", "09:30", "23:59"})
    void horarioValido(String horario) {
        assertTrue(RegrasEntrevista.valida(base.comHorario(horario)));
    }

    @DisplayName("U22 - partição do horário: inválidos")
    @ParameterizedTest
    @ValueSource(strings = {"9h30", "24:00", "12:60", "meio-dia"})
    void horarioInvalido(String horario) {
        assertEquals(Set.of("interviewTime"), RegrasEntrevista.camposInvalidos(base.comHorario(horario)));
    }

    @Test
    @DisplayName("U22 - horário ausente é válido (campo opcional)")
    void horarioAusente() {
        assertTrue(RegrasEntrevista.valida(base.comHorario(null)));
    }

    @Test
    @DisplayName("U23 - data ausente é inválida")
    void dataAusente() {
        assertEquals(Set.of("interviewDate"), RegrasEntrevista.camposInvalidos(base.comData(null)));
    }

    @Test
    @DisplayName("Corpo da API usa a data ISO e os nomes de CandidateInterviewSchedulingAPI")
    void corpoDaApi() {
        var corpo = base.comData(java.time.LocalDate.of(2026, 12, 1)).comoCorpoApi();
        assertEquals("2026-12-01", corpo.get("interviewDate"));
        assertEquals(List.of(7), corpo.get("interviewerEmpNumbers"));
        assertTrue(!corpo.containsKey("note"));
    }
}
