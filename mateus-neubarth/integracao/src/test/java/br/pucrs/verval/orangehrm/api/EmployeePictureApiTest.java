package br.pucrs.verval.orangehrm.api;

import br.pucrs.verval.orangehrm.api.support.EmployeeApi;
import br.pucrs.verval.orangehrm.api.support.Images;
import br.pucrs.verval.orangehrm.api.support.OrangeHrmSession;
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
 * Casos de integração da foto no cadastro (fluxo A1 e exceção E3): IT-05, IT-06 e IT-07.
 */
@DisplayName("API REST v2: foto no cadastro de funcionário")
class EmployeePictureApiTest {

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

    private Response createWithPicture(Map<String, Object> picture) {
        Map<String, Object> body = EmployeeApi.body("Ana", "Foto");
        body.put("empPicture", picture);
        return employees.create(body);
    }

    @Test
    @DisplayName("IT-05: foto PNG válida é gravada e devolvida pelo endpoint da foto (A1)")
    void it05_fotoValidaEhGravada() {
        byte[] png = Images.pngOfSize(500);

        Response created = createWithPicture(Images.attachment("perfil.png", "image/png", png));

        assertThat(created.statusCode()).isEqualTo(200);
        Response picture = employees.picture(created.path("data.empNumber"));
        assertThat(picture.statusCode()).isEqualTo(200);
        assertThat((String) picture.path("data.filename")).isEqualTo("perfil.png");
        assertThat((String) picture.path("data.fileType")).isEqualTo("image/png");
        // a API serializa o tamanho como string ("500")
        assertThat(String.valueOf((Object) picture.path("data.size"))).isEqualTo("500");
    }

    @ParameterizedTest(name = "IT-06: {0} ({1}) -> 422")
    @CsvSource({
            "perfil.pdf, application/pdf",
            "perfil.bmp, image/bmp",
            "perfil.svg, image/svg+xml",
            "perfil.exe, image/png",
    })
    @DisplayName("IT-06: tipo ou extensão não permitidos são recusados (E3)")
    void it06_tipoNaoPermitido(String name, String type) {
        int before = employees.total();

        assertInvalidParams(createWithPicture(Images.attachment(name, type, Images.pngOfSize(200))), "empPicture");
        assertThat(employees.total()).isEqualTo(before);
    }

    /**
     * IT-07: limite de 1 MB (ConfigService::MAX_ATTACHMENT_SIZE = 1.048.576 bytes) também no servidor.
     * O risco R3 ("tamanho validado só no cliente") foi descartado: a regra Base64Attachment confere o
     * tamanho e, por padrão, compara o tamanho declarado com o conteúdo real (checkSizeStrictly).
     */
    @ParameterizedTest(name = "IT-07: {0} bytes -> aceito={1}")
    @CsvSource({"1048575, true", "1048576, true", "1048577, false"})
    @DisplayName("IT-07: limite de 1 MB da foto validado no servidor (valor-limite)")
    void it07_limiteDeTamanho(int bytes, boolean accepted) {
        Response response = createWithPicture(Images.attachment("grande.png", "image/png", Images.pngOfSize(bytes)));

        if (accepted) {
            assertThat(response.statusCode()).isEqualTo(200);
        } else {
            assertInvalidParams(response, "empPicture");
        }
    }

    @Test
    @DisplayName("IT-07: tamanho declarado menor que o conteúdo real é recusado (cliente não pode burlar o limite)")
    void it07_tamanhoDeclaradoFalso() {
        byte[] big = Images.pngOfSize(Images.ONE_MB + 1);

        assertInvalidParams(createWithPicture(Images.attachment("grande.png", "image/png", big, 1000)), "empPicture");
    }
}
