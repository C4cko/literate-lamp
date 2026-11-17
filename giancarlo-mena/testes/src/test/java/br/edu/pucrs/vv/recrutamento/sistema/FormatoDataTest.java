package br.edu.pucrs.vv.recrutamento.sistema;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** U33 - teste unitário (sem tag) do apoio usado pelos testes de sistema para preencher datas. */
class FormatoDataTest {

    @DisplayName("U33 - data é formatada conforme o placeholder do campo")
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "yyyy-dd-mm, 2026-05-12",
        "yyyy-mm-dd, 2026-12-05",
        "dd-mm-yyyy, 05-12-2026",
        "mm/dd/yyyy, 12/05/2026",
        "'',         2026-12-05",
    })
    void formata(String placeholder, String esperado) {
        assertEquals(esperado, TelaAcao.formatarData(LocalDate.of(2026, 12, 5), placeholder));
    }
}
