package br.edu.pucrs.eduardo.attendance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Acesso ao banco da instância LOCAL, só para três coisas: deixar cada teste começar sem registros e com a
 * opção "Employee can change current time" desligada, semear um registro concluído (ST-05) e conferir o que a
 * tela gravou. A jornada em si é sempre executada pela interface.
 */
final class BancoDeTeste {

    private BancoDeTeste() {
    }

    private static Connection conectar() throws SQLException {
        return DriverManager.getConnection(Ambiente.BANCO_URL, Ambiente.BANCO_USUARIO, Ambiente.BANCO_SENHA);
    }

    /** Apaga todos os registros de ponto e desliga a opção de alterar a hora (linhas ESS / ação 8 do fluxo 1). */
    static void restaurarEstadoInicial() {
        try (Connection c = conectar()) {
            executar(c, "DELETE FROM ohrm_attendance_record");
            executar(c, "DELETE FROM ohrm_workflow_state_machine WHERE workflow = '1' AND role = 'ESS USER' AND action = '8'");
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível restaurar o estado inicial do banco local", e);
        }
    }

    static int contarRegistros() {
        return contar("SELECT COUNT(*) FROM ohrm_attendance_record");
    }

    static int contarRegistrosComEstado(String estado) {
        try (Connection c = conectar();
             PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM ohrm_attendance_record WHERE state = ?")) {
            p.setString(1, estado);
            try (ResultSet r = p.executeQuery()) {
                r.next();
                return r.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Hora de entrada do usuário (hora de parede) do único registro existente, no formato yyyy-MM-dd HH:mm:ss. */
    static String horaDeEntradaDoUsuario() {
        return texto("SELECT DATE_FORMAT(punch_in_user_time, '%Y-%m-%d %H:%i:%s') FROM ohrm_attendance_record ORDER BY id LIMIT 1");
    }

    /**
     * Semeia um registro concluído do Admin, de {@code entrada} a {@code saida} (horas de parede no fuso do
     * navegador, que é o do sistema), gravado em UTC como o OrangeHRM faz.
     */
    static void semearRegistroConcluido(LocalDateTime entrada, LocalDateTime saida) {
        ZoneId fuso = ZoneId.systemDefault();
        LocalDateTime entradaUtc = entrada.atZone(fuso).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime saidaUtc = saida.atZone(fuso).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        double offsetHoras = fuso.getRules().getOffset(entrada.atZone(fuso).toInstant()).getTotalSeconds() / 3600.0;

        String sql = "INSERT INTO ohrm_attendance_record (employee_id, punch_in_utc_time, punch_in_note, "
                + "punch_in_time_offset, punch_in_user_time, punch_in_timezone_name, punch_out_utc_time, "
                + "punch_out_note, punch_out_time_offset, punch_out_user_time, punch_out_timezone_name, state) "
                + "VALUES (?, ?, 'semeado', ?, ?, ?, ?, 'semeado', ?, ?, ?, 'PUNCHED OUT')";
        try (Connection c = conectar(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, empNumberDoAdmin());
            p.setObject(2, entradaUtc);
            p.setString(3, String.valueOf(offsetHoras));
            p.setObject(4, entrada);
            p.setString(5, fuso.getId());
            p.setObject(6, saidaUtc);
            p.setString(7, String.valueOf(offsetHoras));
            p.setObject(8, saida);
            p.setString(9, fuso.getId());
            p.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível semear o registro concluído", e);
        }
    }

    private static int empNumberDoAdmin() {
        return contar("SELECT emp_number FROM ohrm_user WHERE user_name = '" + Ambiente.USUARIO.replace("'", "") + "'");
    }

    private static void executar(Connection c, String sql) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.executeUpdate();
        }
    }

    private static int contar(String sql) {
        try (Connection c = conectar(); PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            r.next();
            return r.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String texto(String sql) {
        try (Connection c = conectar(); PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            return r.next() ? r.getString(1) : null;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
