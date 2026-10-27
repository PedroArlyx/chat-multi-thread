package org.example.servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.example.banco.BancoDados;

public class Servidor {
    private static final int PORTA = 4000;
    private final ServerSocket instancia;
    private final List<ConexaoCliente> clientes =
            Collections.synchronizedList(new ArrayList<>());

    public Servidor() throws IOException {
        this.instancia = new ServerSocket(PORTA);
        System.out.println("[SERVIDOR] Iniciado na porta " + PORTA);
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

                System.out.println("[SERVIDOR] Nova conexão de " + cliente.getRemoteSocketAddress() +
                        " | ativos: " + clientes.size());

                Thread thread = new Thread(conexaoCliente, "cliente-" + cliente.getPort());
                thread.start();
            } catch (Exception e) {
                System.out.println("[SERVIDOR] Erro ao iniciar cliente: " + e.getMessage());
                try {
                    cliente.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public boolean apelidoEmUso(String apelido) {
        synchronized (clientes) {
            for (ConexaoCliente c : clientes) {
                if (apelido.equalsIgnoreCase(c.getIdentificacao())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void processarMensagem(String mensagem, ConexaoCliente remetente) {
        if (mensagem.startsWith("/")) {
            tratarComando(mensagem, remetente);
        } else {
            distribuirMensagemPublica(mensagem, remetente);
        }
    }

    public void distribuirMensagemPublica(String conteudo, ConexaoCliente origem) {

        BancoDados.salvarMensagem(origem.getIdentificacao(), null, "geral", conteudo);

        String formatada = "[" + origem.getIdentificacao() + "]: " + conteudo;
        distribuirMensagem(formatada, origem);
    }

    public void enviarMensagemPrivada(ConexaoCliente remetente, String destinoNome, String texto) {
        ConexaoCliente destino = null;

        synchronized (clientes) {
            for (ConexaoCliente c : clientes) {
                if (c.getIdentificacao() != null && c.getIdentificacao().equalsIgnoreCase(destinoNome)) {
                    destino = c;
                    break;
                }
            }
        }

        if (destino != null) {
            if (destino == remetente) {
                remetente.enviarMensagem("[SERVIDOR] Você não pode enviar mensagem privada para si mesmo.");
                return;
            }

            destino.enviarMensagem("[PRIVADO de " + remetente.getIdentificacao() + "]: " + texto);
            remetente.enviarMensagem("[PRIVADO para " + destino.getIdentificacao() + "]: " + texto);

          
            BancoDados.salvarMensagem(remetente.getIdentificacao(), destino.getIdentificacao(), null, texto);
        } else {
            remetente.enviarMensagem("[SERVIDOR] Usuário '" + destinoNome + "' não encontrado ou offline.");
        }
    }

    private void tratarComando(String comandoCompleto, ConexaoCliente remetente) {
        String[] partes = comandoCompleto.split("\\s+", 3);
        String comando = partes[0].toLowerCase();

        switch (comando) {
            case "/online", "/usuarios" -> {
                StringBuilder sb = new StringBuilder("[ONLINE] Usuários conectados: ");
                synchronized (clientes) {
                    List<String> apelidos = new ArrayList<>();
                    for (ConexaoCliente c : clientes) {
                        if (c.getIdentificacao() != null) {
                            apelidos.add(c.getIdentificacao());
                        }
                    }
                    sb.append(String.join(", ", apelidos));
                }
                remetente.enviarMensagem(sb.toString());
            }

            case "/msg", "/w" -> {
                if (partes.length < 3) {
                    remetente.enviarMensagem("[SERVIDOR] Uso correto: /msg <apelido> <mensagem>");
                    return;
                }
                String destinatario = partes[1];
                String texto = partes[2];
                enviarMensagemPrivada(remetente, destinatario, texto);
            }

            case "/ajuda", "/help" -> {
                remetente.enviarMensagem("""
                    [AJUDA] Comandos disponíveis:
                    /online                - Lista usuários conectados
                    /msg <apelido> <texto> - Envia mensagem privada
                    /ajuda                 - Exibe esta mensagem de ajuda
                    /sair                  - Desconecta do chat
                    """);
            }

            default -> remetente.enviarMensagem("[SERVIDOR] Comando desconhecido: " + comando + ". Digite /ajuda para opções.");
        }
    }

    public void distribuirMensagem(String mensagem, ConexaoCliente origem) {
        List<ConexaoCliente> destinatarios;

        synchronized (clientes) {
            destinatarios = new ArrayList<>(clientes);
        }
        destinatarios.forEach((conexaoCliente) -> {
            if (conexaoCliente != origem && conexaoCliente.getIdentificacao() != null) {
                conexaoCliente.enviarMensagem(mensagem);
            }
        });
    }

    public void removerCliente(ConexaoCliente cliente) {
        if (clientes.remove(cliente)) {
            System.out.println("[SERVIDOR] Cliente desconectado " + cliente.getIdentificacao() + " | ativos: " + clientes.size());
        }
    }
}
