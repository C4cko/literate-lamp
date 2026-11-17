package br.edu.pucrs.vv.recrutamento.modelo;

public class TransicaoInvalidaException extends RuntimeException {

    public TransicaoInvalidaException(Status estado, Acao acao) {
        super("Ação " + acao + " não é permitida no status " + estado);
    }
}
