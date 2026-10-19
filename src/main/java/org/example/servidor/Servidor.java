package org.example.servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Servidor {
    private static final int PORTA = 4000;
    private final ServerSocket instancia;
    private final List<ConexaoCliente> clientes =
            Collections.synchronizedList(new ArrayList<>());

    public Servidor() throws IOException {
        this.instancia = new ServerSocket(PORTA);
        System.out.println("[SERVIDOR] Iniciado na porta! " + PORTA);
    }

    public void aceitarConexoes() {
        System.out.println("[SERVIDOR] Aguardando conexoes...");
        while (!instancia.isClosed()) {
            Socket cliente;
            try {
                cliente = instancia.accept();
            } catch (IOException e) {
                if (instancia.isClosed()) break;
                System.out.println("[SERVIDOR] Erro ao aceitar conexão: " + e.getMessage());
                continue;
            }
            try {
                ConexaoCliente conexaoCliente = new ConexaoCliente(cliente, this);
                clientes.add(conexaoCliente);

                System.out.println("[SERVIDOR] Cliente conectado: " + conexaoCliente.getIdentificacao() +
                        " | ativos: " + clientes.size());

                Thread thread = new Thread(conexaoCliente, "cliente-" + cliente.getPort());
                thread.start();
            } catch (Exception e) {
                System.out.println("[SERVIDOR] erro ao aceitar conexao! ");
                try {
                    cliente.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public void distribuirMensagem(String mensagem, ConexaoCliente origem) {
        List<ConexaoCliente> destinatarios;

        synchronized (clientes) {
            destinatarios = new ArrayList<>(clientes);
        }
        destinatarios.forEach((conexaoCliente) -> {
            if (conexaoCliente != origem) {
                conexaoCliente.enviarMensagem(mensagem);
            }
        });

    }

    public void removerCliente(ConexaoCliente cliente) {
        if (clientes.remove(cliente)) {
            System.out.println("[SERVIDOR] cliente desconectado " + cliente.getIdentificacao() + " | ativos: " + clientes.size());
        }
    }
}
