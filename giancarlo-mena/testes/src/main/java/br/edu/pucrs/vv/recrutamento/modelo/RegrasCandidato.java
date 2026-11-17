package br.edu.pucrs.vv.recrutamento.modelo;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Regras do formulário de candidato segundo a especificação: campos obrigatórios do contrato OpenAPI de
 * {@code CandidateAPI} e limites da tela {@code SaveCandidate.vue}.
 *
 * <table>
 *   <caption>Regras</caption>
 *   <tr><th>Campo</th><th>Obrigatório</th><th>Limite</th><th>Formato</th></tr>
 *   <tr><td>firstName, lastName</td><td>sim</td><td>30</td><td>-</td></tr>
 *   <tr><td>middleName</td><td>não</td><td>30</td><td>-</td></tr>
 *   <tr><td>email</td><td>sim</td><td>50</td><td>{@link #EMAIL}</td></tr>
 *   <tr><td>contactNumber</td><td>não</td><td>25</td><td>{@link #TELEFONE}</td></tr>
 *   <tr><td>keywords, comment</td><td>não</td><td>250</td><td>-</td></tr>
 * </table>
 */
public final class RegrasCandidato {

    public static final int MAX_NOME = 30;
    public static final int MAX_EMAIL = 50;
    public static final int MAX_TELEFONE = 25;
    public static final int MAX_TEXTO = 250;

    /** Mesma expressão de {@code Rules/Email.php} e de {@code validEmailFormat} no front-end. */
    public static final Pattern EMAIL = Pattern.compile(
            "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?"
                    + "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$");

    /** Mesma expressão de {@code Rules/Phone.php}. */
    public static final Pattern TELEFONE = Pattern.compile("^[0-9+\\-/() ]+$");

    private RegrasCandidato() {
    }

    /** Devolve os nomes (da API) dos campos inválidos; conjunto vazio quando o candidato é válido. */
    public static Set<String> camposInvalidos(Candidato c) {
        Set<String> erros = new TreeSet<>();
        obrigatorio(erros, "firstName", c.primeiroNome(), MAX_NOME);
        opcional(erros, "middleName", c.nomeDoMeio(), MAX_NOME);
        obrigatorio(erros, "lastName", c.sobrenome(), MAX_NOME);
        obrigatorio(erros, "email", c.email(), MAX_EMAIL);
        if (preenchido(c.email()) && !EMAIL.matcher(c.email()).matches()) {
            erros.add("email");
        }
        opcional(erros, "contactNumber", c.telefone(), MAX_TELEFONE);
        if (preenchido(c.telefone()) && !TELEFONE.matcher(c.telefone()).matches()) {
            erros.add("contactNumber");
        }
        opcional(erros, "keywords", c.palavrasChave(), MAX_TEXTO);
        opcional(erros, "comment", c.comentario(), MAX_TEXTO);
        return erros;
    }

    public static boolean valido(Candidato c) {
        return camposInvalidos(c).isEmpty();
    }

    static boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static void obrigatorio(Set<String> erros, String campo, String valor, int limite) {
        if (!preenchido(valor) || valor.length() > limite) {
            erros.add(campo);
        }
    }

    private static void opcional(Set<String> erros, String campo, String valor, int limite) {
        if (valor != null && valor.length() > limite) {
            erros.add(campo);
        }
    }
}
