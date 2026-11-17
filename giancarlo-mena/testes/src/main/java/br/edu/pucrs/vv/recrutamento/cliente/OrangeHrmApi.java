package br.edu.pucrs.vv.recrutamento.cliente;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.pucrs.vv.recrutamento.modelo.Acao;
import br.edu.pucrs.vv.recrutamento.modelo.Candidato;
import br.edu.pucrs.vv.recrutamento.modelo.Entrevista;
import br.edu.pucrs.vv.recrutamento.modelo.Status;

/**
 * Cliente da API REST v2 do OrangeHRM usando só o {@link HttpClient} do Java 21.
 *
 * <p>A API usa o mesmo cookie de sessão do navegador. Por isso o login segue o fluxo da tela:
 * {@code GET /auth/login} para pegar o token CSRF e {@code POST /auth/validate} com usuário e senha.
 */
public final class OrangeHrmApi {

    private static final Duration TEMPO_LIMITE = Duration.ofSeconds(60);

    private final String url;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();

    private OrangeHrmApi(String url) {
        this.url = url;
        this.http = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(TEMPO_LIMITE)
                .build();
    }

    public static OrangeHrmApi entrarComoAdmin() {
        return entrar(Ambiente.url(), Ambiente.usuario(), Ambiente.senha());
    }

    public static OrangeHrmApi entrar(String url, String usuario, String senha) {
        OrangeHrmApi api = new OrangeHrmApi(url);
        api.login(usuario, senha);
        return api;
    }

    private void login(String usuario, String senha) {
        HttpResponse<String> pagina = enviar(HttpRequest.newBuilder(URI.create(url + "/auth/login")).GET());
        String token = TokenCsrf.extrair(pagina.body());

        String formulario = "_token=" + codificar(token)
                + "&username=" + codificar(usuario)
                + "&password=" + codificar(senha);
        HttpResponse<String> validacao = enviar(HttpRequest.newBuilder(URI.create(url + "/auth/validate"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(BodyPublishers.ofString(formulario)));

        String destino = validacao.headers().firstValue("Location").orElse("");
        if (validacao.statusCode() != 302 || destino.contains("/auth/login")) {
            throw new IllegalStateException("Login falhou para " + usuario + " (HTTP " + validacao.statusCode()
                    + ", Location=" + destino + ")");
        }
    }

    // ------------------------------------------------------------------ chamadas genéricas

    public Resposta get(String caminho) {
        return chamar("GET", caminho, null);
    }

    public Resposta post(String caminho, Object corpo) {
        return chamar("POST", caminho, corpo);
    }

    public Resposta put(String caminho, Object corpo) {
        return chamar("PUT", caminho, corpo);
    }

    public Resposta delete(String caminho, Object corpo) {
        return chamar("DELETE", caminho, corpo);
    }

    public Resposta chamar(String metodo, String caminho, Object corpo) {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url + "/api/v2" + caminho))
                .timeout(TEMPO_LIMITE)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .method(metodo, corpo == null ? BodyPublishers.noBody() : BodyPublishers.ofString(paraJson(corpo)));
        HttpResponse<String> r = enviar(req);
        return new Resposta(r.statusCode(), lerJson(r.body()), r.body());
    }

    // ------------------------------------------------------------------ dados de apoio

    /** Id do primeiro cargo cadastrado em Admin › Job Titles. */
    public int primeiroCargo() {
        Resposta r = exigir(get("/admin/job-titles?limit=1"));
        if (r.data().isEmpty()) {
            throw new IllegalStateException("Nenhum cargo (job title) cadastrado no servidor");
        }
        return r.data().get(0).get("id").asInt();
    }

    /** empNumber de até {@code quantidade} funcionários ativos. */
    public List<Integer> funcionarios(int quantidade) {
        Resposta r = exigir(get("/pim/employees?limit=" + quantidade + "&includeEmployees=onlyCurrent"));
        List<Integer> nums = new ArrayList<>();
        r.data().forEach(e -> nums.add(e.get("empNumber").asInt()));
        return nums;
    }

    /** Funcionários (ativos ou não) cujo nome ou id contém o texto. */
    public JsonNode buscarFuncionarios(String nomeOuId) {
        return exigir(get("/pim/employees?limit=50&includeEmployees=currentAndPast&nameOrId="
                + codificar(nomeOuId))).data();
    }

    public void apagarFuncionarios(List<Integer> empNumbers) {
        if (!empNumbers.isEmpty()) {
            delete("/pim/employees", Map.of("ids", empNumbers));
        }
    }

    // ------------------------------------------------------------------ vagas

    public long criarVaga(String nome, int cargoId, int gerenteEmpNumber) {
        return exigir(post("/recruitment/vacancies", corpoVaga(nome, cargoId, gerenteEmpNumber, true))).id();
    }

    public Resposta alterarStatusVaga(long vagaId, String nome, int cargoId, int gerenteEmpNumber, boolean ativa) {
        return put("/recruitment/vacancies/" + vagaId, corpoVaga(nome, cargoId, gerenteEmpNumber, ativa));
    }

    public void apagarVagas(List<Long> ids) {
        if (!ids.isEmpty()) {
            delete("/recruitment/vacancies", Map.of("ids", ids));
        }
    }

    private static Map<String, Object> corpoVaga(String nome, int cargoId, int gerente, boolean ativa) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("name", nome);
        corpo.put("description", "Vaga criada pelos testes automatizados de V&V");
        corpo.put("jobTitleId", cargoId);
        corpo.put("employeeId", gerente);
        corpo.put("numOfPositions", 1);
        corpo.put("status", ativa);
        corpo.put("isPublished", false);
        return corpo;
    }

    // ------------------------------------------------------------------ candidatos

    public Resposta cadastrar(Candidato candidato) {
        return post("/recruitment/candidates", candidato.comoCorpoApi());
    }

    public long cadastrarValido(Candidato candidato) {
        return exigir(cadastrar(candidato)).id();
    }

    public JsonNode candidato(long id) {
        return exigir(get("/recruitment/candidates/" + id)).data();
    }

    public Status status(long candidatoId) {
        JsonNode status = candidato(candidatoId).get("status");
        if (status == null || status.isNull()) {
            throw new IllegalStateException("Candidato " + candidatoId + " não está em nenhuma vaga");
        }
        return Status.porId(status.get("id").asInt());
    }

    public Set<Acao> acoesPermitidas(long candidatoId) {
        Resposta r = exigir(get("/recruitment/candidates/" + candidatoId + "/actions/allowed"));
        Set<Acao> acoes = new TreeSet<>();
        r.data().forEach(a -> {
            int id = a.get("id").asInt();
            if (id != 1) { // 1 = "Initiate Application", só aparece para candidato sem vaga
                acoes.add(Acao.porId(id));
            }
        });
        return acoes;
    }

    public JsonNode historico(long candidatoId) {
        return exigir(get("/recruitment/candidates/" + candidatoId + "/history?limit=50")).data();
    }

    /** Executa uma ação que não cria entrevista (shortlist, reject, pass, fail, offer, decline, hire). */
    public Resposta executar(Acao acao, long candidatoId, Long entrevistaId) {
        if (acao == Acao.AGENDAR_ENTREVISTA) {
            throw new IllegalArgumentException("Use agendar(...) para marcar entrevista");
        }
        return chamar(acao.metodo(), acao.caminho(candidatoId, entrevistaId),
                Map.of("note", "Executado pelos testes: " + acao.rotulo()));
    }

    public Resposta agendar(long candidatoId, Entrevista entrevista) {
        return post(Acao.AGENDAR_ENTREVISTA.caminho(candidatoId, null), entrevista.comoCorpoApi());
    }

    public void apagarCandidatos(List<Long> ids) {
        if (!ids.isEmpty()) {
            delete("/recruitment/candidates", Map.of("ids", ids));
        }
    }

    // ------------------------------------------------------------------ utilitários

    /** Falha com mensagem legível se a resposta não for 2xx. */
    public static Resposta exigir(Resposta r) {
        if (!r.sucesso()) {
            throw new IllegalStateException("Chamada à API falhou: " + r.resumo());
        }
        return r;
    }

    private HttpResponse<String> enviar(HttpRequest.Builder req) {
        try {
            return http.send(req.build(), BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Falha de comunicação com " + url + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Requisição interrompida", e);
        }
    }

    private String paraJson(Object corpo) {
        try {
            return json.writeValueAsString(corpo);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private JsonNode lerJson(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return json.readTree(texto);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
