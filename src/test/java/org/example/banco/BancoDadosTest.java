package org.example.banco;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BancoDadosTest {

    @BeforeAll
    public static void setup() throws Exception {
        BancoDados.inicializar();
        // Limpar tabelas para os testes
        try (Connection conn = BancoDados.conectar();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM mensagens");
            stmt.execute("DELETE FROM usuarios");
        }
    }

    @Test
    public void testSalvarEBuscarHistoricoGeral() {
        BancoDados.salvarUsuario("Alice");
        BancoDados.salvarUsuario("Bob");

        // Salvar mensagens públicas
        BancoDados.salvarMensagem("Alice", null, "geral", "Mensagem 1");
        BancoDados.salvarMensagem("Bob", null, "geral", "Mensagem 2");
        BancoDados.salvarMensagem("Alice", null, "geral", "Mensagem 3");

        // Salvar mensagem privada (não deve vir no histórico geral)
        BancoDados.salvarMensagem("Alice", "Bob", null, "Mensagem privada secreta");

        List<String> historico = BancoDados.buscarHistorico("geral", 15);
        assertNotNull(historico);
        assertEquals(3, historico.size());

        // Verificar ordem cronológica (antiga -> recente)
        assertTrue(historico.get(0).contains("Alice: Mensagem 1"));
        assertTrue(historico.get(1).contains("Bob: Mensagem 2"));
        assertTrue(historico.get(2).contains("Alice: Mensagem 3"));

        // Garantir que a privada não está
        for (String msg : historico) {
            assertFalse(msg.contains("Mensagem privada secreta"));
        }
    }

    @Test
    public void testLimiteHistorico() {
        for (int i = 1; i <= 20; i++) {
            BancoDados.salvarMensagem("User", null, "geral", "Msg " + i);
        }

        List<String> limite5 = BancoDados.buscarHistorico("geral", 5);
        assertEquals(5, limite5.size());
        // Deve trazer as 5 mais recentes em ordem cronológica: Msg 16, 17, 18, 19, 20
        assertTrue(limite5.get(0).contains("Msg 16"));
        assertTrue(limite5.get(4).contains("Msg 20"));
    }
}
