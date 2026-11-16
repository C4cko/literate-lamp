package br.pucrs.verval.orangehrm.e2e.pages;

import br.pucrs.verval.orangehrm.e2e.support.Config;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.FilePayload;

/**
 * Formulário PIM > Add Employee (/pim/addEmployee, componente SaveEmployee.vue).
 * Os campos são localizados pelo placeholder, pelo atributo name ou pelo rótulo visível; nunca por
 * posição ou XPath absoluto (lição da análise de terceiros).
 */
public class AddEmployeePage {

    private final Page page;

    public AddEmployeePage(Page page) {
        this.page = page;
    }

    public AddEmployeePage open() {
        page.navigate(Config.baseUrl() + "/pim/addEmployee");
        // O título h6 "Add Employee" existe mas fica oculto no layout 5.9; o campo First Name indica o formulário pronto.
        page.getByPlaceholder("First Name").waitFor();
        return this;
    }

    // ------------------------------------------------------------------ nomes e Employee Id

    public AddEmployeePage fillName(String first, String middle, String last) {
        page.getByPlaceholder("First Name").fill(first == null ? "" : first);
        page.getByPlaceholder("Middle Name").fill(middle == null ? "" : middle);
        page.getByPlaceholder("Last Name").fill(last == null ? "" : last);
        return this;
    }

    public Locator firstName() {
        return page.locator("input[name='firstName']");
    }

    public Locator employeeId() {
        return field("Employee Id");
    }

    public AddEmployeePage setEmployeeId(String id) {
        employeeId().fill(id);
        return this;
    }

    // ------------------------------------------------------------------ foto

    /** Anexa a foto pelo input de arquivo do ProfileImageInput (o input fica oculto atrás do avatar). */
    public AddEmployeePage attachPhoto(String fileName, String mimeType, byte[] content) {
        page.locator("input[type='file']").setInputFiles(new FilePayload(fileName, mimeType, content));
        return this;
    }

    /** Mensagem de erro do campo de foto. */
    public Locator photoError() {
        return page.locator("input[type='file']")
                .locator("xpath=ancestor::div[contains(@class,'oxd-input-group')][1]")
                .locator(".oxd-input-field-error-message");
    }

    // ------------------------------------------------------------------ Create Login Details

    public AddEmployeePage enableLogin() {
        page.locator(".oxd-switch-input").click();
        field("Username").waitFor();
        return this;
    }

    public AddEmployeePage fillLogin(String username, String password, String confirmPassword) {
        field("Username").fill(username);
        field("Password").fill(password);
        field("Confirm Password").fill(confirmPassword);
        return this;
    }

    public AddEmployeePage setStatusDisabled() {
        page.getByText("Disabled", new Page.GetByTextOptions().setExact(true)).click();
        return this;
    }

    // ------------------------------------------------------------------ localização genérica

    /** Input do campo pelo texto exato do rótulo (o OXD não associa label e input por "for"). */
    public Locator field(String label) {
        return group(label).locator("input");
    }

    /** Mensagem de erro exibida sob o campo com o rótulo informado. */
    public Locator errorFor(String label) {
        return group(label).locator(".oxd-input-field-error-message");
    }

    /** Mensagem de erro sob um dos campos de nome (firstName, middleName, lastName). */
    public Locator nameError(String inputName) {
        return page.locator("input[name='" + inputName + "']")
                .locator("xpath=ancestor::div[contains(@class,'oxd-input-group')][1]")
                .locator(".oxd-input-field-error-message");
    }

    private Locator group(String label) {
        return page.locator(".oxd-input-group")
                .filter(new Locator.FilterOptions().setHas(
                        page.locator("label").getByText(label, new Locator.GetByTextOptions().setExact(true))));
    }

    // ------------------------------------------------------------------ ações

    public void save() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save")).click();
    }

    public void cancel() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Cancel")).click();
    }

    /**
     * Toast de feedback. Em SaveEmployee.vue o toast é exibido ANTES do navigate() para Personal Details,
     * que recarrega a página; por isso ele deve ser verificado logo após save(), antes de waitForProfile().
     */
    public Locator toast() {
        return page.locator(".oxd-toast");
    }

    /** Espera a tela Personal Details do novo funcionário e devolve o empNumber da URL. */
    public int waitForProfile() {
        page.waitForURL("**/pim/viewPersonalDetails/empNumber/*");
        String url = page.url();
        return Integer.parseInt(url.substring(url.lastIndexOf('/') + 1));
    }
}
