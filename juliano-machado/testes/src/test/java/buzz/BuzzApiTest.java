package buzz;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TI-01 a TI-13: integração com a API REST v2 do Buzz no servidor de demonstração.
 * Cada teste cria os próprios posts com o prefixo "JM-" e um número único,
 * e todos são excluídos no final, para não depender nem interferir em dados de outras pessoas.
 */
@Tag("integracao")
class BuzzApiTest {

    private static ClienteBuzz api;
    private final List<Integer> criados = new ArrayList<>();

    @BeforeAll
    static void entrar() throws Exception {
        String url = System.getProperty("orangehrm.url", ClienteBuzz.URL_PADRAO);
        api = new ClienteBuzz(url);
        api.login("Admin", "admin123");
    }

    @AfterEach
    void limpar() throws Exception {
        for (int id : criados) {
            api.excluir(id);
        }
    }

    // TI-01: post de texto válido aparece no feed com contadores zerados
    @DisplayName("TI-01 post de texto válido aparece no feed com contadores zerados")
    @Test
    void criarPostDeTexto() throws Exception {
        String texto = textoUnico("TI01");
        assertEquals(200, api.criarPostTexto(texto).status());

        JsonNode share = buscarNoFeed(texto);
        assertEquals(0, Feed.curtidas(share));
        assertEquals(0, Feed.comentarios(share));
    }

    // TI-02: texto vazio é recusado (partição inválida)
    @DisplayName("TI-02 texto vazio é recusado (partição inválida)")
    @Test
    void postComTextoVazio() throws Exception {
        ClienteBuzz.Resposta r = api.criarPostTexto("");
        assertEquals(422, r.status());
        assertEquals("text", r.corpo().path("error").path("data").path("invalidParamKeys").path(0).asText());
    }

    // TI-03: tipo de post fora de {text, photo, video} é recusado
    @DisplayName("TI-03 tipo de post fora de {text, photo, video} é recusado")
    @Test
    void postComTipoInvalido() throws Exception {
        ClienteBuzz.Resposta r = api.criarPost(Map.of("type", "audio", "text", textoUnico("TI03")));
        assertEquals(422, r.status());
    }

    // TI-04: limite superior aceito (65530 caracteres)
    @DisplayName("TI-04 limite superior aceito (65530 caracteres)")
    @Test
    void postNoLimiteMaximo() throws Exception {
        String inicio = textoUnico("TI04") + " ";
        String texto = inicio + "a".repeat(RegraTexto.TAMANHO_MAXIMO - inicio.length());
        assertEquals(RegraTexto.TAMANHO_MAXIMO, texto.length());

        assertEquals(200, api.criarPostTexto(texto).status());
        buscarNoFeed(texto);
    }

    // TI-05: um caractere acima do limite (65531) é recusado
    @DisplayName("TI-05 um caractere acima do limite (65531) é recusado")
    @Test
    void postAcimaDoLimite() throws Exception {
        String inicio = textoUnico("TI05") + " ";
        String texto = inicio + "a".repeat(RegraTexto.TAMANHO_MAXIMO + 1 - inicio.length());

        ClienteBuzz.Resposta r = api.criarPostTexto(texto);
        assertEquals(422, r.status());
        assertTrue(Feed.buscarPorTexto(api.feed().corpo(), texto).isEmpty());
    }

    // TI-06: comentar atualiza o contador de comentários
    @DisplayName("TI-06 comentar atualiza o contador de comentários")
    @Test
    void comentarNoPost() throws Exception {
        int id = criarPost("TI06");
        assertEquals(200, api.comentar(id, "comentário do teste TI06").status());

        assertEquals(1, Feed.comentarios(buscarNoFeedPorId(id)));
    }

    // TI-07: curtir atualiza o contador; curtir de novo é recusado
    @DisplayName("TI-07 curtir atualiza o contador; curtir de novo é recusado")
    @Test
    void curtirDuasVezes() throws Exception {
        int id = criarPost("TI07");
        assertEquals(200, api.curtir(id).status());

        ClienteBuzz.Resposta segunda = api.curtir(id);
        assertEquals(400, segunda.status());
        assertEquals("Already liked", segunda.corpo().path("error").path("message").asText());
        assertEquals(1, Feed.curtidas(buscarNoFeedPorId(id)));
    }

    // TI-08: excluir tira o post do feed
    @DisplayName("TI-08 excluir tira o post do feed")
    @Test
    void excluirPost() throws Exception {
        String texto = textoUnico("TI08");
        api.criarPostTexto(texto);
        int id = buscarNoFeed(texto).path("id").asInt();
        criados.remove(Integer.valueOf(id));

        assertEquals(200, api.excluir(id).status());
        assertTrue(Feed.buscarPorTexto(api.feed().corpo(), texto).isEmpty());
    }

    // TI-09: comentário sem o campo text é recusado
    @DisplayName("TI-09 comentário sem o campo text é recusado")
    @Test
    void comentarioSemCampoTexto() throws Exception {
        int id = criarPost("TI09");
        ClienteBuzz.Resposta r = api.comentarSemTexto(id);
        assertEquals(422, r.status());
        assertEquals(0, Feed.comentarios(buscarNoFeedPorId(id)));
    }

    // TI-10: comentário com texto vazio deveria ser recusado como na tela.
    @DisplayName("TI-10 comentário com texto vazio deveria ser recusado como na tela")
    // Falha no servidor atual (devolve 200): defeito D1, por isso fica com a tag "defeito".
    @Test
    @Tag("defeito")
    void comentarioComTextoVazio() throws Exception {
        int id = criarPost("TI10");
        ClienteBuzz.Resposta r = api.comentar(id, "");
        assertFalse(RegraTexto.comentarioValido(""));
        assertEquals(422, r.status(), "a API aceitou comentário vazio");
    }

    // TI-11: descurtir volta o contador para 0; descurtir de novo é recusado
    @Test
    @DisplayName("TI-11 descurtir volta o contador; descurtir de novo é recusado")
    void descurtir() throws Exception {
        int id = criarPost("TI11");
        assertEquals(200, api.curtir(id).status());
        assertEquals(200, api.descurtir(id).status());

        JsonNode share = buscarNoFeedPorId(id);
        assertEquals(0, Feed.curtidas(share));
        assertFalse(share.path("liked").asBoolean());

        ClienteBuzz.Resposta segunda = api.descurtir(id);
        assertEquals(400, segunda.status());
        assertEquals("Not previously liked", segunda.corpo().path("error").path("message").asText());
    }

    // TI-12: curtir um comentário; curtir o mesmo comentário de novo é recusado
    @Test
    @DisplayName("TI-12 curtir comentário; curtir de novo é recusado")
    void curtirComentario() throws Exception {
        int id = criarPost("TI12");
        int comentario = api.comentar(id, "comentário do teste TI12").corpo()
                .path("data").path("comment").path("id").asInt();

        assertEquals(200, api.curtirComentario(comentario).status());
        assertEquals(400, api.curtirComentario(comentario).status());

        JsonNode c = api.comentarios(id).corpo().path("data").path(0).path("comment");
        assertEquals(comentario, c.path("id").asInt());
        assertEquals(1, c.path("numOfLikes").asInt());
        assertTrue(c.path("liked").asBoolean());
    }

    // TI-13: aba Most Liked: feed em ordem decrescente de curtidas, post curtido antes do não curtido
    @Test
    @DisplayName("TI-13 Most Liked ordena por curtidas")
    void maisCurtidos() throws Exception {
        int semCurtida = criarPost("TI13a");
        int comCurtida = criarPost("TI13b");
        api.curtir(comCurtida);

        JsonNode feed = api.feedMaisCurtidos().corpo();
        assertTrue(Feed.ordenadoPorCurtidas(feed));

        int posCom = -1;
        int posSem = -1;
        for (int i = 0; i < feed.path("data").size(); i++) {
            int atual = feed.path("data").get(i).path("id").asInt();
            if (atual == comCurtida) {
                posCom = i;
            } else if (atual == semCurtida) {
                posSem = i;
            }
        }
        assertTrue(posCom >= 0, "post curtido não está entre os 50 mais curtidos");
        assertTrue(posSem < 0 || posCom < posSem, "post curtido deveria vir antes do não curtido");
    }

    // ---------------------------------------------------------------

    private int criarPost(String caso) throws Exception {
        String texto = textoUnico(caso);
        assertEquals(200, api.criarPostTexto(texto).status());
        return buscarNoFeed(texto).path("id").asInt();
    }

    private JsonNode buscarNoFeed(String texto) throws Exception {
        JsonNode share = Feed.buscarPorTexto(api.feed().corpo(), texto)
                .orElseThrow(() -> new AssertionError("post não apareceu no feed: " + texto.substring(0, 20)));
        int id = share.path("id").asInt();
        if (!criados.contains(id)) {
            criados.add(id);
        }
        return share;
    }

    private JsonNode buscarNoFeedPorId(int id) throws Exception {
        for (JsonNode item : api.feed().corpo().path("data")) {
            if (item.path("id").asInt() == id) {
                return item;
            }
        }
        throw new AssertionError("share " + id + " não está no feed");
    }

    private static String textoUnico(String caso) {
        return "JM-" + caso + "-" + System.nanoTime();
    }
}
