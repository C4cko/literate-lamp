package buzz;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/** Leitura da resposta de GET /api/v2/buzz/feed. */
public final class Feed {

    private Feed() {
    }

    /** Procura no feed o compartilhamento (share) cujo texto é exatamente o informado. */
    public static Optional<JsonNode> buscarPorTexto(JsonNode respostaFeed, String texto) {
        JsonNode lista = respostaFeed.path("data");
        for (JsonNode item : lista) {
            JsonNode t = item.path("text");
            if (!t.isNull() && texto.equals(t.asText())) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    public static int curtidas(JsonNode share) {
        return share.path("stats").path("numOfLikes").asInt();
    }

    public static int comentarios(JsonNode share) {
        return share.path("stats").path("numOfComments").asInt();
    }

    /** Aba "Most Liked Posts": cada post tem que ter curtidas maiores ou iguais às do próximo. */
    public static boolean ordenadoPorCurtidas(JsonNode respostaFeed) {
        JsonNode lista = respostaFeed.path("data");
        for (int i = 1; i < lista.size(); i++) {
            if (curtidas(lista.get(i - 1)) < curtidas(lista.get(i))) {
                return false;
            }
        }
        return true;
    }
}
