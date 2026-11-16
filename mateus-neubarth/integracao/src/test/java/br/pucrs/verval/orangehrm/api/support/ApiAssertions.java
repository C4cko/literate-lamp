package br.pucrs.verval.orangehrm.api.support;

import io.restassured.response.Response;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Asserções sobre o formato de erro da API REST v2 do OrangeHRM. */
public final class ApiAssertions {

    private ApiAssertions() {
    }

    /** Validação recusada (HTTP 422) exatamente nos parâmetros informados. */
    public static void assertInvalidParams(Response response, String... params) {
        assertThat(response.statusCode()).as("status HTTP").isEqualTo(422);
        List<String> invalid = response.path("error.data.invalidParamKeys");
        assertThat(invalid).as("invalidParamKeys").containsExactlyInAnyOrder(params);
    }
}
