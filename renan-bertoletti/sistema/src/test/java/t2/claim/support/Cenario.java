package t2.claim.support;

import java.util.List;

/**
 * Dados próprios de uma classe de teste, criados pela API com um sufixo único: um funcionário (ESS), o supervisor
 * dele e os dois usuários, além do vínculo Report-to. Nenhum teste usa dados de outras pessoas do servidor
 * compartilhado (crítica a P2 e P3 na análise #5).
 */
public final class Cenario {
    public static final String SENHA = "Vv#Teste2026";

    public final String sufixo;
    public final OrangeApi admin;
    public final int essEmp;
    public final int supEmp;
    public final String essNome;
    public final String essUser;
    public final String supUser;
    public final int eventId;
    public final int expenseTypeId;

    private Cenario() {
        sufixo = Long.toString(System.currentTimeMillis(), 36);
        admin = OrangeApi.login(Config.ADMIN_USER, Config.ADMIN_PASSWORD);
        essNome = "VVEss " + sufixo;
        essEmp = admin.createEmployee("VVEss", sufixo);
        supEmp = admin.createEmployee("VVSup", sufixo);
        essUser = "vvess" + sufixo;
        supUser = "vvsup" + sufixo;
        admin.createEssUser(essUser, SENHA, essEmp);
        admin.createEssUser(supUser, SENHA, supEmp);
        admin.assignSupervisor(essEmp, supEmp);
        eventId = admin.idByName("claim/events", Config.EVENT);
        expenseTypeId = admin.idByName("claim/expenses/types", Config.EXPENSE_TYPE);
    }

    public static Cenario criar() {
        return new Cenario();
    }

    public OrangeApi ess() {
        return OrangeApi.login(essUser, SENHA);
    }

    public OrangeApi supervisor() {
        return OrangeApi.login(supUser, SENHA);
    }

    /** Reivindicação do ESS, criada por ele, com uma despesa de 10,50 e já levada ao estado pedido. */
    public int reivindicacaoDoEss(String... acoes) {
        OrangeApi ess = ess();
        int id = ess.createOwnClaim(eventId, "VV " + sufixo);
        ess.addExpense(id, expenseTypeId, 10.5);
        for (String acao : acoes) {
            (acao.equals("SUBMIT") || acao.equals("CANCEL") ? ess : supervisor()).action(id, acao);
        }
        return id;
    }

    /**
     * Remove os funcionários criados (e, com eles, os usuários). As reivindicações não têm exclusão na API.
     * Faz login de novo: a sessão aberta no início pode ter expirado durante o teste.
     */
    public void remover() {
        OrangeApi.login(Config.ADMIN_USER, Config.ADMIN_PASSWORD).deleteEmployees(List.of(essEmp, supEmp));
    }
}
