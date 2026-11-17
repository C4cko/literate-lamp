package br.edu.pucrs.vv.recrutamento.integracao;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import br.edu.pucrs.vv.recrutamento.cliente.OrangeHrmApi;
import br.edu.pucrs.vv.recrutamento.cliente.Resposta;
import br.edu.pucrs.vv.recrutamento.modelo.Acao;
import br.edu.pucrs.vv.recrutamento.modelo.Candidato;
import br.edu.pucrs.vv.recrutamento.modelo.Entrevista;
import br.edu.pucrs.vv.recrutamento.modelo.FluxoRecrutamento;
import br.edu.pucrs.vv.recrutamento.modelo.Status;

/**
 * Base dos testes de integração: abre a sessão de Admin, cria uma vaga só para a classe de teste e,
 * no final, apaga candidatos, vaga e funcionários criados pela contratação.
 */
abstract class CenarioRecrutamento {

    protected static OrangeHrmApi api;
    protected static int cargo;
    protected static List<Integer> funcionarios;
    protected static String nomeVaga;
    protected static long vaga;

    private static final List<Long> candidatosCriados = new ArrayList<>();
    private static final List<Long> vagasCriadas = new ArrayList<>();
    private static final List<String> sobrenomesContratados = new ArrayList<>();

    /** Candidato criado pelo teste e o que já aconteceu com ele. */
    protected record CandidatoEmTeste(long id, String sobrenome, Long ultimaEntrevista, int entrevistas) {
    }

    @BeforeAll
    static void abrirCenario() {
        api = OrangeHrmApi.entrarComoAdmin();
        cargo = api.primeiroCargo();
        funcionarios = api.funcionarios(5);
        if (funcionarios.isEmpty()) {
            throw new IllegalStateException("O servidor não tem funcionários para usar como gerente/entrevistador");
        }
        nomeVaga = "Vaga VV " + sufixo();
        vaga = api.criarVaga(nomeVaga, cargo, gerente());
        vagasCriadas.add(vaga);
    }

    @AfterAll
    static void limparCenario() {
        if (api == null) {
            return;
        }
        api.apagarCandidatos(List.copyOf(candidatosCriados));
        api.apagarVagas(List.copyOf(vagasCriadas));
        List<Integer> contratados = new ArrayList<>();
        for (String sobrenome : sobrenomesContratados) {
            api.buscarFuncionarios(sobrenome).forEach(f -> contratados.add(f.get("empNumber").asInt()));
        }
        api.apagarFuncionarios(contratados);
        candidatosCriados.clear();
        vagasCriadas.clear();
        sobrenomesContratados.clear();
    }

    /** Sufixo único: VV + data/hora + 3 dígitos aleatórios. */
    protected static String sufixo() {
        return "VV" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    protected static int gerente() {
        return funcionarios.get(0);
    }

    protected static void registrarCandidato(long id) {
        candidatosCriados.add(id);
    }

    protected static void registrarVaga(long id) {
        vagasCriadas.add(id);
    }

    protected static void registrarContratacao(String sobrenome) {
        sobrenomesContratados.add(sobrenome);
    }

    /** Candidato válido e único, ainda não enviado. */
    protected static Candidato novoCandidato() {
        return Candidato.valido(sufixo()).naVaga(vaga);
    }

    /** Cadastra um candidato válido na vaga do cenário. */
    protected static CandidatoEmTeste cadastrarNaVaga() {
        return cadastrarNaVaga(vaga);
    }

    protected static CandidatoEmTeste cadastrarNaVaga(long vagaId) {
        Candidato c = Candidato.valido(sufixo()).naVaga(vagaId);
        long id = api.cadastrarValido(c);
        registrarCandidato(id);
        return new CandidatoEmTeste(id, c.sobrenome(), null, 0);
    }

    protected static Entrevista entrevistaPadrao() {
        return Entrevista.valida(List.of(funcionarios.get(0)));
    }

    /**
     * Executa uma ação pela API e devolve o candidato atualizado. Falha se a API recusar, porque aqui
     * a ação é usada só para preparar o cenário.
     */
    protected static CandidatoEmTeste avancar(CandidatoEmTeste c, Acao acao) {
        if (acao == Acao.AGENDAR_ENTREVISTA) {
            Resposta r = OrangeHrmApi.exigir(api.agendar(c.id(), entrevistaPadrao()));
            return new CandidatoEmTeste(c.id(), c.sobrenome(), r.id(), c.entrevistas() + 1);
        }
        OrangeHrmApi.exigir(api.executar(acao, c.id(), c.ultimaEntrevista()));
        if (acao == Acao.CONTRATAR) {
            registrarContratacao(c.sobrenome());
        }
        return c;
    }

    /** Caminho mais curto, pela máquina de estados, para deixar um candidato novo no status pedido. */
    protected static CandidatoEmTeste candidatoNoStatus(Status alvo) {
        CandidatoEmTeste c = cadastrarNaVaga();
        for (Acao acao : caminhoAte(alvo)) {
            c = avancar(c, acao);
        }
        return c;
    }

    static List<Acao> caminhoAte(Status alvo) {
        List<Acao> passos = switch (alvo) {
            case APPLICATION_INITIATED -> List.of();
            case SHORTLISTED -> List.of(Acao.SHORTLIST);
            case REJECTED -> List.of(Acao.REJEITAR);
            case INTERVIEW_SCHEDULED -> List.of(Acao.SHORTLIST, Acao.AGENDAR_ENTREVISTA);
            case INTERVIEW_PASSED -> List.of(Acao.SHORTLIST, Acao.AGENDAR_ENTREVISTA, Acao.APROVAR_ENTREVISTA);
            case INTERVIEW_FAILED -> List.of(Acao.SHORTLIST, Acao.AGENDAR_ENTREVISTA, Acao.REPROVAR_ENTREVISTA);
            case JOB_OFFERED -> List.of(Acao.SHORTLIST, Acao.AGENDAR_ENTREVISTA, Acao.APROVAR_ENTREVISTA,
                    Acao.OFERECER_VAGA);
            case OFFER_DECLINED -> List.of(Acao.SHORTLIST, Acao.AGENDAR_ENTREVISTA, Acao.APROVAR_ENTREVISTA,
                    Acao.OFERECER_VAGA, Acao.RECUSAR_OFERTA);
            case HIRED -> List.of(Acao.SHORTLIST, Acao.AGENDAR_ENTREVISTA, Acao.APROVAR_ENTREVISTA,
                    Acao.OFERECER_VAGA, Acao.CONTRATAR);
        };
        // confere o caminho no próprio modelo, para não preparar cenário impossível
        Status s = Status.APPLICATION_INITIATED;
        int entrevistas = 0;
        for (Acao a : passos) {
            s = FluxoRecrutamento.aplicar(s, a, entrevistas);
            if (a == Acao.AGENDAR_ENTREVISTA) {
                entrevistas++;
            }
        }
        if (s != alvo) {
            throw new IllegalStateException("Caminho errado até " + alvo);
        }
        return passos;
    }
}
