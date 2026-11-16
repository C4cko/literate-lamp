package br.pucrs.verval.orangehrm.api.support;

import java.util.concurrent.atomic.AtomicInteger;

/** Gera dados únicos por execução, para não colidir com outros dados nem com execuções anteriores. */
public final class TestData {

    private static final String RUN = Long.toString(System.currentTimeMillis() % 1_000_000, 36);
    private static final AtomicInteger SEQ = new AtomicInteger();

    private TestData() {
    }

    /** Sufixo curto e único, ex.: "k3f9a1". */
    public static String unique() {
        return RUN + SEQ.incrementAndGet();
    }

    /** Employee Id único com até 10 caracteres (limite do sistema). */
    public static String employeeId() {
        String id = "T" + unique();
        return id.length() > 10 ? id.substring(id.length() - 10) : id;
    }

    /** Username único com 5 a 40 caracteres (limite do sistema). */
    public static String username() {
        return "t2u" + unique();
    }

    /** Senha que cumpre a política padrão e tem força "strong" (9 caracteres; ver defeito D2). */
    public static final String STRONG_PASSWORD = "t7#Rw!9Zp";

    /** String com exatamente {@code length} caracteres, útil para valor-limite. */
    public static String ofLength(int length) {
        return "a".repeat(length);
    }
}
