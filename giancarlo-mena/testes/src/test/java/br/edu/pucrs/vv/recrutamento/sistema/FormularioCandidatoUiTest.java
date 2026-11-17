package br.edu.pucrs.vv.recrutamento.sistema;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;

@Tag("sistema")
@DisplayName("Sistema - validações do formulário Add Candidate")
class FormularioCandidatoUiTest extends SistemaBase {

    @Test
    @DisplayName("S02 - salvar o formulário vazio mostra Required nos 3 campos obrigatórios")
    void formularioVazio() {
        TelaCandidato tela = telaCandidato().abrirNovo().clicarSalvar();

        Locator obrigatorios = tela.mensagensDeErro()
                .filter(new Locator.FilterOptions().setHasText("Required"));
        assertThat(obrigatorios).hasCount(3);
        assertThat(tela.erroDe("Email")).hasText("Required");
    }

    @Test
    @DisplayName("S03 - e-mail sem domínio mostra o formato esperado")
    void emailInvalido() {
        TelaCandidato tela = telaCandidato().abrirNovo().digitarEmail("ana@");

        assertThat(tela.erroDe("Email")).hasText("Expected format: admin@example.com");
    }

    @Test
    @DisplayName("S04 - primeiro nome com 31 caracteres é recusado e com 30 é aceito")
    void limitePrimeiroNome() {
        TelaCandidato tela = telaCandidato().abrirNovo().digitarPrimeiroNome("N".repeat(31));
        Locator erroLimite = tela.mensagensDeErro()
                .filter(new Locator.FilterOptions().setHasText("Should not exceed 30 characters"));
        assertThat(erroLimite).hasCount(1);

        tela.digitarPrimeiroNome("N".repeat(30));
        assertThat(erroLimite).hasCount(0);
    }

    @Test
    @DisplayName("S05 - telefone com letras mostra a mensagem de formato")
    void telefoneComLetras() {
        TelaCandidato tela = telaCandidato().abrirNovo().digitarTelefone("abc");

        assertThat(tela.erroDe("Contact Number")).hasText("Allows numbers and only + - / ( )");
    }
}
