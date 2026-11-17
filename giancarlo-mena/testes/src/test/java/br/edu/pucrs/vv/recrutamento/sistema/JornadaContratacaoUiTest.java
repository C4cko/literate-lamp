package br.edu.pucrs.vv.recrutamento.sistema;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import br.edu.pucrs.vv.recrutamento.modelo.Acao;
import br.edu.pucrs.vv.recrutamento.modelo.Candidato;
import br.edu.pucrs.vv.recrutamento.modelo.FluxoRecrutamento;
import br.edu.pucrs.vv.recrutamento.modelo.Status;

@Tag("sistema")
@DisplayName("Sistema - jornada de contratação pela interface")
class JornadaContratacaoUiTest extends SistemaBase {

    @Test
    @DisplayName("S01 - Ana cadastra o candidato e o leva até a contratação")
    void jornadaCompleta() {
        Candidato c = Candidato.valido(sufixo());
        TelaCandidato tela = telaCandidato().abrirNovo().preencher(c, nomeVaga);
        long id = tela.salvarEObterId();
        registrarCandidato(id);

        Status esperado = Status.APPLICATION_INITIATED;
        assertThat(tela.status()).containsText("Status: " + esperado.rotulo());

        int entrevistas = 0;
        Acao[] jornada = {Acao.SHORTLIST, Acao.AGENDAR_ENTREVISTA, Acao.APROVAR_ENTREVISTA,
                          Acao.OFERECER_VAGA, Acao.CONTRATAR};
        for (Acao acao : jornada) {
            esperado = FluxoRecrutamento.aplicar(esperado, acao, entrevistas);
            tela.clicarAcao(acao);
            if (acao == Acao.AGENDAR_ENTREVISTA) {
                telaAcao().agendarEntrevista("Entrevista técnica", entrevistador, LocalDate.now().plusDays(7));
                entrevistas++;
            } else {
                telaAcao().confirmar("Etapa " + acao.rotulo() + " registrada pelos testes de sistema");
            }
            if (acao == Acao.CONTRATAR) {
                registrarContratacao(c.sobrenome());
            }
            assertThat(tela.status()).containsText("Status: " + esperado.rotulo());
        }

        assertEquals(Status.HIRED, esperado);
        assertThat(tela.botoesDeAcao()).hasCount(0);
        assertEquals(1, api.buscarFuncionarios(c.sobrenome()).size(),
                "o candidato contratado deve aparecer no PIM");
    }

    @Test
    @DisplayName("S06 - rejeitar a partir de Application Initiated encerra o processo")
    void rejeitarEncerraProcesso() {
        Candidato c = Candidato.valido(sufixo());
        TelaCandidato tela = telaCandidato().abrirNovo().preencher(c, nomeVaga);
        registrarCandidato(tela.salvarEObterId());

        tela.clicarAcao(Acao.REJEITAR);
        telaAcao().confirmar("Perfil não atende à vaga");

        assertThat(tela.status()).containsText("Status: " + Status.REJECTED.rotulo());
        assertThat(tela.botoesDeAcao()).hasCount(0);
    }
}
