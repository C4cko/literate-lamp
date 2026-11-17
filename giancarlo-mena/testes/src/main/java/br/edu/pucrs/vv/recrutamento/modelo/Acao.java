package br.edu.pucrs.vv.recrutamento.modelo;

import java.util.Arrays;

/**
 * Ações do processo seletivo. O id é o mesmo da coluna {@code action} de
 * {@code ohrm_workflow_state_machine}; o rótulo é o texto do botão na tela e o caminho é o endpoint da API v2.
 */
public enum Acao {
    SHORTLIST(2, "Shortlist", "PUT", "/recruitment/candidates/%d/shortlist"),
    REJEITAR(3, "Reject", "PUT", "/recruitment/candidates/%d/reject"),
    AGENDAR_ENTREVISTA(4, "Schedule Interview", "POST", "/recruitment/candidates/%d/shedule-interview"),
    APROVAR_ENTREVISTA(5, "Mark Interview Passed", "PUT", "/recruitment/candidates/%d/interviews/%d/pass"),
    REPROVAR_ENTREVISTA(6, "Mark Interview Failed", "PUT", "/recruitment/candidates/%d/interviews/%d/fail"),
    OFERECER_VAGA(7, "Offer Job", "PUT", "/recruitment/candidates/%d/job/offer"),
    RECUSAR_OFERTA(8, "Decline Offer", "PUT", "/recruitment/candidates/%d/job/decline"),
    CONTRATAR(9, "Hire", "PUT", "/recruitment/candidates/%d/hire");

    private final int id;
    private final String rotulo;
    private final String metodo;
    private final String modeloCaminho;

    Acao(int id, String rotulo, String metodo, String modeloCaminho) {
        this.id = id;
        this.rotulo = rotulo;
        this.metodo = metodo;
        this.modeloCaminho = modeloCaminho;
    }

    public int id() {
        return id;
    }

    public String rotulo() {
        return rotulo;
    }

    public String metodo() {
        return metodo;
    }

    /** Ações sobre uma entrevista específica precisam do id da entrevista no caminho. */
    public boolean exigeEntrevista() {
        return this == APROVAR_ENTREVISTA || this == REPROVAR_ENTREVISTA;
    }

    public String caminho(long candidatoId, Long entrevistaId) {
        if (exigeEntrevista()) {
            if (entrevistaId == null) {
                throw new IllegalArgumentException(name() + " precisa do id da entrevista");
            }
            return modeloCaminho.formatted(candidatoId, entrevistaId);
        }
        return modeloCaminho.formatted(candidatoId);
    }

    public static Acao porId(int id) {
        return Arrays.stream(values())
                .filter(a -> a.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ação desconhecida: " + id));
    }
}
