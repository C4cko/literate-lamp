package br.edu.pucrs.vv.recrutamento.cliente;

/**
 * Configuração do ambiente sob teste. Cada valor pode vir de uma propriedade do sistema
 * ({@code -Dorangehrm.url=...}) ou de uma variável de ambiente ({@code ORANGEHRM_URL=...}).
 */
public final class Ambiente {

    public static final String URL_DEMO = "https://opensource-demo.orangehrmlive.com/web/index.php";

    private Ambiente() {
    }

    /** Endereço até {@code /web/index.php}, sem barra no final. */
    public static String url() {
        String url = valor("orangehrm.url", "ORANGEHRM_URL", URL_DEMO);
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public static String usuario() {
        return valor("orangehrm.usuario", "ORANGEHRM_USUARIO", "Admin");
    }

    public static String senha() {
        return valor("orangehrm.senha", "ORANGEHRM_SENHA", "admin123");
    }

    /** Mostra o navegador nos testes de sistema ({@code -Dnavegador.visivel=true}). */
    public static boolean navegadorVisivel() {
        return Boolean.parseBoolean(valor("navegador.visivel", "NAVEGADOR_VISIVEL", "false"));
    }

    private static String valor(String propriedade, String variavel, String padrao) {
        String v = System.getProperty(propriedade);
        if (v == null || v.isBlank()) {
            v = System.getenv(variavel);
        }
        return v == null || v.isBlank() ? padrao : v.trim();
    }
}
