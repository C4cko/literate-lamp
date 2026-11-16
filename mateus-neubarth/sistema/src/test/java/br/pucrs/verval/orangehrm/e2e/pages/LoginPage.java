package br.pucrs.verval.orangehrm.e2e.pages;

import br.pucrs.verval.orangehrm.e2e.support.Config;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** Tela de login (/auth/login). */
public class LoginPage {

    private final Page page;

    public LoginPage(Page page) {
        this.page = page;
    }

    public LoginPage open() {
        page.navigate(Config.baseUrl() + "/auth/login");
        return this;
    }

    public void loginAs(String username, String password) {
        page.getByPlaceholder("Username").fill(username);
        page.getByPlaceholder("Password").fill(password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();
        page.waitForURL("**/dashboard/index");
    }
}
