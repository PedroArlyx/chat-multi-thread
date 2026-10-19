package org.example;

import org.example.servidor.Servidor;

import java.io.IOException;


public class Main {
    public static void main(String[] args) {

        try {
            Servidor servidor = new Servidor();
            servidor.aceitarConexoes();
        } catch (IOException e) {
            System.out.println("[ERROR] Não foi possível iniciar o servidor.");
        }
    }
}
