package buzz;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A tela de login do OrangeHRM traz o token CSRF dentro do componente Vue:
 * {@code <auth-login ... :token="&quot;abc&quot;">}. O POST de login precisa dele.
 */
public final class TokenCsrf {

    private static final Pattern PADRAO = Pattern.compile(":token=\"&quot;([^&\"]+)&quot;\"");

    private TokenCsrf() {
    }

    public static String extrair(String html) {
        if (html == null) {
            throw new IllegalArgumentException("html nulo");
        }
        Matcher m = PADRAO.matcher(html);
        if (!m.find()) {
            throw new IllegalArgumentException("token não encontrado na página de login");
        }
        return m.group(1);
    }
}
