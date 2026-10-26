package org.example.banco;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BancoDados {

    

    private static final String URL = "jdbc:sqlite:chat.db";

     public static Connection conectar() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
        }
        return conn;
    }


    public static void inicializar() {
            
            String sqlUsuarios = """
                CREATE TABLE IF NOT EXISTS usuarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    apelido TEXT UNIQUE NOT NULL,
                    email TEXT,
                    data_criacao DATETIME DEFAULT CURRENT_TIMESTAMP
                );
            """;
    
            String sqlMensagens = """
                CREATE TABLE IF NOT EXISTS mensagens (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    remetente TEXT NOT NULL,
                    destinatario TEXT, -- se for nulo, é mensagem pública para a sala
                    sala TEXT DEFAULT 'geral',
                    conteudo TEXT NOT NULL,
                    data_hora DATETIME DEFAULT CURRENT_TIMESTAMP
                );
            """;

           
            try (Connection conn = conectar();
                Statement stmt = conn.createStatement()) {
                
                stmt.execute("PRAGMA journal_mode=WAL;");
                stmt.execute(sqlUsuarios);
                stmt.execute(sqlMensagens);
                System.out.println("[BANCO] Tabelas verificadas/criadas com sucesso.");

            } catch (SQLException e) {
                System.err.println("[BANCO] Erro ao inicializar o banco: " + e.getMessage());
            }
        }

        public static void salvarUsuario(String apelido) {
            String sql = "INSERT OR IGNORE INTO usuarios (apelido) VALUES (?);";
            try (Connection conn = conectar();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, apelido);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[BANCO] Erro ao registrar usuário: " + e.getMessage());
            }
        }

           public static void salvarMensagem(String remetente, String destinatario, String sala, String conteudo) {
            String sql = """
                INSERT INTO mensagens (remetente, destinatario, sala, conteudo)
                VALUES (?, ?, ?, ?);
            """;
    
            try (Connection conn = conectar();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
    
                pstmt.setString(1, remetente);
                pstmt.setString(2, destinatario); 
                pstmt.setString(3, sala != null ? sala : "geral");
                pstmt.setString(4, conteudo);
    
                pstmt.executeUpdate();
    
            } catch (SQLException e) {
                System.err.println("[BANCO] Erro ao salvar mensagem: " + e.getMessage());
            }
        }

           public static List<String> buscarHistorico(String sala, int limite) {
            List<String> historico = new ArrayList<>();
            
           
            String sql = """
                SELECT remetente, conteudo, data_hora 
                FROM mensagens 
                WHERE sala = ? AND destinatario IS NULL
                ORDER BY id DESC 
                LIMIT ?;
            """;
    
            try (Connection conn = conectar();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, sala != null ? sala : "geral");
                pstmt.setInt(2, limite);

                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    String remetente = rs.getString("remetente");
                    String conteudo = rs.getString("conteudo");
                    String dataHora = rs.getString("data_hora");

                    historico.add("[" + dataHora + "] " + remetente + ": " + conteudo);
                }

               
                Collections.reverse(historico);

            } catch (SQLException e) {
                System.err.println("[BANCO] Erro ao buscar histórico: " + e.getMessage());
            }

            return historico;
        }



    
}
