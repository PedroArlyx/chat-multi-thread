package org.example.servidor;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ConexaoCliente implements Runnable {

    private final Socket conexao;
    private final Servidor servidor;
    private final DataInputStream entrada;
    private final DataOutputStream saida;
    private final String identificacao;

    public ConexaoCliente(Socket conexao, Servidor servidor) throws IOException {
        this.conexao = conexao;
        this.servidor = servidor;
        this.entrada = new DataInputStream(conexao.getInputStream());
        this.saida = new DataOutputStream(conexao.getOutputStream());
        this.identificacao = conexao.getRemoteSocketAddress().toString();

    }

    @Override
    public void run() {
        try {
            while (true) {
                String mensagem = entrada.readUTF();
                servidor.distribuirMensagem(identificacao + ": " + mensagem, this);
            }
        } catch (IOException ignored) {

        } finally {
            servidor.removerCliente(this);
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
