package br.pucrs.verval.orangehrm.e2e.pages;

import br.pucrs.verval.orangehrm.e2e.support.Config;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/** Admin > User Management > Users (/admin/viewSystemUsers): usado para conferir o login criado no fluxo A2. */
public class SystemUsersPage {

    private final Page page;

    public SystemUsersPage(Page page) {
        this.page = page;
    }

    public SystemUsersPage open() {
        page.navigate(Config.baseUrl() + "/admin/viewSystemUsers");
        page.locator(".oxd-table-filter").waitFor();
        return this;
    }

    public SystemUsersPage searchByUsername(String username) {
        page.locator(".oxd-table-filter .oxd-input-group")
                .filter(new Locator.FilterOptions().setHas(page.locator("label").getByText("Username",
                        new Locator.GetByTextOptions().setExact(true))))
                .locator("input").fill(username);
        page.locator(".oxd-table-filter").getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Search")).click();
        return this;
    }

    public Locator rows() {
        return page.locator(".oxd-table-body .oxd-table-row");
    }
}
