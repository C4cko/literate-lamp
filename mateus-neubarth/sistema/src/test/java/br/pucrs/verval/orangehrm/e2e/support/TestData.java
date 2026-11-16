package br.pucrs.verval.orangehrm.e2e.support;

import java.util.concurrent.atomic.AtomicInteger;

/** Gera dados únicos por execução, para não colidir com dados de outras execuções. */
public final class TestData {

    private static final String RUN = Long.toString(System.currentTimeMillis() % 1_000_000, 36);
    private static final AtomicInteger SEQ = new AtomicInteger();

    private TestData() {
    }

    public static String unique() {
        return RUN + SEQ.incrementAndGet();
    }

    public static String username() {
        return "t2e" + unique();
    }

    public static String employeeId() {
        String id = "E" + unique();
        return id.length() > 10 ? id.substring(id.length() - 10) : id;
    }

    /** Senha que cumpre a política padrão e tem força "strong" (9 caracteres; ver defeito D2). */
    public static final String STRONG_PASSWORD = "t7#Rw!9Zp";

    public static String ofLength(int length) {
        return "a".repeat(length);
    }
}
