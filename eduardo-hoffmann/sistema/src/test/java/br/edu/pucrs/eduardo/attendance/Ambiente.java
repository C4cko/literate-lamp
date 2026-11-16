package br.edu.pucrs.eduardo.attendance;

/**
 * Configuração do ambiente dos testes de sistema. Os valores padrão são os da instância local criada por
 * eduardo-hoffmann/ambiente (docker compose, serviço "app"); cada um pode ser trocado com -D na linha do Maven.
 *
 * A credencial padrão é a do arquivo ambiente/config/cli_install_app.yaml, que só existe nesse ambiente local.
 * Os testes gravam dados e mudam a configuração de Attendance, por isso NÃO devem apontar para o servidor de
 * demonstração compartilhado.
 */
final class Ambiente {

    static final String URL = propriedade("orangehrm.url", "http://localhost:8201");
    static final String USUARIO = propriedade("orangehrm.user", "Admin");
    static final String SENHA = propriedade("orangehrm.password", "Admin@OHRM123");
    static final boolean SEM_JANELA = Boolean.parseBoolean(propriedade("headless", "true"));

    /** Banco da instância local, usado só para preparar e limpar a massa. */
    static final String BANCO_URL = propriedade("orangehrm.db.url", "jdbc:mariadb://127.0.0.1:3308/orangehrm_app");
    static final String BANCO_USUARIO = propriedade("orangehrm.db.user", "root");
    static final String BANCO_SENHA = propriedade("orangehrm.db.password", "root");

    private Ambiente() {
    }

    private static String propriedade(String nome, String padrao) {
        String valor = System.getProperty(nome);
        return (valor == null || valor.isBlank()) ? padrao : valor;
    }
}
