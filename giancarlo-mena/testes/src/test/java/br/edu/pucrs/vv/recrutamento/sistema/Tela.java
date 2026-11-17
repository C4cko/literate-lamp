package br.edu.pucrs.vv.recrutamento.sistema;

import java.util.regex.Pattern;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Funções comuns às páginas do OrangeHRM (componentes "oxd" do front-end Vue). */
abstract class Tela {

    protected final Page page;
    protected final String url;

    protected Tela(Page page, String url) {
        this.page = page;
        this.url = url;
    }

    /** Grupo de campo ({@code .oxd-input-group}) cujo rótulo é exatamente o texto informado. */
    protected Locator grupo(String rotulo) {
        Locator label = page.locator("label",
                new Page.LocatorOptions().setHasText(Pattern.compile("^\\s*" + Pattern.quote(rotulo) + "\\s*$")));
        return page.locator(".oxd-input-group").filter(new Locator.FilterOptions().setHas(label));
    }

    protected Locator campo(String rotulo) {
        return grupo(rotulo).locator("input, textarea").first();
    }

    public Locator mensagensDeErro() {
        return page.locator(".oxd-input-field-error-message");
    }

    public Locator erroDe(String rotulo) {
        return grupo(rotulo).locator(".oxd-input-field-error-message");
    }

    protected void salvar() {
        page.locator("button[type='submit']").click();
    }
}
