package br.pucrs.verval.orangehrm.api;

import br.pucrs.verval.orangehrm.api.support.EmployeeApi;
import br.pucrs.verval.orangehrm.api.support.OrangeHrmSession;
import br.pucrs.verval.orangehrm.api.support.TestData;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static br.pucrs.verval.orangehrm.api.support.ApiAssertions.assertInvalidParams;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Casos de integração de POST /api/v2/pim/employees (docs/casos-de-teste.md, seção 4.2):
 * IT-01 a IT-04.
 */
@DisplayName("API REST v2: criação de funcionário")
class CreateEmployeeApiTest {

    private static OrangeHrmSession session;
    private EmployeeApi employees;

    @BeforeAll
    static void login() {
        session = OrangeHrmSession.admin();
    }

    @BeforeEach
    void setUp() {
        employees = new EmployeeApi(session);
    }

    @AfterEach
    void tearDown() {
        employees.cleanup();
    }

    @Test
    @DisplayName("IT-01: nome e sobrenome válidos criam o funcionário (fluxo principal)")
    void it01_criaFuncionarioComDadosMinimos() {
        String firstName = "Ana" + TestData.unique();

        Response created = employees.create(firstName, "Teste");

        assertThat(created.statusCode()).isEqualTo(200);
        int empNumber = created.path("data.empNumber");
        assertThat(empNumber).isPositive();

        Response fetched = employees.get(empNumber);
        assertThat(fetched.statusCode()).isEqualTo(200);
        assertThat((String) fetched.path("data.firstName")).isEqualTo(firstName);
        assertThat((String) fetched.path("data.lastName")).isEqualTo("Teste");
        assertThat((String) fetched.path("data.middleName")).isEmpty();
    }

    @ParameterizedTest(name = "IT-02: sem {0} -> 422 em {0}")
    @CsvSource({"firstName", "lastName"})
    @DisplayName("IT-02: nome obrigatório ausente é recusado e nada é gravado (E1)")
    void it02_nomeObrigatorioAusente(String field) {
        int before = employees.total();
        Map<String, Object> body = EmployeeApi.body("Ana", "Teste");
        body.remove(field);

        assertInvalidParams(employees.create(body), field);
        assertThat(employees.total()).isEqualTo(before);
    }

    @ParameterizedTest(name = "IT-03: {0} com {1} caracteres -> aceito={2}")
    @CsvSource({
            "firstName, 30, true",
            "firstName, 31, false",
            "middleName, 30, true",
            "middleName, 31, false",
            "lastName, 30, true",
            "lastName, 31, false",
    })
    @DisplayName("IT-03: limite de 30 caracteres nos nomes (E7, valor-limite)")
    void it03_limiteDosNomes(String field, int length, boolean accepted) {
        Map<String, Object> body = EmployeeApi.body("Ana", "Teste");
        body.put(field, TestData.ofLength(length));

        Response response = employees.create(body);

        if (accepted) {
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat((String) response.path("data." + field)).hasSize(length);
        } else {
            assertInvalidParams(response, field);
        }
    }

    @Test
    @DisplayName("IT-04: Employee Id repetido é recusado (E2)")
    void it04_employeeIdDuplicado() {
        String employeeId = TestData.employeeId();
        Map<String, Object> first = EmployeeApi.body("Ana", "Primeira");
        first.put("employeeId", employeeId);
        assertThat(employees.create(first).statusCode()).isEqualTo(200);

        Map<String, Object> second = EmployeeApi.body("Bia", "Segunda");
        second.put("employeeId", employeeId);
        assertInvalidParams(employees.create(second), "employeeId");
    }

    @ParameterizedTest(name = "IT-04: employeeId com {0} caracteres -> aceito={1}")
    @CsvSource({"10, true", "11, false"})
    @DisplayName("IT-04: limite de 10 caracteres no Employee Id (E7, valor-limite)")
    void it04_limiteDoEmployeeId(int length, boolean accepted) {
        String id = (TestData.employeeId() + "XXXXXXXXXXX").substring(0, length);
        Map<String, Object> body = EmployeeApi.body("Ana", "Teste");
        body.put("employeeId", id);

        Response response = employees.create(body);

        if (accepted) {
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat((String) response.path("data.employeeId")).isEqualTo(id);
        } else {
            assertInvalidParams(response, "employeeId");
        }
    }
}
