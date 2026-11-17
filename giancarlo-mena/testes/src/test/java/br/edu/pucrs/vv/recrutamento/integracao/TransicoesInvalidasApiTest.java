package br.edu.pucrs.vv.recrutamento.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import br.edu.pucrs.vv.recrutamento.cliente.Resposta;
import br.edu.pucrs.vv.recrutamento.modelo.Acao;
import br.edu.pucrs.vv.recrutamento.modelo.FluxoRecrutamento;
import br.edu.pucrs.vv.recrutamento.modelo.Status;

/**
 * I07 - Técnica: transição de estados (pares inválidos). Os pares vêm do modelo: para cada status
 * escolhido, todas as ações que o modelo diz que são proibidas. Um candidato por status é preparado uma
 * única vez e reaproveitado; como cada tentativa deve ser recusada, o status não pode mudar.
 */
@Tag("integracao")
@DisplayName("Integração - ações proibidas pela máquina de estados")
class TransicoesInvalidasApiTest extends CenarioRecrutamento {

    private static final List<Status> ESTADOS = List.of(Status.APPLICATION_INITIATED, Status.SHORTLISTED,
            Status.INTERVIEW_FAILED, Status.HIRED, Status.REJECTED);

    private static final Map<Status, CandidatoEmTeste> preparados = new EnumMap<>(Status.class);

    static Stream<Arguments> paresProibidos() {
        List<Arguments> pares = new ArrayList<>();
        for (Status estado : ESTADOS) {
            boolean temEntrevista = CenarioRecrutamento.caminhoAte(estado).contains(Acao.AGENDAR_ENTREVISTA);
            for (Acao acao : Acao.values()) {
                if (acao.exigeEntrevista() && !temEntrevista) {
                    continue; // sem entrevista não há como montar o caminho .../interviews/{id}/...
                }
                if (FluxoRecrutamento.proximo(estado, acao, 1).isEmpty()) {
                    pares.add(Arguments.of(estado, acao));
                }
            }
        }
        return pares.stream();
    }

    @AfterAll
    static void esquecerPreparados() {
        preparados.clear();
    }

    private static CandidatoEmTeste candidato(Status estado) {
        return preparados.computeIfAbsent(estado, CenarioRecrutamento::candidatoNoStatus);
    }

    @DisplayName("I07 - ação proibida é recusada com 403 e o status não muda")
    @ParameterizedTest(name = "{0} + {1} -> 403")
    @MethodSource("paresProibidos")
    void acaoProibida(Status estado, Acao acao) {
        CandidatoEmTeste c = candidato(estado);

        Resposta r = acao == Acao.AGENDAR_ENTREVISTA
                ? api.agendar(c.id(), entrevistaPadrao())
                : api.executar(acao, c.id(), c.ultimaEntrevista());

        assertEquals(403, r.status(), estado + " + " + acao + ": " + r.resumo());
        assertEquals(estado, api.status(c.id()), "status depois da tentativa");
    }
}
