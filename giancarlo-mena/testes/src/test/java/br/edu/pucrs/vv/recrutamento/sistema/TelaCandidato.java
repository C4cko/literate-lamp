package br.edu.pucrs.vv.recrutamento.sistema;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import br.edu.pucrs.vv.recrutamento.modelo.Acao;
import br.edu.pucrs.vv.recrutamento.modelo.Candidato;

/**
 * Tela Recruitment › Candidates › Add ({@code /recruitment/addCandidate}) e perfil do candidato
 * ({@code /recruitment/addCandidate/{id}}), que mostra o status e os botões de ação.
 */
class TelaCandidato extends Tela {

    private static final Pattern URL_PERFIL = Pattern.compile(".*/recruitment/addCandidate/(\\d+).*");

    TelaCandidato(Page page, String url) {
        super(page, url);
    }

    TelaCandidato abrirNovo() {
        page.navigate(url + "/recruitment/addCandidate");
        page.locator("input[name='firstName']").waitFor();
        return this;
    }

    TelaCandidato abrirPerfil(long id) {
        page.navigate(url + "/recruitment/addCandidate/" + id);
        status().waitFor();
        return this;
    }

    TelaCandidato preencher(Candidato c, String nomeVaga) {
        preencherSeInformado(page.locator("input[name='firstName']"), c.primeiroNome());
        preencherSeInformado(page.locator("input[name='middleName']"), c.nomeDoMeio());
        preencherSeInformado(page.locator("input[name='lastName']"), c.sobrenome());
        if (nomeVaga != null) {
            grupo("Vacancy").locator(".oxd-select-text").click();
            page.locator(".oxd-select-option").filter(new Locator.FilterOptions().setHasText(nomeVaga)).first().click();
        }
        preencherSeInformado(campo("Email"), c.email());
        preencherSeInformado(campo("Contact Number"), c.telefone());
        return this;
    }

    TelaCandidato digitarPrimeiroNome(String nome) {
        page.locator("input[name='firstName']").fill(nome);
        return this;
    }

    TelaCandidato digitarEmail(String email) {
        campo("Email").fill(email);
        return this;
    }

    TelaCandidato digitarTelefone(String telefone) {
        campo("Contact Number").fill(telefone);
        return this;
    }

    TelaCandidato clicarSalvar() {
        salvar();
        return this;
    }

    /** Salva um candidato válido e devolve o id lido do endereço do perfil. */
    long salvarEObterId() {
        salvar();
        page.waitForURL(URL_PERFIL);
        Matcher m = URL_PERFIL.matcher(page.url());
        if (!m.matches()) {
            throw new IllegalStateException("Endereço inesperado após salvar: " + page.url());
        }
        return Long.parseLong(m.group(1));
    }

    Locator status() {
        return page.locator(".orangehrm-recruitment-status");
    }

    Locator botoesDeAcao() {
        return page.locator(".orangehrm-recruitment-actions button");
    }

    void clicarAcao(Acao acao) {
        botoesDeAcao().filter(new Locator.FilterOptions()
                .setHasText(Pattern.compile("^\\s*" + Pattern.quote(acao.rotulo()) + "\\s*$"))).click();
        page.waitForURL(Pattern.compile(".*/recruitment/changeCandidateVacancyStatus.*"));
    }

    private static void preencherSeInformado(Locator campo, String valor) {
        if (valor != null) {
            campo.fill(valor);
        }
    }
}
