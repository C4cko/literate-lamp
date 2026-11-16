package br.pucrs.verval.orangehrm.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/** Tela Personal Details (/pim/viewPersonalDetails/empNumber/{n}), aberta depois de salvar o cadastro. */
public class PersonalDetailsPage {

    private final Page page;

    public PersonalDetailsPage(Page page) {
        this.page = page;
    }

    /** Nome completo exibido no topo do perfil. */
    public Locator employeeName() {
        return page.locator(".orangehrm-edit-employee-name");
    }

    /** Imagem do perfil; com foto cadastrada, aponta para /pim/viewPhoto/empNumber/{n}. */
    public Locator profilePicture() {
        return page.locator(".orangehrm-edit-employee-image img");
    }
}
