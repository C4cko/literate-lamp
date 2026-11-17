package br.edu.pucrs.vv.recrutamento.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import br.edu.pucrs.vv.recrutamento.cliente.OrangeHrmApi;
import br.edu.pucrs.vv.recrutamento.cliente.Resposta;
import br.edu.pucrs.vv.recrutamento.modelo.Acao;
import br.edu.pucrs.vv.recrutamento.modelo.Status;

@Tag("integracao")
@DisplayName("Integração - candidato em vaga fechada")
class VagaFechadaApiTest extends CenarioRecrutamento {

    @Test
    @DisplayName("I11 - com a vaga fechada nenhuma ação é oferecida e shortlist é recusado com 403")
    void vagaFechadaBloqueiaAcoes() {
        String nome = "Vaga fechada VV " + sufixo();
        long vagaFechada = api.criarVaga(nome, cargo, gerente());
        registrarVaga(vagaFechada);
        CandidatoEmTeste c = cadastrarNaVaga(vagaFechada);
        assertEquals(Status.APPLICATION_INITIATED, api.status(c.id()));

        OrangeHrmApi.exigir(api.alterarStatusVaga(vagaFechada, nome, cargo, gerente(), false));

        assertTrue(api.acoesPermitidas(c.id()).isEmpty(), "ações permitidas em vaga fechada");
        Resposta r = api.executar(Acao.SHORTLIST, c.id(), null);
        assertEquals(403, r.status(), r.resumo());
        assertEquals(Status.APPLICATION_INITIATED, api.status(c.id()));
    }
}
