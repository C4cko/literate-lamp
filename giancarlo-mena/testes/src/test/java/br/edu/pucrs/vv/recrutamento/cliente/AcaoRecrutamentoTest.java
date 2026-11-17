package br.edu.pucrs.vv.recrutamento.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import br.edu.pucrs.vv.recrutamento.modelo.Acao;

/** U32 - caminhos conferidos com src/plugins/orangehrmRecruitmentPlugin/config/routes.yaml. */
class AcaoRecrutamentoTest {

    @DisplayName("U32 - cada ação monta o caminho e o método da rota da API")
    @ParameterizedTest(name = "{0} -> {1} {2}")
    @CsvSource({
        "SHORTLIST,           PUT,  /recruitment/candidates/7/shortlist",
        "REJEITAR,            PUT,  /recruitment/candidates/7/reject",
        "AGENDAR_ENTREVISTA,  POST, /recruitment/candidates/7/shedule-interview",
        "APROVAR_ENTREVISTA,  PUT,  /recruitment/candidates/7/interviews/3/pass",
        "REPROVAR_ENTREVISTA, PUT,  /recruitment/candidates/7/interviews/3/fail",
        "OFERECER_VAGA,       PUT,  /recruitment/candidates/7/job/offer",
        "RECUSAR_OFERTA,      PUT,  /recruitment/candidates/7/job/decline",
        "CONTRATAR,           PUT,  /recruitment/candidates/7/hire",
    })
    void caminhoDaAcao(Acao acao, String metodo, String caminho) {
        assertEquals(metodo, acao.metodo());
        assertEquals(caminho, acao.caminho(7, 3L));
    }

    @Test
    @DisplayName("U32 - aprovar entrevista sem id de entrevista é erro de uso")
    void aprovarSemEntrevista() {
        assertThrows(IllegalArgumentException.class, () -> Acao.APROVAR_ENTREVISTA.caminho(7, null));
    }

    @Test
    @DisplayName("Ambiente remove a barra final da URL configurada")
    void urlSemBarraFinal() {
        System.setProperty("orangehrm.url", "http://localhost:8080/web/index.php/");
        try {
            assertEquals("http://localhost:8080/web/index.php", Ambiente.url());
        } finally {
            System.clearProperty("orangehrm.url");
        }
    }
}
