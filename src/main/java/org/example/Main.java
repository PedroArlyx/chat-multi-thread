package org.example;

import java.io.IOException;

import org.example.banco.BancoDados;
import org.example.servidor.Servidor;


public class Main {
    public static void main(String[] args) {

        BancoDados.inicializar();
        try {
            Servidor servidor = new Servidor();
            servidor.aceitarConexoes();
        } catch (IOException e) {
            System.out.println("[ERROR] Não foi possível iniciar o servidor.");
        }
    }
}
