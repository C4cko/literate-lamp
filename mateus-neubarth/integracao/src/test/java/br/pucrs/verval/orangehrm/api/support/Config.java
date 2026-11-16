package br.pucrs.verval.orangehrm.api.support;

/**
 * Configuração do ambiente sob teste. Cada valor pode ser sobrescrito por propriedade de sistema
 * (-Dohrm.baseUrl=...) ou variável de ambiente (OHRM_BASE_URL=...).
 * O padrão aponta para o ambiente local definido em ../ambiente/docker-compose.yml.
 */
public final class Config {

    private Config() {
    }

    /** URL até o index.php, sem barra no fim. Ex.: https://opensource-demo.orangehrmlive.com/web/index.php */
    public static String baseUrl() {
        return get("ohrm.baseUrl", "OHRM_BASE_URL", "http://localhost:8200/web/index.php");
    }

    public static String adminUser() {
        return get("ohrm.user", "OHRM_USER", "Admin");
    }

    public static String adminPassword() {
        return get("ohrm.password", "OHRM_PASSWORD", "Admin@OHRM123");
    }

    private static String get(String property, String env, String defaultValue) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(env);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
