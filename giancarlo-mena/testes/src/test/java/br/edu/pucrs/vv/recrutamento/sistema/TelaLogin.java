package br.edu.pucrs.vv.recrutamento.sistema;

import java.util.regex.Pattern;

import com.microsoft.playwright.Page;

class TelaLogin extends Tela {

    TelaLogin(Page page, String url) {
        super(page, url);
    }

    void entrar(String usuario, String senha) {
        page.navigate(url + "/auth/login");
        page.locator("input[name='username']").fill(usuario);
        page.locator("input[name='password']").fill(senha);
        page.locator("button[type='submit']").click();
        page.waitForURL(Pattern.compile(".*/dashboard/index.*"));
    }
}
