package br.edu.pucrs.vv.recrutamento.modelo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dados do formulário "Add Candidate". Os nomes dos campos em {@link #comoCorpoApi()} são os de
 * {@code CandidateAPI} ({@code firstName}, {@code lastName}...). Campos nulos não são enviados.
 */
public record Candidato(
        String primeiroNome,
        String nomeDoMeio,
        String sobrenome,
        String email,
        String telefone,
        String palavrasChave,
        String comentario,
        Long vagaId) {

    /** Candidato válido, com nome único para não colidir com dados de outras pessoas no servidor. */
    public static Candidato valido(String sufixo) {
        return new Candidato("Ana", null, "Teste" + sufixo, "ana." + sufixo.toLowerCase() + "@exemplo.com",
                "51 3320-3500", "java,testes", "cadastrada pelos testes automatizados", null);
    }

    public Candidato comPrimeiroNome(String valor) {
        return new Candidato(valor, nomeDoMeio, sobrenome, email, telefone, palavrasChave, comentario, vagaId);
    }

    public Candidato comNomeDoMeio(String valor) {
        return new Candidato(primeiroNome, valor, sobrenome, email, telefone, palavrasChave, comentario, vagaId);
    }

    public Candidato comSobrenome(String valor) {
        return new Candidato(primeiroNome, nomeDoMeio, valor, email, telefone, palavrasChave, comentario, vagaId);
    }

    public Candidato comEmail(String valor) {
        return new Candidato(primeiroNome, nomeDoMeio, sobrenome, valor, telefone, palavrasChave, comentario, vagaId);
    }

    public Candidato comTelefone(String valor) {
        return new Candidato(primeiroNome, nomeDoMeio, sobrenome, email, valor, palavrasChave, comentario, vagaId);
    }

    public Candidato comPalavrasChave(String valor) {
        return new Candidato(primeiroNome, nomeDoMeio, sobrenome, email, telefone, valor, comentario, vagaId);
    }

    public Candidato comComentario(String valor) {
        return new Candidato(primeiroNome, nomeDoMeio, sobrenome, email, telefone, palavrasChave, valor, vagaId);
    }

    public Candidato naVaga(Long valor) {
        return new Candidato(primeiroNome, nomeDoMeio, sobrenome, email, telefone, palavrasChave, comentario, valor);
    }

    public Map<String, Object> comoCorpoApi() {
        Map<String, Object> corpo = new LinkedHashMap<>();
        coloca(corpo, "firstName", primeiroNome);
        coloca(corpo, "middleName", nomeDoMeio);
        coloca(corpo, "lastName", sobrenome);
        coloca(corpo, "email", email);
        coloca(corpo, "contactNumber", telefone);
        coloca(corpo, "keywords", palavrasChave);
        coloca(corpo, "comment", comentario);
        coloca(corpo, "vacancyId", vagaId);
        corpo.put("consentToKeepData", false);
        return corpo;
    }

    private static void coloca(Map<String, Object> corpo, String chave, Object valor) {
        if (valor != null) {
            corpo.put(chave, valor);
        }
    }
}
