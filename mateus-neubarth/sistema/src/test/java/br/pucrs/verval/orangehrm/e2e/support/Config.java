package br.pucrs.verval.orangehrm.e2e.support;

/**
 * Configuração do ambiente sob teste. Sobrescreva com -Dohrm.baseUrl=... ou OHRM_BASE_URL=...
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

    /** -Dheadless=false abre o navegador visível (útil para gravar o vídeo). */
    public static boolean headless() {
        return Boolean.parseBoolean(get("headless", "HEADLESS", "true"));
    }

    /** Atraso entre ações em ms (-DslowMo=500), para demonstrações. */
    public static double slowMo() {
        return Double.parseDouble(get("slowMo", "SLOW_MO", "0"));
    }

    private static String get(String property, String env, String defaultValue) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(env);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
