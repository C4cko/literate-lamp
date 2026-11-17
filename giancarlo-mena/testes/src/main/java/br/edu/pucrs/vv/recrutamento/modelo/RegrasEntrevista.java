package br.edu.pucrs.vv.recrutamento.modelo;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Regras de {@code CandidateInterviewSchedulingAPI::getCommonBodyValidationRules}: título obrigatório até
 * 100 caracteres, data obrigatória, horário opcional no formato H:i (00:00 a 23:59), 1 a 5 entrevistadores,
 * nota até 2000 caracteres.
 */
public final class RegrasEntrevista {

    public static final int MAX_TITULO = 100;
    public static final int MIN_ENTREVISTADORES = 1;
    public static final int MAX_ENTREVISTADORES = 5;
    public static final int MAX_NOTA = 2000;

    private static final Pattern HORARIO = Pattern.compile("^([01][0-9]|2[0-3]):[0-5][0-9]$");

    private RegrasEntrevista() {
    }

    public static Set<String> camposInvalidos(Entrevista e) {
        Set<String> erros = new TreeSet<>();
        if (e.titulo() == null || e.titulo().isBlank() || e.titulo().length() > MAX_TITULO) {
            erros.add("interviewName");
        }
        if (e.data() == null) {
            erros.add("interviewDate");
        }
        if (e.horario() != null && !HORARIO.matcher(e.horario()).matches()) {
            erros.add("interviewTime");
        }
        int n = e.entrevistadores() == null ? 0 : e.entrevistadores().size();
        if (n < MIN_ENTREVISTADORES || n > MAX_ENTREVISTADORES) {
            erros.add("interviewerEmpNumbers");
        }
        if (e.nota() != null && e.nota().length() > MAX_NOTA) {
            erros.add("note");
        }
        return erros;
    }

    public static boolean valida(Entrevista e) {
        return camposInvalidos(e).isEmpty();
    }
}
