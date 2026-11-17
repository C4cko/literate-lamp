package buzz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TU-04 e TU-05: leitura do feed com uma resposta fixa (sem servidor). */
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

    private static JsonNode lerJson(String s) {
        try {
            return new ObjectMapper().readTree(s);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
