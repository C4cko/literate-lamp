package br.edu.pucrs.vv.recrutamento.integracao;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;

import br.edu.pucrs.vv.recrutamento.cliente.Resposta;
import br.edu.pucrs.vv.recrutamento.modelo.Candidato;
import br.edu.pucrs.vv.recrutamento.modelo.RegrasCandidato;
import br.edu.pucrs.vv.recrutamento.modelo.Status;

@Tag("integracao")
@DisplayName("Integração - cadastro de candidato (POST /recruitment/candidates)")
class CadastroCandidatoApiTest extends CenarioRecrutamento {

    @Test
    @DisplayName("I01 - candidato válido é salvo com os mesmos dados e status Application Initiated")
    void cadastroValido() {
        Candidato enviado = novoCandidato();
        Resposta r = api.cadastrar(enviado);
        assertEquals(200, r.status(), r.resumo());
        long id = r.id();
        registrarCandidato(id);

        JsonNode salvo = api.candidato(id);
        assertAll(
                () -> assertEquals(enviado.primeiroNome(), salvo.get("firstName").asText()),
                () -> assertEquals(enviado.sobrenome(), salvo.get("lastName").asText()),
                () -> assertEquals(enviado.email(), salvo.get("email").asText()),
                () -> assertEquals(enviado.telefone(), salvo.get("contactNumber").asText()),
                () -> assertEquals(vaga, salvo.get("vacancy").get("id").asLong()),
                () -> assertEquals(Status.APPLICATION_INITIATED.id(), salvo.get("status").get("id").asInt()));
    }

    @Test
    @DisplayName("I02 - primeiro nome com exatamente 30 caracteres é aceito")
    void primeiroNomeNoLimite() {
        Candidato c = novoCandidato().comPrimeiroNome("N".repeat(30));
        assertTrue(RegrasCandidato.valido(c), "pré-condição: o modelo considera válido");

        Resposta r = api.cadastrar(c);
        assertEquals(200, r.status(), r.resumo());
        registrarCandidato(r.id());
        assertEquals(30, api.candidato(r.id()).get("firstName").asText().length());
    }

    static Stream<Arguments> candidatosInvalidos() {
        return Stream.of(
                Arguments.of("sem primeiro nome", (UnaryOperator<Candidato>) c -> c.comPrimeiroNome(null)),
                Arguments.of("sem sobrenome", (UnaryOperator<Candidato>) c -> c.comSobrenome(null)),
                Arguments.of("e-mail 'ana@'", (UnaryOperator<Candidato>) c -> c.comEmail("ana@")),
                Arguments.of("primeiro nome com 31", (UnaryOperator<Candidato>) c -> c.comPrimeiroNome("N".repeat(31))),
                Arguments.of("telefone 'abc'", (UnaryOperator<Candidato>) c -> c.comTelefone("abc")));
    }

    @DisplayName("I03 - dados inválidos são recusados com 422")
    @ParameterizedTest(name = "{0}")
    @MethodSource("candidatosInvalidos")
    void cadastroInvalido(String descricao, UnaryOperator<Candidato> alteracao) {
        Candidato c = alteracao.apply(novoCandidato());
        assertFalse(RegrasCandidato.valido(c), "pré-condição: o modelo considera inválido");

        Resposta r = api.cadastrar(c);
        if (r.sucesso()) {
            registrarCandidato(r.id());
        }
        assertEquals(422, r.status(), descricao + ": " + r.resumo());
    }
}
