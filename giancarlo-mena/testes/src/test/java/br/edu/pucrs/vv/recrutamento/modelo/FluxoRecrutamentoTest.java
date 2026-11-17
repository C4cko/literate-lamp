package br.edu.pucrs.vv.recrutamento.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Técnica: transição de estados. A tabela abaixo foi copiada à mão das linhas 38 a 52 do
 * dbscript-2.sql (papel ADMIN) e é independente da implementação de {@link FluxoRecrutamento}.
 */
class FluxoRecrutamentoTest {

    private static final String[] TRANSICOES_VALIDAS = {
        "APPLICATION_INITIATED, SHORTLIST,           SHORTLISTED",
        "APPLICATION_INITIATED, REJEITAR,            REJECTED",
        "SHORTLISTED,           AGENDAR_ENTREVISTA,  INTERVIEW_SCHEDULED",
        "SHORTLISTED,           REJEITAR,            REJECTED",
        "INTERVIEW_SCHEDULED,   REJEITAR,            REJECTED",
        "INTERVIEW_SCHEDULED,   APROVAR_ENTREVISTA,  INTERVIEW_PASSED",
        "INTERVIEW_SCHEDULED,   REPROVAR_ENTREVISTA, INTERVIEW_FAILED",
        "INTERVIEW_PASSED,      AGENDAR_ENTREVISTA,  INTERVIEW_SCHEDULED",
        "INTERVIEW_PASSED,      OFERECER_VAGA,       JOB_OFFERED",
        "INTERVIEW_PASSED,      REJEITAR,            REJECTED",
        "INTERVIEW_FAILED,      REJEITAR,            REJECTED",
        "JOB_OFFERED,           RECUSAR_OFERTA,      OFFER_DECLINED",
        "JOB_OFFERED,           REJEITAR,            REJECTED",
        "JOB_OFFERED,           CONTRATAR,           HIRED",
        "OFFER_DECLINED,        REJEITAR,            REJECTED",
    };

    @DisplayName("U01 - transições válidas levam ao estado da tabela")
    @ParameterizedTest(name = "{0} --{1}--> {2}")
    @MethodSource("transicoesValidas")
    void transicaoValida(Status origem, Acao acao, Status destino) {
        assertEquals(Optional.of(destino), FluxoRecrutamento.proximo(origem, acao, 1));
    }

    @DisplayName("U02 - pares (estado, ação) fora da tabela não são permitidos")
    @ParameterizedTest(name = "{0} --{1}--> proibido")
    @MethodSource("transicoesInvalidas")
    void transicaoInvalida(Status origem, Acao acao) {
        assertEquals(Optional.empty(), FluxoRecrutamento.proximo(origem, acao, 1));
    }

    @Test
    @DisplayName("U02 - a tabela tem 15 transições válidas e 57 inválidas")
    void tamanhoDaTabela() {
        assertEquals(15, transicoesValidas().count());
        assertEquals(57, transicoesInvalidas().count());
    }

    @DisplayName("U03 - estados finais não têm ações")
    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"HIRED", "REJECTED"})
    void estadosFinais(Status estado) {
        assertTrue(FluxoRecrutamento.ehFinal(estado));
        assertTrue(FluxoRecrutamento.acoesPermitidas(estado, 0).isEmpty());
    }

    @DisplayName("U03 - estados intermediários não são finais")
    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"HIRED", "REJECTED"}, mode = EnumSource.Mode.EXCLUDE)
    void estadosNaoFinais(Status estado) {
        assertFalse(FluxoRecrutamento.ehFinal(estado));
    }

    @DisplayName("U04 - Shortlisted: agendar entrevista depende do número de entrevistas")
    @ParameterizedTest(name = "{0} entrevista(s) -> agendar permitido = {1}")
    @CsvSource({"0, true", "1, true", "2, false", "3, false"})
    void limiteDeEntrevistasEmShortlisted(int entrevistas, boolean permitido) {
        Set<Acao> acoes = FluxoRecrutamento.acoesPermitidas(Status.SHORTLISTED, entrevistas);
        assertEquals(permitido, acoes.contains(Acao.AGENDAR_ENTREVISTA));
        assertTrue(acoes.contains(Acao.REJEITAR), "rejeitar continua permitido");
    }

    @DisplayName("U05 - Interview Passed: segunda entrevista só se houver apenas uma")
    @ParameterizedTest(name = "{0} entrevista(s) -> agendar permitido = {1}")
    @CsvSource({"1, true", "2, false"})
    void limiteDeEntrevistasEmInterviewPassed(int entrevistas, boolean permitido) {
        Set<Acao> acoes = FluxoRecrutamento.acoesPermitidas(Status.INTERVIEW_PASSED, entrevistas);
        assertEquals(permitido, acoes.contains(Acao.AGENDAR_ENTREVISTA));
        assertTrue(acoes.contains(Acao.OFERECER_VAGA));
    }

    @Test
    @DisplayName("U04 - número de entrevistas negativo é rejeitado")
    void entrevistasNegativas() {
        assertThrows(IllegalArgumentException.class,
                () -> FluxoRecrutamento.acoesPermitidas(Status.SHORTLISTED, -1));
    }

    @Test
    @DisplayName("U06 - caminho principal termina em Hired")
    void caminhoPrincipal() {
        Status s = Status.APPLICATION_INITIATED;
        s = FluxoRecrutamento.aplicar(s, Acao.SHORTLIST, 0);
        s = FluxoRecrutamento.aplicar(s, Acao.AGENDAR_ENTREVISTA, 0);
        s = FluxoRecrutamento.aplicar(s, Acao.APROVAR_ENTREVISTA, 1);
        s = FluxoRecrutamento.aplicar(s, Acao.OFERECER_VAGA, 1);
        s = FluxoRecrutamento.aplicar(s, Acao.CONTRATAR, 1);
        assertEquals(Status.HIRED, s);
    }

    @Test
    @DisplayName("U07 - ação proibida lança exceção com estado e ação na mensagem")
    void acaoProibidaLancaExcecao() {
        TransicaoInvalidaException e = assertThrows(TransicaoInvalidaException.class,
                () -> FluxoRecrutamento.aplicar(Status.APPLICATION_INITIATED, Acao.CONTRATAR, 0));
        assertTrue(e.getMessage().contains("CONTRATAR"));
        assertTrue(e.getMessage().contains("APPLICATION_INITIATED"));
    }

    @DisplayName("Status e ações são encontrados pelo id usado na API")
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9})
    void statusPorId(int id) {
        assertEquals(id, Status.porId(id).id());
    }

    @Test
    @DisplayName("Id de status inexistente é rejeitado")
    void statusIdInexistente() {
        assertThrows(IllegalArgumentException.class, () -> Status.porId(10));
        assertThrows(IllegalArgumentException.class, () -> Acao.porId(1));
    }

    static Stream<Arguments> transicoesValidas() {
        return Stream.of(TRANSICOES_VALIDAS).map(linha -> {
            String[] p = linha.split(",");
            return Arguments.of(Status.valueOf(p[0].trim()), Acao.valueOf(p[1].trim()), Status.valueOf(p[2].trim()));
        });
    }

    static Stream<Arguments> transicoesInvalidas() {
        List<String> validas = new ArrayList<>();
        transicoesValidas().forEach(a -> validas.add(a.get()[0] + ">" + a.get()[1]));
        List<Arguments> invalidas = new ArrayList<>();
        for (Status s : Status.values()) {
            for (Acao a : Acao.values()) {
                if (!validas.contains(s + ">" + a)) {
                    invalidas.add(Arguments.of(s, a));
                }
            }
        }
        return invalidas.stream();
    }
}
