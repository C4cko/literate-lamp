package br.edu.pucrs.vv.recrutamento.cliente;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A página {@code /auth/login} entrega o token CSRF como propriedade do componente Vue, por exemplo
 * {@code <auth-login :token="&quot;abc.def&quot;" ...>} (ver {@code LoginController::handle}).
 * O formulário de login precisa reenviar esse valor no campo {@code _token}.
 */
public final class TokenCsrf {

    private static final Pattern ATRIBUTO = Pattern.compile(":token=\"([^\"]*)\"");

    private TokenCsrf() {
    }

    public static String extrair(String html) {
        if (html == null) {
            throw new IllegalStateException("Página de login vazia");
        }
        Matcher m = ATRIBUTO.matcher(html);
        if (!m.find()) {
            throw new IllegalStateException("Token CSRF não encontrado na página de login");
        }
        // o valor passa por json_encode (Prop::getValue) e depois pelo escape HTML do Twig
        String valor = m.group(1).replace("&quot;", "\"").replace("&amp;", "&").replace("\\/", "/").trim();
        if (valor.length() >= 2 && valor.startsWith("\"") && valor.endsWith("\"")) {
            valor = valor.substring(1, valor.length() - 1);
        }
        if (valor.isEmpty()) {
            throw new IllegalStateException("Token CSRF vazio na página de login");
        }
        return valor;
    }
}
