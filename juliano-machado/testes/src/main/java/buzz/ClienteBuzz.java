package buzz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/** Cliente HTTP mínimo para a API REST v2 do Buzz. */
public class ClienteBuzz {

    public static final String URL_PADRAO = "https://opensource-demo.orangehrmlive.com/web/index.php";

    public record Resposta(int status, JsonNode corpo) {
    }

    private final String base;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();

    public ClienteBuzz(String base) {
        this.base = base;
        this.http = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public void login(String usuario, String senha) throws IOException, InterruptedException {
        String html = http.send(get("/auth/login"), HttpResponse.BodyHandlers.ofString()).body();
        String token = TokenCsrf.extrair(html);
        String form = "_token=" + enc(token) + "&username=" + enc(usuario) + "&password=" + enc(senha);
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + "/auth/validate"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
        String destino = r.headers().firstValue("location").orElse("");
        if (r.statusCode() != 302 || !destino.contains("dashboard")) {
            throw new IllegalStateException("login falhou: " + r.statusCode() + " " + destino);
        }
    }

    public Resposta criarPost(Map<String, Object> corpo) throws IOException, InterruptedException {
        return enviar("POST", "/api/v2/buzz/posts", corpo);
    }

    public Resposta criarPostTexto(String texto) throws IOException, InterruptedException {
        return criarPost(Map.of("type", "text", "text", texto));
    }

    public Resposta feed() throws IOException, InterruptedException {
        return enviar("GET", "/api/v2/buzz/feed?limit=50&offset=0&sortOrder=DESC&sortField=share.createdAtUtc", null);
    }

    public Resposta comentar(int shareId, String texto) throws IOException, InterruptedException {
        return enviar("POST", "/api/v2/buzz/shares/" + shareId + "/comments", Map.of("text", texto));
    }

    public Resposta comentarSemTexto(int shareId) throws IOException, InterruptedException {
        return enviar("POST", "/api/v2/buzz/shares/" + shareId + "/comments", Map.of());
    }

    public Resposta curtir(int shareId) throws IOException, InterruptedException {
        return enviar("POST", "/api/v2/buzz/shares/" + shareId + "/likes", null);
    }

    public Resposta descurtir(int shareId) throws IOException, InterruptedException {
        return enviar("DELETE", "/api/v2/buzz/shares/" + shareId + "/likes", null);
    }

    public Resposta curtirComentario(int commentId) throws IOException, InterruptedException {
        return enviar("POST", "/api/v2/buzz/comments/" + commentId + "/likes", null);
    }

    public Resposta comentarios(int shareId) throws IOException, InterruptedException {
        return enviar("GET", "/api/v2/buzz/shares/" + shareId + "/comments?model=detailed", null);
    }

    /** Mesma consulta da aba "Most Liked Posts". */
    public Resposta feedMaisCurtidos() throws IOException, InterruptedException {
        return enviar("GET", "/api/v2/buzz/feed?limit=50&offset=0&sortOrder=DESC&sortField=share.numOfLikes", null);
    }

    public Resposta excluir(int shareId) throws IOException, InterruptedException {
        return enviar("DELETE", "/api/v2/buzz/shares/" + shareId, null);
    }

    private Resposta enviar(String metodo, String caminho, Object corpo) throws IOException, InterruptedException {
        HttpRequest.BodyPublisher publisher = corpo == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(corpo));
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + caminho))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(60))
                .method(metodo, publisher)
                .build();
        HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode no = r.body().isBlank() ? json.nullNode() : json.readTree(r.body());
        return new Resposta(r.statusCode(), no);
    }

    private HttpRequest get(String caminho) {
        return HttpRequest.newBuilder(URI.create(base + caminho)).GET().build();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
