package t2.claim.support;

import java.time.Duration;

/**
 * Configuração dos testes de sistema. Cada valor pode ser trocado com -D (ex.: mvn test -Dheadless=true).
 * Os nomes de evento, tipo de despesa e moeda são os cadastrados no servidor de demonstração.
 */
public final class Config {
    public static final String BASE_URL =
            System.getProperty("ohrm.baseUrl", "https://opensource-demo.orangehrmlive.com/web/index.php");
    public static final String ADMIN_USER = System.getProperty("ohrm.user", "Admin");
    public static final String ADMIN_PASSWORD = System.getProperty("ohrm.password", "admin123");

    public static final String EVENT = System.getProperty("ohrm.event", "Travel Allowance");
    public static final String EXPENSE_TYPE = System.getProperty("ohrm.expenseType", "Transport");
    public static final String CURRENCY_ID = System.getProperty("ohrm.currencyId", "USD");
    public static final String CURRENCY_NAME = System.getProperty("ohrm.currencyName", "United States Dollar");

    public static final boolean HEADLESS = Boolean.parseBoolean(System.getProperty("headless", "false"));
    public static final Duration TIMEOUT = Duration.ofSeconds(Long.getLong("timeout", 30));

    private Config() {
    }
}
