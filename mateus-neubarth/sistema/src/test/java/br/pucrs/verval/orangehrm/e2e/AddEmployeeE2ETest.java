package br.pucrs.verval.orangehrm.e2e;

import br.pucrs.verval.orangehrm.e2e.pages.AddEmployeePage;
import br.pucrs.verval.orangehrm.e2e.pages.PersonalDetailsPage;
import br.pucrs.verval.orangehrm.e2e.pages.SystemUsersPage;
import br.pucrs.verval.orangehrm.e2e.support.BaseE2ETest;
import br.pucrs.verval.orangehrm.e2e.support.Images;
import br.pucrs.verval.orangehrm.e2e.support.TestData;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Casos de sistema dos fluxos de sucesso da jornada (docs/casos-de-teste.md, seção 4.3):
 * ST-01, ST-02, ST-03, ST-04 e ST-13.
 */
@DisplayName("Sistema: PIM > Add Employee (fluxos de sucesso)")
class AddEmployeeE2ETest extends BaseE2ETest {

    /** Salva, confere o toast antes do redirecionamento e registra o funcionário para limpeza. */
    private int saveSuccessfully(AddEmployeePage form) {
        form.save();
        try {
            assertThat(form.toast()).containsText("Successfully Saved");
        } finally {
            // registra mesmo se o toast falhar, para a limpeza não deixar lixo no servidor
            registerCreatedEmployee(form.waitForProfile());
        }
        return Integer.parseInt(page.url().substring(page.url().lastIndexOf('/') + 1));
    }

    @Test
    @DisplayName("ST-01: cadastro mínimo mostra o toast de sucesso e abre Personal Details")
    void st01_cadastroMinimo() {
        String firstName = "Ana" + TestData.unique();

        AddEmployeePage form = new AddEmployeePage(page).open().fillName(firstName, null, "Teste");
        saveSuccessfully(form);

        assertThat(new PersonalDetailsPage(page).employeeName()).containsText(firstName + " Teste");
    }

    @Test
    @DisplayName("ST-02: cadastro com foto grava a foto do funcionário (A1)")
    void st02_cadastroComFoto() {
        AddEmployeePage form = new AddEmployeePage(page).open()
                .fillName("Ana" + TestData.unique(), null, "Foto")
                .attachPhoto("perfil.png", "image/png", Images.pngOfSize(2_000));

        int empNumber = saveSuccessfully(form);

        JsonObject picture = apiGet("/pim/employees/" + empNumber + "/picture").getAsJsonObject("data");
        assertThat(picture.get("filename").getAsString()).isEqualTo("perfil.png");
        assertThat(picture.get("fileType").getAsString()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("ST-03: cadastro com login cria usuário ESS visível em User Management (A2)")
    void st03_cadastroComLogin() {
        String username = TestData.username();
        AddEmployeePage form = new AddEmployeePage(page).open()
                .fillName("Ana" + TestData.unique(), null, "Login")
                .enableLogin()
                .fillLogin(username, TestData.STRONG_PASSWORD, TestData.STRONG_PASSWORD);

        saveSuccessfully(form);

        SystemUsersPage users = new SystemUsersPage(page).open().searchByUsername(username);
        assertThat(users.rows()).hasCount(1);
        assertThat(users.rows().first()).containsText(username);
        assertThat(users.rows().first()).containsText("ESS");
        assertThat(users.rows().first()).containsText("Enabled");
    }

    @Test
    @DisplayName("ST-04: cadastro com foto e login, usuário desativado (tabela de decisão, regra R-4)")
    void st04_cadastroComFotoELogin() {
        String username = TestData.username();
        AddEmployeePage form = new AddEmployeePage(page).open()
                .fillName("Ana" + TestData.unique(), null, "Completa")
                .attachPhoto("perfil.png", "image/png", Images.pngOfSize(2_000))
                .enableLogin()
                .fillLogin(username, TestData.STRONG_PASSWORD, TestData.STRONG_PASSWORD)
                .setStatusDisabled();

        int empNumber = saveSuccessfully(form);

        assertThat(apiGet("/pim/employees/" + empNumber + "/picture").getAsJsonObject("data")
                .get("filename").getAsString()).isEqualTo("perfil.png");
        SystemUsersPage users = new SystemUsersPage(page).open().searchByUsername(username);
        assertThat(users.rows()).hasCount(1);
        assertThat(users.rows().first()).containsText("Disabled");
    }

    @Test
    @DisplayName("ST-13: Cancel volta para a lista de funcionários sem gravar nada")
    void st13_cancelar() {
        int before = employeeTotal();
        AddEmployeePage form = new AddEmployeePage(page).open().fillName("Ana" + TestData.unique(), null, "Cancel");

        form.cancel();

        page.waitForURL("**/pim/viewEmployeeList");
        assertThat(employeeTotal()).isEqualTo(before);
    }
}
