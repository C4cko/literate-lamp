package br.pucrs.verval.orangehrm.api.support;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abre uma sessão autenticada como o navegador faz: busca o token CSRF na página de login
 * (prop {@code :token} do componente Vue, ver Login.vue) e envia o formulário para /auth/validate.
 * A API REST v2 aceita o cookie de sessão resultante.
 */
public final class OrangeHrmSession {

    private static final Pattern TOKEN = Pattern.compile(":token=\"&quot;([^&]+)&quot;\"");

    private final CookieFilter cookies = new CookieFilter();

    private OrangeHrmSession() {
    }

    public static OrangeHrmSession login(String username, String password) {
        OrangeHrmSession session = new OrangeHrmSession();
        String loginPage = RestAssured.given()
                .filter(session.cookies)
                .get(Config.baseUrl() + "/auth/login")
                .then().statusCode(200)
                .extract().asString();

        Matcher matcher = TOKEN.matcher(loginPage);
        if (!matcher.find()) {
            throw new IllegalStateException("Token CSRF não encontrado na página de login");
        }

        Response validate = RestAssured.given()
                .filter(session.cookies)
                .redirects().follow(false)
                .contentType(ContentType.URLENC)
                .formParam("_token", matcher.group(1))
                .formParam("username", username)
                .formParam("password", password)
                .post(Config.baseUrl() + "/auth/validate");

        String location = validate.getHeader("Location");
        if (location == null || location.contains("/auth/login")) {
            throw new IllegalStateException("Login falhou para o usuário " + username);
        }
        return session;
    }

    public static OrangeHrmSession admin() {
        return login(Config.adminUser(), Config.adminPassword());
    }

    /** Especificação base para chamadas em /api/v2, já com o cookie de sessão. */
    public RequestSpecification api() {
        return RestAssured.given(new RequestSpecBuilder()
                .setBaseUri(Config.baseUrl() + "/api/v2")
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(cookies)
                .log(LogDetail.METHOD)
                .log(LogDetail.URI)
                .build());
    }

    /** Requisição a uma página (não API) com o cookie de sessão, ex.: "/pim/addEmployee". */
    public String page(String path) {
        return RestAssured.given()
                .filter(cookies)
                .get(Config.baseUrl() + path)
                .then().statusCode(200)
                .extract().asString();
    }
}
