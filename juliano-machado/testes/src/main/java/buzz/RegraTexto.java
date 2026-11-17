package buzz;

/**
 * Regras de tamanho do texto no Buzz, tiradas do código do OrangeHRM
 * (BuzzPostAPI e BuzzCommentAPI, plugin orangehrmBuzzPlugin).
 */
public final class RegraTexto {

    /** BuzzPostAPI::PARAM_RULE_TEXT_MAX_LENGTH */
    public static final int TAMANHO_MAXIMO = 65530;

    private RegraTexto() {
    }

    /** Post do tipo texto: obrigatório e com no máximo 65530 caracteres. */
    public static boolean postValido(String texto) {
        return texto != null && !texto.isEmpty() && tamanho(texto) <= TAMANHO_MAXIMO;
    }

    /**
     * Comentário: a tela não deixa enviar comentário vazio, então
     * a especificação considerada é a mesma do post.
     */
    public static boolean comentarioValido(String texto) {
        return postValido(texto);
    }

    // conta caracteres e não unidades UTF-16, igual ao mb_strlen do PHP
    static int tamanho(String texto) {
        return texto.codePointCount(0, texto.length());
    }
}
