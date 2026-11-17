package br.edu.pucrs.vv.recrutamento.modelo;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dados do formulário "Schedule Interview" ({@code CandidateInterviewSchedulingAPI}). */
public record Entrevista(String titulo, LocalDate data, String horario, List<Integer> entrevistadores, String nota) {

    public Entrevista {
        entrevistadores = entrevistadores == null ? null : List.copyOf(entrevistadores);
    }

    public static Entrevista valida(List<Integer> entrevistadores) {
        return new Entrevista("Entrevista técnica", LocalDate.now().plusDays(7), "10:00", entrevistadores, null);
    }

    public Entrevista comTitulo(String valor) {
        return new Entrevista(valor, data, horario, entrevistadores, nota);
    }

    public Entrevista comData(LocalDate valor) {
        return new Entrevista(titulo, valor, horario, entrevistadores, nota);
    }

    public Entrevista comHorario(String valor) {
        return new Entrevista(titulo, data, valor, entrevistadores, nota);
    }

    public Entrevista comEntrevistadores(List<Integer> valor) {
        return new Entrevista(titulo, data, horario, valor, nota);
    }

    public Map<String, Object> comoCorpoApi() {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("interviewName", titulo);
        corpo.put("interviewDate", data == null ? null : data.toString());
        if (horario != null) {
            corpo.put("interviewTime", horario);
        }
        corpo.put("interviewerEmpNumbers", entrevistadores);
        if (nota != null) {
            corpo.put("note", nota);
        }
        return corpo;
    }
}
