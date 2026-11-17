package br.edu.pucrs.vv.recrutamento.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.pucrs.vv.recrutamento.cliente.Resposta;
import br.edu.pucrs.vv.recrutamento.modelo.Acao;
import br.edu.pucrs.vv.recrutamento.modelo.Candidato;
import br.edu.pucrs.vv.recrutamento.modelo.RegrasCandidato;
import br.edu.pucrs.vv.recrutamento.modelo.Status;

/**
 * Hipóteses de defeito tiradas da leitura do código (seção 4 do plano). Cada teste espera o comportamento
 * especificado; se falhar, a hipótese foi confirmada e o defeito vai para o relatório de execução.
 */
@Tag("integracao")
@Tag("hipotese")
@DisplayName("Integração - hipóteses de defeito")
class HipotesesDefeitoApiTest extends CenarioRecrutamento {

    /** Envia um candidato que o modelo considera inválido e espera 422. */
    private void esperaRecusa(Candidato c, String campo) {
        assertTrue(RegrasCandidato.camposInvalidos(c).contains(campo), "pré-condição: o modelo recusa " + campo);
        Resposta r = api.cadastrar(c);
        if (r.sucesso()) {
            registrarCandidato(r.id());
        }
        assertEquals(422, r.status(), "esperado 422 para " + campo + "; " + r.resumo());
    }

    @Test
    @DisplayName("H1 - e-mail vazio deve ser recusado (campo obrigatório)")
    void emailVazio() {
        esperaRecusa(novoCandidato().comEmail(""), "email");
    }

    @DisplayName("H2 - primeiro nome vazio ou só com espaços deve ser recusado")
    @ParameterizedTest(name = "primeiro nome = \"{0}\"")
    @ValueSource(strings = {"", "   "})
    void primeiroNomeEmBranco(String nome) {
        esperaRecusa(novoCandidato().comPrimeiroNome(nome), "firstName");
    }

    @Test
    @DisplayName("H3 - telefone com 31 dígitos deve ser recusado com 422, não com erro interno")
    void telefoneMaiorQueAColuna() {
        esperaRecusa(novoCandidato().comTelefone("9".repeat(31)), "contactNumber");
    }

    @Test
    @DisplayName("H4 - e-mail com 101 caracteres deve ser recusado com 422, não com erro interno")
    void emailMaiorQueAColuna() {
        String dominio = "@exemplo.com";
        String email = "a".repeat(101 - dominio.length()) + dominio;
        assertEquals(101, email.length());
        esperaRecusa(novoCandidato().comEmail(email), "email");
    }

    @Test
    @DisplayName("H5 - não deve ser possível aprovar a entrevista de outro candidato")
    void aprovarEntrevistaDeOutroCandidato() {
        CandidatoEmTeste a = candidatoNoStatus(Status.INTERVIEW_SCHEDULED);
        CandidatoEmTeste b = candidatoNoStatus(Status.INTERVIEW_SCHEDULED);
        assertFalse(a.ultimaEntrevista().equals(b.ultimaEntrevista()), "pré-condição: entrevistas diferentes");

        Resposta r = api.executar(Acao.APROVAR_ENTREVISTA, a.id(), b.ultimaEntrevista());

        assertTrue(r.status() >= 400 && r.status() < 500,
                "entrevista " + b.ultimaEntrevista() + " é do candidato " + b.id() + ", não de " + a.id()
                        + "; " + r.resumo());
        assertEquals(Status.INTERVIEW_SCHEDULED, api.status(a.id()), "status do candidato A não deve mudar");
    }
}
