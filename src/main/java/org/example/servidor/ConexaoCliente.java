package org.example.servidor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

import org.example.banco.BancoDados;

public class ConexaoCliente implements Runnable {

    private final Socket conexao;
    private final Servidor servidor;
    private final DataInputStream entrada;
    private final DataOutputStream saida;
    private volatile String identificacao;

    public ConexaoCliente(Socket conexao, Servidor servidor) throws IOException {
        this.conexao = conexao;
        this.servidor = servidor;
        this.entrada = new DataInputStream(conexao.getInputStream());
        this.saida = new DataOutputStream(conexao.getOutputStream());
        this.identificacao = null;
    }

    @Override
    public void run() {
        try {
     
            while (true) {
                String apelido = entrada.readUTF().trim();
                if (apelido.isBlank() || apelido.contains(" ") || apelido.startsWith("/")) {
                    enviarMensagem("[ERRO] Apelido inválido. Escolha um nome sem espaços e sem barras.");
                    continue;
                }
                if (servidor.apelidoEmUso(apelido)) {
                    enviarMensagem("[ERRO] Apelido '" + apelido + "' já está em uso.");
                    continue;
                }

                this.identificacao = apelido;
                BancoDados.salvarUsuario(apelido);
                enviarMensagem("[OK] Bem-vindo ao chat, " + apelido + "!");
                break;
            }

          
            List<String> historico = BancoDados.buscarHistorico("geral", 15);
            if (!historico.isEmpty()) {
                enviarMensagem("========== ÚLTIMAS MENSAGENS ==========");
                for (String linha : historico) {
                    enviarMensagem(linha);
                }
                enviarMensagem("=======================================");
            }

            servidor.distribuirMensagem("[SERVIDOR] " + identificacao + " entrou no chat!", this);

         
            while (true) {
                String mensagem = entrada.readUTF();
                servidor.processarMensagem(mensagem, this);
            }
        } catch (IOException ignored) {
        } finally {
            servidor.removerCliente(this);
            if (identificacao != null) {
                servidor.distribuirMensagem("[SERVIDOR] " + identificacao + " saiu do chat.", this);
            }
            fechar();
        }
    }

    public void fechar() {
        try {
            conexao.close();
        } catch (IOException e) {
            System.err.println("[ERROR] Erro ao fechar conexão.");
        }
    }

    public synchronized void enviarMensagem(String mensagem) {
        try {
            saida.writeUTF(mensagem);
            saida.flush();
        } catch (IOException e) {
            System.out.println("[SERVIDOR] Falha ao enviar para " + identificacao + ", encerrando conexão.");
            fechar();
        }
    }

    public String getIdentificacao() {
        return identificacao;
    }
}
