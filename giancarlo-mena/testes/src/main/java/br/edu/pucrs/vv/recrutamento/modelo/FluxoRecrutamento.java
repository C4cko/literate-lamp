package br.edu.pucrs.vv.recrutamento.modelo;

import static br.edu.pucrs.vv.recrutamento.modelo.Acao.AGENDAR_ENTREVISTA;
import static br.edu.pucrs.vv.recrutamento.modelo.Acao.APROVAR_ENTREVISTA;
import static br.edu.pucrs.vv.recrutamento.modelo.Acao.CONTRATAR;
import static br.edu.pucrs.vv.recrutamento.modelo.Acao.OFERECER_VAGA;
import static br.edu.pucrs.vv.recrutamento.modelo.Acao.RECUSAR_OFERTA;
import static br.edu.pucrs.vv.recrutamento.modelo.Acao.REJEITAR;
import static br.edu.pucrs.vv.recrutamento.modelo.Acao.REPROVAR_ENTREVISTA;
import static br.edu.pucrs.vv.recrutamento.modelo.Acao.SHORTLIST;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Modelo da máquina de estados do recrutamento para o papel Admin.
 *
 * <p>Fonte: linhas 38 a 52 de {@code ohrm_workflow_state_machine} (workflow 2) em
 * {@code installer/Migration/V3_3_3/dbscript-2.sql} e a regra de no máximo duas entrevistas em
 * {@code CandidateInterviewSchedulingAPI::MAXIMUM_ALLOWED_INTERVIEWS_COUNT}.
 *
 * <p>É usado como oráculo pelos testes de integração e de sistema.
 */
public final class FluxoRecrutamento {

    public static final int MAXIMO_ENTREVISTAS = 2;

    private static final Map<Status, Map<Acao, Status>> TRANSICOES = new EnumMap<>(Status.class);

    static {
        transicao(Status.APPLICATION_INITIATED, SHORTLIST, Status.SHORTLISTED);
        transicao(Status.APPLICATION_INITIATED, REJEITAR, Status.REJECTED);
        transicao(Status.SHORTLISTED, AGENDAR_ENTREVISTA, Status.INTERVIEW_SCHEDULED);
        transicao(Status.SHORTLISTED, REJEITAR, Status.REJECTED);
        transicao(Status.INTERVIEW_SCHEDULED, REJEITAR, Status.REJECTED);
        transicao(Status.INTERVIEW_SCHEDULED, APROVAR_ENTREVISTA, Status.INTERVIEW_PASSED);
        transicao(Status.INTERVIEW_SCHEDULED, REPROVAR_ENTREVISTA, Status.INTERVIEW_FAILED);
        transicao(Status.INTERVIEW_PASSED, AGENDAR_ENTREVISTA, Status.INTERVIEW_SCHEDULED);
        transicao(Status.INTERVIEW_PASSED, OFERECER_VAGA, Status.JOB_OFFERED);
        transicao(Status.INTERVIEW_PASSED, REJEITAR, Status.REJECTED);
        transicao(Status.INTERVIEW_FAILED, REJEITAR, Status.REJECTED);
        transicao(Status.JOB_OFFERED, RECUSAR_OFERTA, Status.OFFER_DECLINED);
        transicao(Status.JOB_OFFERED, REJEITAR, Status.REJECTED);
        transicao(Status.JOB_OFFERED, CONTRATAR, Status.HIRED);
        transicao(Status.OFFER_DECLINED, REJEITAR, Status.REJECTED);
    }

    private FluxoRecrutamento() {
    }

    private static void transicao(Status origem, Acao acao, Status destino) {
        TRANSICOES.computeIfAbsent(origem, s -> new EnumMap<>(Acao.class)).put(acao, destino);
    }

    /**
     * Ações que o sistema deve oferecer para um candidato.
     *
     * @param estado               status atual
     * @param entrevistasAgendadas quantas entrevistas o candidato já teve nesta vaga
     */
    public static Set<Acao> acoesPermitidas(Status estado, int entrevistasAgendadas) {
        if (entrevistasAgendadas < 0) {
            throw new IllegalArgumentException("Número de entrevistas negativo: " + entrevistasAgendadas);
        }
        Map<Acao, Status> saidas = TRANSICOES.getOrDefault(estado, Map.of());
        Set<Acao> acoes = saidas.isEmpty() ? EnumSet.noneOf(Acao.class) : EnumSet.copyOf(saidas.keySet());
        if (entrevistasAgendadas >= MAXIMO_ENTREVISTAS) {
            acoes.remove(AGENDAR_ENTREVISTA);
        }
        return Collections.unmodifiableSet(acoes);
    }

    /** Estado resultante, ou vazio se a ação não é permitida. */
    public static Optional<Status> proximo(Status estado, Acao acao, int entrevistasAgendadas) {
        if (!acoesPermitidas(estado, entrevistasAgendadas).contains(acao)) {
            return Optional.empty();
        }
        return Optional.of(TRANSICOES.get(estado).get(acao));
    }

    /** Igual a {@link #proximo}, mas lança exceção quando a ação não é permitida. */
    public static Status aplicar(Status estado, Acao acao, int entrevistasAgendadas) {
        return proximo(estado, acao, entrevistasAgendadas)
                .orElseThrow(() -> new TransicaoInvalidaException(estado, acao));
    }

    public static boolean ehFinal(Status estado) {
        return acoesPermitidas(estado, 0).isEmpty();
    }
}
