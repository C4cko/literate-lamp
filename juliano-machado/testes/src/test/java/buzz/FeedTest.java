package buzz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TU-04, TU-05 e TU-07: leitura do feed com uma resposta fixa (sem servidor). */
class FeedTest {

    // resposta no mesmo formato que o servidor devolve, reduzida
    private static final String FEED = """
            {"data":[
              {"id":40,"type":"post","text":null,"stats":{"numOfLikes":0,"numOfComments":0}},
              {"id":41,"type":"post","text":"JM outro","stats":{"numOfLikes":2,"numOfComments":0}},
              {"id":42,"type":"post","text":"JM teste","stats":{"numOfLikes":1,"numOfComments":3}}
            ],"meta":{"total":3}}
            """;

    private final JsonNode feed = lerJson(FEED);

    // TU-04: acha pelo texto exato e lê os contadores
    @DisplayName("TU-04 acha pelo texto exato e lê os contadores")
    @Test
    void encontraPostPeloTexto() {
        Optional<JsonNode> share = Feed.buscarPorTexto(feed, "JM teste");
        assertTrue(share.isPresent());
        assertEquals(42, share.get().path("id").asInt());
        assertEquals(1, Feed.curtidas(share.get()));
        assertEquals(3, Feed.comentarios(share.get()));
    }

    // TU-05: texto inexistente, texto parcial e post sem texto (foto) não dão falso positivo
    @DisplayName("TU-05 texto inexistente, texto parcial e post sem texto (foto) não dão falso positivo")
    @Test
    void naoEncontraTextoInexistenteOuParcial() {
        assertTrue(Feed.buscarPorTexto(feed, "nao existe").isEmpty());
        assertTrue(Feed.buscarPorTexto(feed, "JM").isEmpty());
        assertTrue(Feed.buscarPorTexto(lerJson("{\"data\":[]}"), "JM teste").isEmpty());
    }

    // TU-07: ordem da aba Most Liked (decrescente, com empate, vazia e fora de ordem)
    @Test
    @DisplayName("TU-07 verifica a ordem por curtidas da aba Most Liked")
    void ordemPorCurtidas() {
        assertTrue(Feed.ordenadoPorCurtidas(feedComCurtidas(3, 1, 0)));
        assertTrue(Feed.ordenadoPorCurtidas(feedComCurtidas(2, 2, 0)));
        assertTrue(Feed.ordenadoPorCurtidas(feedComCurtidas()));
        assertTrue(Feed.ordenadoPorCurtidas(feedComCurtidas(5)));
        assertFalse(Feed.ordenadoPorCurtidas(feedComCurtidas(1, 0, 2)));
        assertFalse(Feed.ordenadoPorCurtidas(feedComCurtidas(0, 1)));
    }

    private static JsonNode feedComCurtidas(int... curtidas) {
        StringBuilder sb = new StringBuilder("{\"data\":[");
        for (int i = 0; i < curtidas.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"id\":").append(i).append(",\"stats\":{\"numOfLikes\":").append(curtidas[i]).append("}}");
        }
        return lerJson(sb.append("]}").toString());
    }

    private static JsonNode lerJson(String s) {
        try {
            return new ObjectMapper().readTree(s);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
