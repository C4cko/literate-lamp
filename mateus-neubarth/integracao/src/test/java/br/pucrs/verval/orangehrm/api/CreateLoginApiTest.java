package br.pucrs.verval.orangehrm.api;

import br.pucrs.verval.orangehrm.api.support.EmployeeApi;
import br.pucrs.verval.orangehrm.api.support.OrangeHrmSession;
import br.pucrs.verval.orangehrm.api.support.TestData;
import br.pucrs.verval.orangehrm.api.support.UserApi;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static br.pucrs.verval.orangehrm.api.support.ApiAssertions.assertInvalidParams;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fluxo A2 ("Create Login Details"): o formulário cria o funcionário e, em seguida, faz
 * POST /api/v2/admin/users com o empNumber recebido. Casos IT-08 a IT-11.
 */
@DisplayName("API REST v2: cadastro com login (funcionário + usuário)")
class CreateLoginApiTest {

    private static OrangeHrmSession session;
    private EmployeeApi employees;
    private UserApi users;

    @BeforeAll
    static void login() {
        session = OrangeHrmSession.admin();
    }

    @BeforeEach
    void setUp() {
        employees = new EmployeeApi(session);
        users = new UserApi(session);
    }

    @AfterEach
    void tearDown() {
        users.cleanup();       // usuários antes: dependem do funcionário
        employees.cleanup();
    }

    private int newEmployee() {
        Response created = employees.create("Ana" + TestData.unique(), "Login");
        assertThat(created.statusCode()).isEqualTo(200);
        return created.path("data.empNumber");
    }

    @Test
    @DisplayName("IT-08: funcionário + usuário ESS válidos ficam vinculados (A2, CA5)")
    void it08_criaUsuarioVinculado() {
        int empNumber = newEmployee();
        String username = TestData.username();

        Response created = users.create(username, TestData.STRONG_PASSWORD, false, empNumber);

        assertThat(created.statusCode()).isEqualTo(200);
        Response found = users.findByUsername(username);
        assertThat((Integer) found.path("meta.total")).isEqualTo(1);
        assertThat((Integer) found.path("data[0].employee.empNumber")).isEqualTo(empNumber);
        assertThat((String) found.path("data[0].userRole.name")).isEqualTo("ESS");
        assertThat((Boolean) found.path("data[0].status")).as("status Disabled escolhido").isFalse();
    }

    @ParameterizedTest(name = "IT-09: username com {0} caracteres -> aceito={1}")
    @CsvSource({"4, false", "5, true", "40, true", "41, false"})
    @DisplayName("IT-09: limites de 5 a 40 caracteres do username (E8, valor-limite)")
    void it09_limitesDoUsername(int length, boolean accepted) {
        int empNumber = newEmployee();
        String username = (TestData.username() + "x".repeat(41)).substring(0, length);

        Response response = users.create(username, TestData.STRONG_PASSWORD, true, empNumber);

        if (accepted) {
            assertThat(response.statusCode()).isEqualTo(200);
        } else {
            assertInvalidParams(response, "username");
        }
    }

    @Test
    @DisplayName("IT-09: username já existente é recusado (E4)")
    void it09_usernameExistente() {
        int empNumber = newEmployee();

        assertInvalidParams(users.create("Admin", TestData.STRONG_PASSWORD, true, empNumber), "username");
    }

    static Stream<Arguments> senhasInvalidas() {
        return Stream.of(
                Arguments.of("I13: 7 caracteres", "Ab1#xyz"),
                Arguments.of("I14: sem maiúscula", "t7#rw!9zp"),
                Arguments.of("I15: sem minúscula", "T7#RW!9ZP"),
                Arguments.of("I16: sem número", "tq#Rw!xZp"),
                Arguments.of("I17: sem especial", "t7kRwm9Zp"),
                Arguments.of("I18: com espaço", "t7#Rw 9Zp"),
                Arguments.of("I19: 65 caracteres", "t7#Rw!9Zp".repeat(7) + "ab"),
                Arguments.of("I20: previsível", "Password1!")
        );
    }

    @ParameterizedTest(name = "IT-10: {0} -> 422")
    @MethodSource("senhasInvalidas")
    @DisplayName("IT-10: senhas fora da política são recusadas (E6)")
    void it10_senhaForaDaPolitica(String classe, String password) {
        int empNumber = newEmployee();

        assertInvalidParams(users.create(TestData.username(), password, true, empNumber), "password");
    }

    @ParameterizedTest(name = "IT-10: {0} -> aceita")
    @CsvSource({"9 caracteres, t7#Rw!9Zp", "64 caracteres, t7#Rw!9Zpt7#Rw!9Zpt7#Rw!9Zpt7#Rw!9Zpt7#Rw!9Zpt7#Rw!9Zpt7#Rw!9Zp1"})
    @DisplayName("IT-10: senhas dentro da política são aceitas")
    void it10_senhaValida(String descricao, String password) {
        int empNumber = newEmployee();

        assertThat(users.create(TestData.username(), password, true, empNumber).statusCode()).isEqualTo(200);
    }

    /**
     * IT-10 (limite inferior anunciado): 8 caracteres cumprindo todas as regras deveria ser aceita.
     * FALHA no OrangeHRM 5.9: defeito D2 (docs/defeitos.md).
     */
    @Test
    @Tag("defeito-conhecido")
    @DisplayName("IT-10: senha de 8 caracteres que cumpre as regras é aceita (expõe D2)")
    void it10_senhaDe8Caracteres() {
        int empNumber = newEmployee();

        Response response = users.create(TestData.username(), "Qz7#mX2k", true, empNumber);

        assertThat(response.statusCode())
                .as("Defeito D2: o mínimo anunciado é 8, mas nenhuma senha de 8 caracteres atinge a força exigida")
                .isEqualTo(200);
    }

    /**
     * IT-11 (risco R1): reproduz a sequência do formulário com a 2ª requisição falhando no servidor.
     * Caracteriza o comportamento atual: o cadastro NÃO é atômico, e o funcionário permanece criado sem
     * login. O teste passa enquanto esse for o comportamento; se o sistema passar a desfazer o cadastro,
     * ele falha e avisa que a documentação do risco R1 deve ser revista.
     */
    @Test
    @DisplayName("IT-11 (R1): falha na criação do usuário deixa o funcionário criado sem login")
    void it11_cadastroNaoAtomico() {
        int empNumber = newEmployee();

        Response user = users.create("Admin", TestData.STRONG_PASSWORD, true, empNumber);

        assertInvalidParams(user, "username");
        assertThat(employees.get(empNumber).statusCode()).as("funcionário continua existindo").isEqualTo(200);
        List<?> logins = users.findByEmployee(empNumber).path("data");
        assertThat(logins).as("funcionário sem nenhum usuário").isEmpty();
    }
}
