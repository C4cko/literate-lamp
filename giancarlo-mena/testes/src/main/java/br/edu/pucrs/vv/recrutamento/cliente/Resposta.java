package br.edu.pucrs.vv.recrutamento.cliente;

import com.fasterxml.jackson.databind.JsonNode;

/** Resposta de uma chamada à API: código HTTP e corpo JSON (ou {@code null} se o corpo não for JSON). */
public record Resposta(int status, JsonNode corpo, String texto) {

    public JsonNode data() {
        if (corpo == null || !corpo.has("data")) {
            throw new IllegalStateException("Resposta sem campo data (HTTP " + status + "): " + resumo());
        }
        return corpo.get("data");
    }

    public long id() {
        return data().get("id").asLong();
    }

    public boolean sucesso() {
        return status >= 200 && status < 300;
    }

    /** Texto curto para mensagens de falha dos testes. */
    public String resumo() {
        String t = texto == null ? "" : texto;
        return "HTTP " + status + " " + (t.length() > 300 ? t.substring(0, 300) + "..." : t);
    }
}
