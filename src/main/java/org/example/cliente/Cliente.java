package org.example.cliente;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Cliente {
    private static final int PORTA = 4000;
    private static final String ENDERECO = "localhost";

    private final Socket conexao;
    private final DataInputStream entrada;
    private final DataOutputStream saida;
    private final Scanner scanner;
    private volatile boolean encerrando;

    public Cliente() throws IOException {
        this.conexao = new Socket(ENDERECO, PORTA);
        this.entrada = new DataInputStream(conexao.getInputStream());
        this.saida = new DataOutputStream(conexao.getOutputStream());
        this.scanner = new Scanner(System.in);
        this.encerrando = false;

        System.out.println("[CLIENTE] Conectado a " + ENDERECO + ":" + PORTA + ".");
    }

    public static void main(String[] args) {
        try {
            Cliente cliente = new Cliente();
            cliente.iniciar();
        } catch (IOException exception) {
            System.out.println("Não foi possível conectar ao servidor.");
        }
    }

    public void iniciar() {
        try {
        
            while (true) {
                System.out.print("Digite seu apelido para entrar: ");
                String apelido = scanner.nextLine().trim();
                if (apelido.isBlank()) {
                    System.out.println("[CLIENTE] O apelido não pode ser vazio.");
                    continue;
                }

                saida.writeUTF(apelido);
                saida.flush();

                String resposta = entrada.readUTF();
                if (resposta.startsWith("[OK]")) {
                    System.out.println(resposta);
                    break;
                } else {
                    System.out.println(resposta);
                }
            }

  
            Thread threadReceptora = new Thread(this::receberMensagens);
            threadReceptora.start();

          
            enviarMensagem();
        } catch (IOException e) {
            System.err.println("[CLIENTE] Erro ao comunicar com o servidor: " + e.getMessage());
        } finally {
            encerrar();
        }
    }

    public void enviarMensagem() {
        System.out.println("[CLIENTE] Digite sua mensagem ou /ajuda para opções (/sair para encerrar).");
        System.out.print("> ");
        while (scanner.hasNextLine()) {
            String mensagem = scanner.nextLine();

            if (mensagem.equalsIgnoreCase("/sair")) break;

            if (mensagem.isBlank()) {
                System.out.print("> ");
                continue;
            }
            try {
                saida.writeUTF(mensagem);
                saida.flush();
            } catch (IOException e) {
                System.out.println("[CLIENTE] Não foi possível enviar a mensagem.");
                break;
            }
            System.out.print("> ");
        }
    }

    public void receberMensagens() {
        try {
            while (true) {
                String mensagem = entrada.readUTF();
                System.out.println("\n" + mensagem);
                System.out.print("> ");
            }
        } catch (IOException exception) {
            if (!encerrando) {
                System.out.println("\n[CLIENTE] Conexão com o servidor encerrada.");
            }
        }
    }

    public void encerrar() {
        encerrando = true;

        try {
            conexao.close();
            System.out.println("[CLIENTE] Desconectado.");
        } catch (IOException exception) {
            System.err.println("[CLIENTE] Erro ao fechar conexão.");
        }
    }
}
