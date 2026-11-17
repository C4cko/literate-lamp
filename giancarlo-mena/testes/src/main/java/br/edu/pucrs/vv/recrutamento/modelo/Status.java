package br.edu.pucrs.vv.recrutamento.modelo;

import java.util.Arrays;

/**
 * Status de um candidato em uma vaga (coluna {@code ohrm_job_candidate_vacancy.status}).
 * O id é o mesmo que a API devolve em {@code status.id}; o rótulo é o texto mostrado na tela.
 */
public enum Status {
    APPLICATION_INITIATED(1, "Application Initiated"),
    SHORTLISTED(2, "Shortlisted"),
    REJECTED(3, "Rejected"),
    INTERVIEW_SCHEDULED(4, "Interview Scheduled"),
    INTERVIEW_PASSED(5, "Interview Passed"),
    INTERVIEW_FAILED(6, "Interview Failed"),
    JOB_OFFERED(7, "Job Offered"),
    OFFER_DECLINED(8, "Offer Declined"),
    HIRED(9, "Hired");

    private final int id;
    private final String rotulo;

    Status(int id, String rotulo) {
        this.id = id;
        this.rotulo = rotulo;
    }

    public int id() {
        return id;
    }

    public String rotulo() {
        return rotulo;
    }

    public static Status porId(int id) {
        return Arrays.stream(values())
                .filter(s -> s.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Status desconhecido: " + id));
    }
}
