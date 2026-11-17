package br.edu.pucrs.vv.recrutamento.sistema;

import java.time.LocalDate;
import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Tela de mudança de status ({@code /recruitment/changeCandidateVacancyStatus}). Para quase todas as ações
 * ela só tem o campo Notes; para "Schedule Interview" tem título, entrevistador, data e hora.
 */
class TelaAcao extends Tela {

    TelaAcao(Page page, String url) {
        super(page, url);
    }

    /** Confirma a ação e espera voltar ao perfil do candidato. */
    void confirmar(String nota) {
        Locator notas = page.locator("textarea").first();
        notas.waitFor();
        notas.fill(nota);
        salvar();
        page.waitForURL(Pattern.compile(".*/recruitment/addCandidate/\\d+.*"));
    }

    void agendarEntrevista(String titulo, String nomeEntrevistador, LocalDate data) {
        campo("Interview Title").fill(titulo);

        Locator entrevistador = campo("Interviewer");
        entrevistador.pressSequentially(nomeEntrevistador, new Locator.PressSequentiallyOptions().setDelay(80));
        Locator opcao = page.locator(".oxd-autocomplete-option")
                .filter(new Locator.FilterOptions().setHasText(nomeEntrevistador)).first();
        opcao.waitFor();
        opcao.click();

        Locator campoData = campo("Date");
        campoData.fill(formatarData(data, campoData.getAttribute("placeholder")));
        page.keyboard().press("Escape");

        confirmar("Entrevista agendada pelos testes de sistema");
    }

    /**
     * O formato da data depende da configuração do servidor (ex.: yyyy-dd-mm no servidor de demonstração).
     * O placeholder do campo mostra o formato em uso.
     */
    static String formatarData(LocalDate data, String placeholder) {
        String formato = placeholder == null || placeholder.isBlank() ? "yyyy-mm-dd" : placeholder.toLowerCase();
        return formato
                .replace("yyyy", String.format("%04d", data.getYear()))
                .replace("mm", String.format("%02d", data.getMonthValue()))
                .replace("dd", String.format("%02d", data.getDayOfMonth()));
    }
}
