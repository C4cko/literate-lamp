package br.pucrs.verval.orangehrm.api.support;

import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente fino para /api/v2/pim/employees. Registra os funcionários criados para que o teste os
 * exclua no fim ({@link #cleanup()}), mantendo os testes independentes.
 */
public final class EmployeeApi {

    /** Prop do componente Vue em /pim/addEmployee: o Employee Id pré-preenchido no formulário. */
    private static final Pattern SUGGESTED_ID = Pattern.compile(":emp-id=\"(?:&quot;)?([0-9A-Za-z]+)(?:&quot;)?\"");

    private final OrangeHrmSession session;
    private final List<Integer> created = new ArrayList<>();

    public EmployeeApi(OrangeHrmSession session) {
        this.session = session;
    }

    public static Map<String, Object> body(String firstName, String lastName) {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        return body;
    }

    public Response create(Map<String, Object> body) {
        Response response = session.api().body(body).post("/pim/employees");
        if (response.statusCode() == 200) {
            created.add(response.path("data.empNumber"));
        }
        return response;
    }

    public Response create(String firstName, String lastName) {
        return create(body(firstName, lastName));
    }

    public Response get(int empNumber) {
        return session.api().get("/pim/employees/" + empNumber);
    }

    public Response picture(int empNumber) {
        return session.api().get("/pim/employees/" + empNumber + "/picture");
    }

    /** Total de funcionários ativos (meta.total), para verificar que uma requisição inválida não gravou nada. */
    public int total() {
        return session.api().queryParam("limit", 1).get("/pim/employees").then().statusCode(200)
                .extract().path("meta.total");
    }

    /** Employee Id que o formulário Add Employee sugere agora (mesma fonte que o navegador recebe). */
    public String suggestedEmployeeId() {
        String html = session.page("/pim/addEmployee");
        Matcher matcher = SUGGESTED_ID.matcher(html);
        if (!matcher.find()) {
            throw new IllegalStateException("emp-id não encontrado na página Add Employee");
        }
        return matcher.group(1);
    }

    public void cleanup() {
        if (!created.isEmpty()) {
            session.api().body(Map.of("ids", created)).delete("/pim/employees");
            created.clear();
        }
    }
}
