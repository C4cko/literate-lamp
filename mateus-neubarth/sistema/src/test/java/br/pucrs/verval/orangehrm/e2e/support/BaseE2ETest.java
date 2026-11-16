package br.pucrs.verval.orangehrm.e2e.support;

import br.pucrs.verval.orangehrm.e2e.pages.LoginPage;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base dos testes E2E: um navegador por classe e um contexto novo (cookies limpos) por teste,
 * login feito no próprio teste e exclusão, via API, dos funcionários criados.
 * Um trace do Playwright é salvo em target/traces/ para cada teste (abrir com `mvn exec`, ver docs).
 */
public abstract class BaseE2ETest {

    private static Playwright playwright;
    private static Browser browser;

    protected BrowserContext context;
    protected Page page;
    private final List<Integer> createdEmployees = new ArrayList<>();

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(Config.headless())
                .setSlowMo(Config.slowMo()));
    }

    @AfterAll
    static void closeBrowser() {
        playwright.close();
    }

    @BeforeEach
    void newContextAndLogin() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1366, 768));
        context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
        page = context.newPage();
        new LoginPage(page).open().loginAs(Config.adminUser(), Config.adminPassword());
    }

    @AfterEach
    void cleanupAndClose(TestInfo info) {
        for (Integer empNumber : createdEmployees) {
            // usuários vinculados (fluxo A2) primeiro, depois o funcionário
            JsonArray users = apiGet("/admin/users?empNumber=" + empNumber).getAsJsonArray("data");
            List<Integer> userIds = new ArrayList<>();
            users.forEach(u -> userIds.add(u.getAsJsonObject().get("id").getAsInt()));
            if (!userIds.isEmpty()) {
                context.request().delete(Config.baseUrl() + "/api/v2/admin/users",
                        RequestOptions.create().setData(Map.of("ids", userIds)));
            }
            context.request().delete(Config.baseUrl() + "/api/v2/pim/employees",
                    RequestOptions.create().setData(Map.of("ids", List.of(empNumber))));
        }
        String name = info.getTestMethod().map(m -> m.getName()).orElse("test");
        context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get("target/traces/" + name + ".zip")));
        context.close();
    }

    /** Registra um funcionário criado pelo teste para ser excluído no fim. */
    protected void registerCreatedEmployee(int empNumber) {
        createdEmployees.add(empNumber);
    }

    // ------------------------------------------------------------------ apoio via API (mesma sessão do navegador)
    // Usado só para preparar dados e para conferir efeitos; a ação testada é sempre feita pela interface.

    protected JsonObject apiGet(String path) {
        APIResponse response = context.request().get(Config.baseUrl() + "/api/v2" + path);
        return JsonParser.parseString(response.text()).getAsJsonObject();
    }

    /** Cria um funcionário pela API (pré-condição de um teste) e o registra para limpeza. */
    protected int createEmployeeViaApi(String firstName, String lastName, String employeeId) {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("employeeId", employeeId);
        APIResponse response = context.request().post(Config.baseUrl() + "/api/v2/pim/employees",
                RequestOptions.create().setData(body));
        int empNumber = JsonParser.parseString(response.text()).getAsJsonObject()
                .getAsJsonObject("data").get("empNumber").getAsInt();
        registerCreatedEmployee(empNumber);
        return empNumber;
    }

    /** Total de funcionários ativos, para verificar que uma ação recusada não gravou nada. */
    protected int employeeTotal() {
        return apiGet("/pim/employees?limit=1").getAsJsonObject("meta").get("total").getAsInt();
    }
}
