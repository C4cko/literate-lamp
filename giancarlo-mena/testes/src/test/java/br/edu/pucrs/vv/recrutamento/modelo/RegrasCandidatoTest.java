package br.edu.pucrs.vv.recrutamento.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RegrasCandidatoTest {

    private final Candidato base = Candidato.valido("VV0001");

    private static String texto(int tamanho) {
        return "a".repeat(tamanho);
    }

    @Test
    @DisplayName("U10 - candidato completo e válido não tem erros")
    void candidatoValido() {
        assertTrue(RegrasCandidato.valido(base), () -> RegrasCandidato.camposInvalidos(base).toString());
    }

    /** Técnica: tabela de decisão com as 8 combinações dos 3 campos obrigatórios (P = presente, A = ausente). */
    @DisplayName("U11 - tabela de decisão dos campos obrigatórios")
    @ParameterizedTest(name = "regra {index}: primeiro={0} sobrenome={1} email={2}")
    @CsvSource({
        "P, P, P",
        "P, P, A",
        "P, A, P",
        "P, A, A",
        "A, P, P",
        "A, P, A",
        "A, A, P",
        "A, A, A",
    })
    void camposObrigatorios(String primeiro, String sobrenome, String email) {
        Candidato c = base
                .comPrimeiroNome(primeiro.equals("P") ? base.primeiroNome() : null)
                .comSobrenome(sobrenome.equals("P") ? base.sobrenome() : null)
                .comEmail(email.equals("P") ? base.email() : null);

        Set<String> esperado = new TreeSet<>();
        if (primeiro.equals("A")) {
            esperado.add("firstName");
        }
        if (sobrenome.equals("A")) {
            esperado.add("lastName");
        }
        if (email.equals("A")) {
            esperado.add("email");
        }
        assertEquals(esperado, RegrasCandidato.camposInvalidos(c));
    }

    @DisplayName("U12 - valor-limite do primeiro nome")
    @ParameterizedTest(name = "{0} caracteres -> válido = {1}")
    @CsvSource({"1, true", "29, true", "30, true", "31, false"})
    void limitePrimeiroNome(int tamanho, boolean valido) {
        assertEquals(valido, RegrasCandidato.valido(base.comPrimeiroNome(texto(tamanho))));
    }

    @DisplayName("U13 - valor-limite do sobrenome e do nome do meio")
    @ParameterizedTest(name = "{0} caracteres -> válido = {1}")
    @CsvSource({"30, true", "31, false"})
    void limiteSobrenomeENomeDoMeio(int tamanho, boolean valido) {
        assertEquals(valido, RegrasCandidato.valido(base.comSobrenome(texto(tamanho))));
        assertEquals(valido, RegrasCandidato.valido(base.comNomeDoMeio(texto(tamanho))));
    }

    @DisplayName("U14 - partição de e-mail: classes válidas")
    @ParameterizedTest
    @ValueSource(strings = {"ana@empresa.com", "ana.souza+rh@sub.empresa.com.br", "a_b-c@x1.io"})
    void emailValido(String email) {
        assertTrue(RegrasCandidato.valido(base.comEmail(email)));
    }

    @DisplayName("U14 - partição de e-mail: classes inválidas")
    @ParameterizedTest
    @ValueSource(strings = {"ana", "ana@", "@empresa.com", "ana@empresa", "ana empresa@x.com", "ana@-x.com"})
    void emailInvalido(String email) {
        assertEquals(Set.of("email"), RegrasCandidato.camposInvalidos(base.comEmail(email)));
    }

    @DisplayName("U14 - valor-limite do e-mail (50 caracteres na tela)")
    @ParameterizedTest(name = "{0} caracteres -> válido = {1}")
    @CsvSource({"50, true", "51, false"})
    void limiteEmail(int tamanho, boolean valido) {
        String dominio = "@exemplo.com";
        String email = texto(tamanho - dominio.length()) + dominio;
        assertEquals(tamanho, email.length());
        assertEquals(valido, RegrasCandidato.valido(base.comEmail(email)));
    }

    @DisplayName("U15 - nome só com espaços é tratado como ausente")
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    void nomeEmBranco(String nome) {
        assertEquals(Set.of("firstName"), RegrasCandidato.camposInvalidos(base.comPrimeiroNome(nome)));
    }

    @DisplayName("U16 - partição de telefone: classes válidas (inclui vazio, campo opcional)")
    @ParameterizedTest
    @ValueSource(strings = {"51 3320-3500", "+55 (51) 99999-0000", "3320/3500", ""})
    void telefoneValido(String telefone) {
        assertTrue(RegrasCandidato.valido(base.comTelefone(telefone)));
    }

    @Test
    @DisplayName("U16 - telefone ausente (null) é válido")
    void telefoneAusente() {
        assertTrue(RegrasCandidato.valido(base.comTelefone(null)));
    }

    @DisplayName("U16 - partição de telefone: classes inválidas")
    @ParameterizedTest
    @ValueSource(strings = {"abc123", "51#3320", "ramal 12"})
    void telefoneInvalido(String telefone) {
        assertEquals(Set.of("contactNumber"), RegrasCandidato.camposInvalidos(base.comTelefone(telefone)));
    }

    @DisplayName("U16 - valor-limite do telefone (25 caracteres na tela)")
    @ParameterizedTest(name = "{0} dígitos -> válido = {1}")
    @CsvSource({"25, true", "26, false"})
    void limiteTelefone(int tamanho, boolean valido) {
        assertEquals(valido, RegrasCandidato.valido(base.comTelefone("9".repeat(tamanho))));
    }

    @DisplayName("U17 - valor-limite de palavras-chave e comentário")
    @ParameterizedTest(name = "{0} caracteres -> válido = {1}")
    @CsvSource({"250, true", "251, false"})
    void limiteTextosLongos(int tamanho, boolean valido) {
        assertEquals(valido, RegrasCandidato.valido(base.comPalavrasChave(texto(tamanho))));
        assertEquals(valido, RegrasCandidato.valido(base.comComentario(texto(tamanho))));
    }

    @Test
    @DisplayName("Corpo da API não envia campos nulos e usa os nomes do CandidateAPI")
    void corpoDaApi() {
        var corpo = base.comNomeDoMeio(null).naVaga(12L).comoCorpoApi();
        assertEquals("Ana", corpo.get("firstName"));
        assertEquals(12L, corpo.get("vacancyId"));
        assertTrue(!corpo.containsKey("middleName"));
        assertEquals(false, corpo.get("consentToKeepData"));
    }
}
