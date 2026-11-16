package br.pucrs.verval.orangehrm.api.support;

import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente fino para /api/v2/admin/users: é a segunda requisição que o formulário Add Employee faz quando
 * "Create Login Details" está ligado (SaveEmployee.vue). Exclui no fim os usuários criados.
 */
public final class UserApi {

    public static final int ESS_ROLE = 2;

    private final OrangeHrmSession session;
    private final List<Integer> created = new ArrayList<>();

    public UserApi(OrangeHrmSession session) {
        this.session = session;
    }

    /** Mesmo corpo que SaveEmployee.vue envia. */
    public Response create(String username, String password, boolean enabled, int empNumber) {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("status", enabled);
        body.put("userRoleId", ESS_ROLE);
        body.put("empNumber", empNumber);
        Response response = session.api().body(body).post("/admin/users");
        if (response.statusCode() == 200) {
            created.add(response.path("data.id"));
        }
        return response;
    }

    public Response findByUsername(String username) {
        return session.api().queryParam("username", username).get("/admin/users");
    }

    public Response findByEmployee(int empNumber) {
        return session.api().queryParam("empNumber", empNumber).get("/admin/users");
    }

    public void cleanup() {
        if (!created.isEmpty()) {
            session.api().body(Map.of("ids", created)).delete("/admin/users");
            created.clear();
        }
    }
}
