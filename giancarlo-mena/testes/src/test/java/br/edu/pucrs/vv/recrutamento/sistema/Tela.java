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
        Locator label = page.locator("label", new Page.LocatorOptions().setHasText(textoExato(rotulo)));
        return page.locator(".oxd-input-group").filter(new Locator.FilterOptions().setHas(label));
    }

    /**
     * Expressão para texto exato (ignorando espaços nas pontas). Não usa Pattern.quote: o Playwright
     * executa a expressão no navegador (JavaScript), que não entende a sintaxe \\Q...\\E do Java.
     */
    static Pattern textoExato(String texto) {
        String escapado = texto.replaceAll("[.*+?^${}()|\\[\\]\\\\/]", "\\\\$0");
        return Pattern.compile("^\\s*" + escapado + "\\s*$");
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
