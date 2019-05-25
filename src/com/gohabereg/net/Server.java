package com.gohabereg.net;

import com.gohabereg.net.io.Receiver;
import com.gohabereg.watcher.Watcher;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Server implements Runnable {
    private Semaphore semaphore;
    private Watcher watcher;
    private int port;

    public Server(int port, Semaphore semaphore, Watcher watcher) {
        this.port = port;
        this.semaphore = semaphore;
        this.watcher = watcher;
    }

    @Override
    public void run() {
        System.out.format("Starting server on %d port\n", port);

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            Socket clientSocket = serverSocket.accept();
            InputStream in = clientSocket.getInputStream();
            System.out.println("Client connected");

            new Receiver(new DataInputStream(in), semaphore, watcher).run();
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port " + port + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    }
}
