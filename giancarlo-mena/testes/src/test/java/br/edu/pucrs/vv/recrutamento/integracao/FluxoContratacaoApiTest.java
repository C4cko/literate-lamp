package br.edu.pucrs.vv.recrutamento.integracao;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import br.edu.pucrs.vv.recrutamento.cliente.Resposta;
import br.edu.pucrs.vv.recrutamento.modelo.Acao;
import br.edu.pucrs.vv.recrutamento.modelo.FluxoRecrutamento;
import br.edu.pucrs.vv.recrutamento.modelo.Status;

/**
 * Caminho principal da jornada pela API. O {@code @BeforeAll} executa a jornada uma vez e guarda o que foi
 * observado em cada passo; cada teste confere um aspecto diferente desse resultado.
 */
@Tag("integracao")
@DisplayName("Integração - jornada de contratação pela API")
class FluxoContratacaoApiTest extends CenarioRecrutamento {

    /** O que o teste viu depois de cada ação. */
    record Passo(Acao acao, int http, Status esperado, Status observado, Set<Acao> acoesEsperadas,
                 Set<Acao> acoesObservadas) {
    }

    private static final List<Acao> JORNADA = List.of(Acao.SHORTLIST, Acao.AGENDAR_ENTREVISTA,
            Acao.APROVAR_ENTREVISTA, Acao.OFERECER_VAGA, Acao.CONTRATAR);

    private static final List<Passo> passos = new ArrayList<>();
    private static CandidatoEmTeste contratado;

    @BeforeAll
    static void executarJornada() {
        passos.clear();
        CandidatoEmTeste c = cadastrarNaVaga();
        Status estado = Status.APPLICATION_INITIATED;
        int entrevistas = 0;
        Long entrevista = null;

        for (Acao acao : JORNADA) {
            Status esperado = FluxoRecrutamento.aplicar(estado, acao, entrevistas);
            Resposta r;
            if (acao == Acao.AGENDAR_ENTREVISTA) {
                r = api.agendar(c.id(), entrevistaPadrao());
                if (r.sucesso()) {
                    entrevista = r.id();
                    entrevistas++;
                }
            } else {
                r = api.executar(acao, c.id(), entrevista);
            }
            Status observado = api.status(c.id());
            passos.add(new Passo(acao, r.status(), esperado, observado,
                    FluxoRecrutamento.acoesPermitidas(esperado, entrevistas), api.acoesPermitidas(c.id())));
            estado = esperado;
        }
        if (api.status(c.id()) == Status.HIRED) {
            registrarContratacao(c.sobrenome());
        }
        contratado = new CandidatoEmTeste(c.id(), c.sobrenome(), entrevista, entrevistas);
    }

    @Test
    @DisplayName("I04 - cada passo responde 200 e deixa status e ações permitidas iguais ao modelo")
    void cadaPassoSegueOModelo() {
        assertEquals(JORNADA.size(), passos.size());
        List<org.junit.jupiter.api.function.Executable> verificacoes = new ArrayList<>();
        for (Passo p : passos) {
            verificacoes.add(() -> assertEquals(200, p.http(), p.acao() + ": código HTTP"));
            verificacoes.add(() -> assertEquals(p.esperado(), p.observado(), p.acao() + ": status"));
            verificacoes.add(() -> assertEquals(p.acoesEsperadas(), p.acoesObservadas(),
                    p.acao() + ": ações permitidas"));
        }
        assertAll(verificacoes.toArray(org.junit.jupiter.api.function.Executable[]::new));
    }

    @Test
    @DisplayName("I05 - contratação cria funcionário no PIM com o nome do candidato")
    void contratacaoCriaFuncionario() {
        JsonNode encontrados = api.buscarFuncionarios(contratado.sobrenome());
        assertEquals(1, encontrados.size(), "funcionários com sobrenome " + contratado.sobrenome());
        JsonNode f = encontrados.get(0);
        assertAll(
                () -> assertEquals("Ana", f.get("firstName").asText()),
                () -> assertEquals(contratado.sobrenome(), f.get("lastName").asText()));
    }

    @Test
    @DisplayName("I06 - histórico registra todas as ações da jornada")
    void historicoRegistraAcoes() {
        JsonNode historico = api.historico(contratado.id());
        List<Integer> acoes = new ArrayList<>();
        historico.forEach(h -> acoes.add(h.get("action").get("id").asInt()));
        for (Acao a : JORNADA) {
            assertTrue(acoes.contains(a.id()), "histórico deveria ter a ação " + a + "; tem " + acoes);
        }
    }

    @Test
    @DisplayName("I12 - caminho alternativo: oferta recusada e depois rejeição")
    void ofertaRecusadaDepoisRejeitada() {
        CandidatoEmTeste c = candidatoNoStatus(Status.JOB_OFFERED);

        Resposta recusa = api.executar(Acao.RECUSAR_OFERTA, c.id(), null);
        assertEquals(200, recusa.status(), recusa.resumo());
        assertEquals(Status.OFFER_DECLINED, api.status(c.id()));
        assertEquals(Set.of(Acao.REJEITAR), api.acoesPermitidas(c.id()));

        Resposta rejeicao = api.executar(Acao.REJEITAR, c.id(), null);
        assertEquals(200, rejeicao.status(), rejeicao.resumo());
        assertEquals(Status.REJECTED, api.status(c.id()));
        assertTrue(api.acoesPermitidas(c.id()).isEmpty());
    }
}
