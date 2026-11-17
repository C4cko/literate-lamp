package br.edu.pucrs.vv.recrutamento.integracao;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.pucrs.vv.recrutamento.cliente.Resposta;
import br.edu.pucrs.vv.recrutamento.modelo.Acao;
import br.edu.pucrs.vv.recrutamento.modelo.Entrevista;
import br.edu.pucrs.vv.recrutamento.modelo.RegrasEntrevista;
import br.edu.pucrs.vv.recrutamento.modelo.Status;

@Tag("integracao")
@DisplayName("Integração - agendamento de entrevistas")
class EntrevistasApiTest extends CenarioRecrutamento {

    @Test
    @DisplayName("I08 - 1ª e 2ª entrevista são aceitas, a 3ª é recusada com 400")
    void limiteDeDuasEntrevistas() {
        CandidatoEmTeste c = candidatoNoStatus(Status.SHORTLISTED);

        Resposta primeira = api.agendar(c.id(), entrevistaPadrao());
        assertEquals(200, primeira.status(), "1ª entrevista: " + primeira.resumo());
        assertEquals(200, api.executar(Acao.APROVAR_ENTREVISTA, c.id(), primeira.id()).status());
        assertTrue(api.acoesPermitidas(c.id()).contains(Acao.AGENDAR_ENTREVISTA),
                "com 1 entrevista ainda deve ser possível agendar outra");

        Resposta segunda = api.agendar(c.id(), entrevistaPadrao().comTitulo("Segunda entrevista"));
        assertEquals(200, segunda.status(), "2ª entrevista: " + segunda.resumo());
        assertEquals(200, api.executar(Acao.APROVAR_ENTREVISTA, c.id(), segunda.id()).status());

        assertAll(
                () -> assertEquals(Status.INTERVIEW_PASSED, api.status(c.id())),
                () -> assertFalse(api.acoesPermitidas(c.id()).contains(Acao.AGENDAR_ENTREVISTA),
                        "com 2 entrevistas, 'Schedule Interview' não deve aparecer"));

        Resposta terceira = api.agendar(c.id(), entrevistaPadrao().comTitulo("Terceira entrevista"));
        assertEquals(400, terceira.status(), "3ª entrevista: " + terceira.resumo());
        assertEquals(Status.INTERVIEW_PASSED, api.status(c.id()), "status não muda após a recusa");
    }

    @DisplayName("I09 - 0 ou 6 entrevistadores são recusados com 422")
    @ParameterizedTest(name = "{0} entrevistadores")
    @ValueSource(ints = {0, 6})
    void quantidadeInvalidaDeEntrevistadores(int quantidade) {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            ids.add(funcionarios.get(i % funcionarios.size()));
        }
        Entrevista e = entrevistaPadrao().comEntrevistadores(ids);
        assertFalse(RegrasEntrevista.valida(e), "pré-condição: o modelo considera inválida");

        CandidatoEmTeste c = candidatoNoStatus(Status.SHORTLISTED);
        Resposta r = api.agendar(c.id(), e);
        assertEquals(422, r.status(), r.resumo());
        assertEquals(Status.SHORTLISTED, api.status(c.id()));
    }

    @Test
    @DisplayName("I10 - 5 entrevistadores diferentes são aceitos")
    void cincoEntrevistadores() {
        Assumptions.assumeTrue(funcionarios.size() >= 5, "o servidor precisa ter 5 funcionários ativos");
        Entrevista e = entrevistaPadrao().comEntrevistadores(funcionarios.subList(0, 5));
        assertTrue(RegrasEntrevista.valida(e));

        CandidatoEmTeste c = candidatoNoStatus(Status.SHORTLISTED);
        Resposta r = api.agendar(c.id(), e);
        assertEquals(200, r.status(), r.resumo());
        assertEquals(Status.INTERVIEW_SCHEDULED, api.status(c.id()));
    }
}
