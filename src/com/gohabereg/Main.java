package com.gohabereg;
import com.gohabereg.net.Client;
import com.gohabereg.net.Server;
import com.gohabereg.watcher.Watcher;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Main {

    public static void main(String[] args) throws IOException {

        String folder = args[0];
        int port = Integer.parseInt(args[1]);

        Semaphore semaphore = new Semaphore(1);

        Path dir = Paths.get(folder);
        Client client = new Client("localhost", Integer.parseInt(args[2]));
        Watcher watcher = new Watcher(dir, true, semaphore, client);

        Thread clientThread = new Thread(client);
        Thread watcherThread = new Thread(watcher);

        clientThread.start();
        watcherThread.start();

        Thread serverThread = new Thread(new Server(port, semaphore, watcher));
        serverThread.start();
    }
}