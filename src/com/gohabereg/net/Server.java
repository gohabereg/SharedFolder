package com.gohabereg.net;

import com.gohabereg.net.io.CustomReader;

import java.io.*;
import java.net.*;

public class Server extends Thread {
//    private ServerSocket serverSocket;
//    private Socket clientSocket;
//    private PrintWriter out;
//    BufferedReader in;

    public Server(Integer port) {
        try (
            ServerSocket serverSocket = new ServerSocket(port);
            Socket clientSocket = serverSocket.accept();
//            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            InputStream in = clientSocket.getInputStream();
        ) {
            System.out.println("Client connected");

            Thread listener = new Thread(new CustomReader(new DataInputStream(in)));
            listener.start();

            while (true) {

            }
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port " + port + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    }

}
