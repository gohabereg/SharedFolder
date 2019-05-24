package com.gohabereg;

import com.gohabereg.net.Client;
import com.gohabereg.net.Server;
import com.gohabereg.watcher.Watcher;

import java.io.*;
import java.nio.file.*;

public class Main {

    public static void main(String[] args) throws IOException {

        if ("server".equals(args[0])) {
            System.out.println("Server");
            new Server(8000);
        } else {
            Path dir = Paths.get(args[1]);
            System.out.println("Client");
            new Watcher(dir, true).processEvents();
        }
    }
}