package org.example.servidor;

import org.example.banco.BancoDados;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ServidorHistoricoIntegrationTest {

    private static Servidor servidor;
    private static Thread servidorThread;
    private static final int PORTA = 4000;

    @BeforeAll
    public static void iniciarServidor() throws Exception {
        BancoDados.inicializar();
        try (Connection conn = BancoDados.conectar();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM mensagens");
            stmt.execute("DELETE FROM usuarios");
        }

        servidor = new Servidor();
        servidorThread = new Thread(() -> servidor.aceitarConexoes(), "servidor-teste");
        servidorThread.start();
        // Pequena pausa para garantir que o ServerSocket está escutando
        TimeUnit.MILLISECONDS.sleep(300);
    }

    @AfterAll
    public static void finalizarServidor() {
        if (servidor != null) {
            servidor.parar();
        }
    }

    private static class ClienteTeste implements AutoCloseable {
        final Socket socket;
        final DataOutputStream out;
        final DataInputStream in;

        ClienteTeste(String apelido) throws IOException {
            this.socket = new Socket("localhost", PORTA);
            this.out = new DataOutputStream(socket.getOutputStream());
            this.in = new DataInputStream(socket.getInputStream());

            // Envia apelido
            out.writeUTF(apelido);
            out.flush();

            // Lê resposta do apelido (deve ser [OK]...)
            String resp = in.readUTF();
            assertTrue(resp.startsWith("[OK]"), "Resposta de login inesperada: " + resp);
        }

        void enviar(String msg) throws IOException {
            out.writeUTF(msg);
            out.flush();
        }

        String ler() throws IOException {
            return in.readUTF();
        }

        List<String> lerMensagensAte(String contem) throws IOException {
            List<String> lidas = new ArrayList<>();
            while (true) {
                String m = in.readUTF();
                lidas.add(m);
                if (m.contains(contem)) {
                    break;
                }
            }
            return lidas;
        }

        @Override
        public void close() throws Exception {
            socket.close();
        }
    }

    @Test
    public void testComandoAjudaEComandoHistoricoCompleto() throws Exception {
        try (ClienteTeste user1 = new ClienteTeste("TestUser1")) {
            // 1. Testa /ajuda
            user1.enviar("/ajuda");
            String ajuda = user1.ler();
            assertTrue(ajuda.contains("/historico [quantidade]"), "Ajuda deveria conter /historico");

            // 2. Testa /historico quando não há mensagens
            user1.enviar("/historico");
            String semHistorico = user1.ler();
            assertEquals("[HISTÓRICO] Nenhuma mensagem pública encontrada.", semHistorico);

            // 3. Testa validações de entrada do /historico
            // Excesso de argumentos
            user1.enviar("/historico 10 20");
            assertEquals("[SERVIDOR] Uso correto: /historico [quantidade]", user1.ler());

            // Argumento não numérico
            user1.enviar("/historico abc");
            assertEquals("[SERVIDOR] A quantidade deve ser um número entre 1 e 50.", user1.ler());

            // Limite menor que 1
            user1.enviar("/historico 0");
            assertEquals("[SERVIDOR] A quantidade deve ser um número entre 1 e 50.", user1.ler());

            // Limite maior que 50
            user1.enviar("/historico 51");
            assertEquals("[SERVIDOR] A quantidade deve ser um número entre 1 e 50.", user1.ler());

            // 4. Enviar mensagens públicas e testar busca com histórico
            user1.enviar("Ola mundo!");
            user1.enviar("Segunda mensagem!");
            user1.enviar("Terceira mensagem!");
            // Espera breve para persistência no SQLite
            TimeUnit.MILLISECONDS.sleep(200);

            // Testa /historico sem argumento (padrão 15)
            user1.enviar("/historico");
            assertEquals("========== HISTÓRICO DE MENSAGENS ==========", user1.ler());
            String m1 = user1.ler();
            String m2 = user1.ler();
            String m3 = user1.ler();
            assertEquals("============================================", user1.ler());

            assertTrue(m1.contains("TestUser1: Ola mundo!"));
            assertTrue(m2.contains("TestUser1: Segunda mensagem!"));
            assertTrue(m3.contains("TestUser1: Terceira mensagem!"));

            // Testa /historico 2 (limite de 2)
            user1.enviar("/historico 2");
            assertEquals("========== HISTÓRICO DE MENSAGENS ==========", user1.ler());
            String lim1 = user1.ler();
            String lim2 = user1.ler();
            assertEquals("============================================", user1.ler());

            assertTrue(lim1.contains("TestUser1: Segunda mensagem!"));
            assertTrue(lim2.contains("TestUser1: Terceira mensagem!"));
        }
    }

    @Test
    public void testNovoUsuarioRecebeHistoricoNoLoginEMensagensPrivadasNaoAparecemNoHistorico() throws Exception {
        // Conectar TestUser2 e TestUser3
        try (ClienteTeste user2 = new ClienteTeste("TestUser2")) {
            // TestUser2 ao conectar deve receber o histórico automático inicial de boas-vindas
            // pois já existem mensagens públicas anteriores no banco
            String cabecalhoLogin = user2.ler();
            assertEquals("========== ÚLTIMAS MENSAGENS ==========", cabecalhoLogin);

            List<String> msgsIniciais = user2.lerMensagensAte("=======================================");
            assertFalse(msgsIniciais.isEmpty());

            try (ClienteTeste user3 = new ClienteTeste("TestUser3")) {
                // user3 também drena as mensagens de histórico recebidas no login
                assertEquals("========== ÚLTIMAS MENSAGENS ==========", user3.ler());
                user3.lerMensagensAte("=======================================");

                // user2 recebe notificação de entrada do user3
                String notificacaoEntrada = user2.ler();
                assertTrue(notificacaoEntrada.contains("TestUser3 entrou no chat!"));

                // user3 envia mensagem privada para user2
                user3.enviar("/msg TestUser2 SegredoPrivado123");

                // user3 recebe confirmação privada
                String confirmacaoUser3 = user3.ler();
                assertTrue(confirmacaoUser3.contains("[PRIVADO para TestUser2]: SegredoPrivado123"));

                // user2 recebe a mensagem privada
                String privadoUser2 = user2.ler();
                assertTrue(privadoUser2.contains("[PRIVADO de TestUser3]: SegredoPrivado123"));

                // user2 consulta o histórico
                user2.enviar("/historico");
                assertEquals("========== HISTÓRICO DE MENSAGENS ==========", user2.ler());
                List<String> historico = user2.lerMensagensAte("============================================");

                // Garante que o segredo privado NÃO está presente no histórico público
                for (String h : historico) {
                    assertFalse(h.contains("SegredoPrivado123"), "Mensagem privada apareceu no histórico público!");
                }
            }
        }
    }
}
