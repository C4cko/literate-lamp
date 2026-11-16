package br.pucrs.verval.orangehrm.e2e;

import br.pucrs.verval.orangehrm.e2e.pages.AddEmployeePage;
import br.pucrs.verval.orangehrm.e2e.support.BaseE2ETest;
import br.pucrs.verval.orangehrm.e2e.support.Images;
import br.pucrs.verval.orangehrm.e2e.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Casos de sistema das exceções da jornada (docs/casos-de-teste.md, seção 4.3): ST-05 a ST-11.
 * Oráculo: as mensagens oficiais (installer/Migration/*&#47;lang-string/*.yaml), a URL que não muda
 * e o total de funcionários, que não pode aumentar.
 */
@DisplayName("Sistema: PIM > Add Employee (exceções e limites)")
class AddEmployeeValidationE2ETest extends BaseE2ETest {

    private void assertNothingSaved(int totalBefore) {
        assertThat(page.url()).contains("/pim/addEmployee");
        assertThat(employeeTotal()).isEqualTo(totalBefore);
    }

    @ParameterizedTest(name = "ST-05: sem {0} -> Required")
    @CsvSource({"firstName", "lastName"})
    @DisplayName("ST-05: nome obrigatório vazio mostra Required e não salva (E1)")
    void st05_nomeVazio(String field) {
        int before = employeeTotal();
        AddEmployeePage form = new AddEmployeePage(page).open()
                .fillName(field.equals("firstName") ? "" : "Ana", null, field.equals("lastName") ? "" : "Teste");

        form.save();

        assertThat(form.nameError(field)).hasText("Required");
        assertNothingSaved(before);
    }

    @Test
    @DisplayName("ST-06: Employee Id de outro funcionário mostra 'Employee Id already exists' (E2)")
    void st06_employeeIdDuplicado() {
        String existing = TestData.employeeId();
        createEmployeeViaApi("Existente", "Teste", existing);
        int before = employeeTotal();

        AddEmployeePage form = new AddEmployeePage(page).open()
                .fillName("Ana", null, "Duplicada")
                .setEmployeeId(existing);
        form.save();

        assertThat(form.errorFor("Employee Id")).hasText("Employee Id already exists");
        assertNothingSaved(before);
    }

    @ParameterizedTest(name = "ST-07: {0} ({1}, {2} bytes) -> {3}")
    @CsvSource({
            "perfil.pdf, application/pdf, 2000, File type not allowed",
            "grande.png, image/png, 1048577, Attachment Size Exceeded",
    })
    @DisplayName("ST-07: foto com tipo inválido ou acima de 1 MB é recusada (E3)")
    void st07_fotoInvalida(String name, String mime, int bytes, String message) {
        AddEmployeePage form = new AddEmployeePage(page).open()
                .fillName("Ana", null, "Foto")
                .attachPhoto(name, mime, Images.pngOfSize(bytes));

        assertThat(form.photoError()).hasText(message);
    }

    @Test
    @DisplayName("ST-07: foto de exatamente 1 MB é aceita (limite)")
    void st07_fotoNoLimite() {
        AddEmployeePage form = new AddEmployeePage(page).open()
                .fillName("Ana" + TestData.unique(), null, "Foto1MB")
                .attachPhoto("limite.png", "image/png", Images.pngOfSize(Images.ONE_MB));

        assertThat(form.photoError()).hasCount(0);
        form.save();
        registerCreatedEmployee(form.waitForProfile());
    }

    @Test
    @DisplayName("ST-08: username já existente mostra 'Username already exists' (E4)")
    void st08_usernameExistente() {
        int before = employeeTotal();
        AddEmployeePage form = new AddEmployeePage(page).open()
                .fillName("Ana", null, "Login")
                .enableLogin()
                .fillLogin("Admin", TestData.STRONG_PASSWORD, TestData.STRONG_PASSWORD);
        form.save();

        assertThat(form.errorFor("Username")).hasText("Username already exists");
        assertNothingSaved(before);
    }

    @Test
    @DisplayName("ST-09: Confirm Password diferente mostra 'Passwords do not match' (E5)")
    void st09_senhasDiferentes() {
        int before = employeeTotal();
        AddEmployeePage form = new AddEmployeePage(page).open()
                .fillName("Ana", null, "Login")
                .enableLogin()
                .fillLogin(TestData.username(), TestData.STRONG_PASSWORD, TestData.STRONG_PASSWORD + "x");
        form.save();

        assertThat(form.errorFor("Confirm Password")).hasText("Passwords do not match");
        assertNothingSaved(before);
    }

    @ParameterizedTest(name = "ST-10: senha \"{0}\" -> {1}")
    @CsvSource(delimiter = '|', value = {
            "abc       | Should have at least 8 characters",
            "abcdefgh1#| Your password must contain minimum 1 upper-case letter",
            "Password1!| Your password meets the minimum requirements, but it could be guessable",
    })
    @DisplayName("ST-10: senha fora da política mostra a mensagem da política (E6)")
    void st10_senhaFraca(String password, String message) {
        int before = employeeTotal();
        AddEmployeePage form = new AddEmployeePage(page).open()
                .fillName("Ana", null, "Login")
                .enableLogin()
                .fillLogin(TestData.username(), password, password);
        form.save();

        assertThat(form.errorFor("Password")).hasText(message);
        assertNothingSaved(before);
    }

    /**
     * ST-10 (limite inferior anunciado): senha de 8 caracteres que cumpre as regras deve ser aceita.
     * FALHA no OrangeHRM 5.9: defeito D2 (docs/defeitos.md).
     */
    @Test
    @Tag("defeito-conhecido")
    @DisplayName("ST-10: senha de 8 caracteres que cumpre as regras é aceita (expõe D2)")
    void st10_senhaDe8Caracteres() {
        AddEmployeePage form = new AddEmployeePage(page).open()
                .fillName("Ana" + TestData.unique(), null, "Senha8")
                .enableLogin()
                .fillLogin(TestData.username(), "Qz7#mX2k", "Qz7#mX2k");
        form.save();

        try {
            page.waitForURL("**/pim/viewPersonalDetails/empNumber/*",
                    new com.microsoft.playwright.Page.WaitForURLOptions().setTimeout(10_000));
        } catch (com.microsoft.playwright.TimeoutError e) {
            // não salvou: a asserção abaixo falha com a mensagem exibida no campo
        }
        if (page.url().contains("/pim/viewPersonalDetails/")) {
            registerCreatedEmployee(form.waitForProfile());
        }
        assertThat(page.url())
                .as("Defeito D2: senha de 8 caracteres recusada com a mensagem \"%s\"",
                        page.url().contains("/pim/addEmployee") ? form.errorFor("Password").textContent() : "")
                .contains("/pim/viewPersonalDetails/");
    }

    @ParameterizedTest(name = "ST-11: {0} com {1} caracteres -> {2}")
    @CsvSource(delimiter = '|', value = {
            "First Name  | 31 | Should not exceed 30 characters",
            "Employee Id | 11 | Should not exceed 10 characters",
            "Username    | 4  | Should be at least 5 characters",
            "Username    | 41 | Should not exceed 40 characters",
    })
    @DisplayName("ST-11: limites de tamanho dos campos (E7, E8, valor-limite)")
    void st11_limites(String campo, int length, String message) {
        AddEmployeePage form = new AddEmployeePage(page).open().fillName("Ana", null, "Limite");
        String value = TestData.ofLength(length);
        switch (campo) {
            case "First Name" -> form.firstName().fill(value);
            case "Employee Id" -> form.setEmployeeId(value);
            default -> form.enableLogin().fillLogin(value, TestData.STRONG_PASSWORD, TestData.STRONG_PASSWORD);
        }
        form.save();

        if (campo.equals("First Name")) {
            assertThat(form.nameError("firstName")).hasText(message);
        } else {
            assertThat(form.errorFor(campo)).hasText(message);
        }
        assertThat(page.url()).contains("/pim/addEmployee");
    }
}
