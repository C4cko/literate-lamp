package t2.claim.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente mínimo da API REST v2, usado só para PREPARAR dados de partida (funcionários, usuários e reivindicações
 * em um estado) e para confirmar efeitos que a tela não mostra. A ação sob teste é sempre feita pela tela.
 * O login imita o navegador: lê o token CSRF da página de login e envia o formulário (como em plano/evidencias).
 */
public final class OrangeApi {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern TOKEN = Pattern.compile(":token=\"&quot;(.+?)&quot;\"");

    private final HttpClient http;

    private OrangeApi(HttpClient http) {
        this.http = http;
    }

    public static OrangeApi login(String user, String password) {
        HttpClient http = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        String page = send(http, HttpRequest.newBuilder(URI.create(Config.BASE_URL + "/auth/login")).GET().build()).body();
        Matcher token = TOKEN.matcher(page);
        if (!token.find()) {
            throw new IllegalStateException("token CSRF não encontrado na página de login");
        }
        String form = "_token=" + enc(token.group(1)) + "&username=" + enc(user) + "&password=" + enc(password);
        HttpResponse<String> response = send(http, HttpRequest.newBuilder(URI.create(Config.BASE_URL + "/auth/validate"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build());
        String location = response.headers().firstValue("location").orElse("");
        if (!location.contains("dashboard")) {
            throw new IllegalStateException("login de " + user + " falhou (redirecionou para " + location + ")");
        }
        return new OrangeApi(http);
    }

    public JsonNode get(String path) {
        return call("GET", path, null);
    }

    public JsonNode post(String path, Object body) {
        return call("POST", path, body);
    }

    public JsonNode put(String path, Object body) {
        return call("PUT", path, body);
    }

    public JsonNode delete(String path, Object body) {
        return call("DELETE", path, body);
    }

    /** Chama a API e exige HTTP 200; devolve o JSON inteiro (data e meta). */
    private JsonNode call(String method, String path, Object body) {
        try {
            HttpRequest.BodyPublisher publisher = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body));
            HttpResponse<String> response = send(http, HttpRequest.newBuilder(URI.create(Config.BASE_URL + "/api/v2/" + path))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .method(method, publisher)
                    .build());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(method + " " + path + " -> HTTP " + response.statusCode() + ": " + response.body());
            }
            return JSON.readTree(response.body());
        } catch (IOException e) {
            throw new IllegalStateException(method + " " + path + " falhou", e);
        }
    }

    // ------------------------------------------------------------------ atalhos usados na preparação

    public int createEmployee(String firstName, String lastName) {
        return post("pim/employees", Map.of("firstName", firstName, "lastName", lastName)).at("/data/empNumber").asInt();
    }

    public void createEssUser(String username, String password, int empNumber) {
        post("admin/users", Map.of("username", username, "password", password, "status", true,
                "userRoleId", 2, "empNumber", empNumber));
    }

    public void assignSupervisor(int subordinate, int supervisor) {
        post("pim/employees/" + subordinate + "/supervisors", Map.of("empNumber", supervisor, "reportingMethodId", 1));
    }

    public void deleteEmployees(List<Integer> empNumbers) {
        delete("pim/employees", Map.of("ids", empNumbers));
    }

    /** Id de um item de cadastro (evento, tipo de despesa) pelo nome. */
    public int idByName(String path, String name) {
        for (JsonNode item : get(path + "?limit=100").get("data")) {
            if (name.equals(item.get("name").asText())) {
                return item.get("id").asInt();
            }
        }
        throw new IllegalStateException("'" + name + "' não encontrado em " + path);
    }

    /** POST /claim/requests: reivindicação do próprio usuário. Devolve o id. */
    public int createOwnClaim(int eventId, String remarks) {
        return post("claim/requests", Map.of("claimEventId", eventId, "currencyId", Config.CURRENCY_ID, "remarks", remarks))
                .at("/data/id").asInt();
    }

    /** POST /claim/employees/{empNumber}/requests: reivindicação atribuída a um funcionário. Devolve o id. */
    public int assignClaim(int empNumber, int eventId, String remarks) {
        return post("claim/employees/" + empNumber + "/requests",
                Map.of("claimEventId", eventId, "currencyId", Config.CURRENCY_ID, "remarks", remarks))
                .at("/data/id").asInt();
    }

    public void addExpense(int claimId, int expenseTypeId, double amount) {
        post("claim/requests/" + claimId + "/expenses",
                Map.of("expenseTypeId", expenseTypeId, "date", "2026-11-10", "amount", amount));
    }

    public void action(int claimId, String action) {
        put("claim/requests/" + claimId + "/action", Map.of("action", action));
    }

    public JsonNode expenses(int claimId) {
        return get("claim/requests/" + claimId + "/expenses").get("data");
    }

    private static HttpResponse<String> send(HttpClient http, HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IllegalStateException(request.method() + " " + request.uri() + " falhou", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrompido", e);
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
