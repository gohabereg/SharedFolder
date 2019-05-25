package com.gohabereg.net;

import com.gohabereg.net.io.*;
import java.net.*;
import java.io.*;

public class Client implements Runnable {
    private int port;
    private String host;
    private DataOutputStream out;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void run() {
        System.out.format("Connecting client to %s:%d\n", host, port);

        while (true) {
            try {
                Socket socket = new Socket(host, port);

                OutputStream out = socket.getOutputStream();

                this.out = new DataOutputStream(out);

                socket.setKeepAlive(true);

                if (socket.isConnected()) {
                    break;
                }
            } catch (UnknownHostException e) {
                System.err.println("Don't know about host " + host);
            } catch (IOException e) {
//                System.err.println("Couldn't get I/O for the connection to " + host);
            }
        }
    }

    public void sendEvent(byte type, String name) {
        System.out.println("Sending " + type + " event with file " + name);

        try {
            this.out.writeByte(type);
            this.out.writeUTF(name);

            if (type == 1 || type == 0) {

                    File fileToSend = new File(name);

                    byte[] data = new byte[(int) fileToSend.length()];

                    FileInputStream fin = new FileInputStream(fileToSend);

                    DataInputStream din = new DataInputStream(fin);

                    din.readFully(data, 0, data.length);

                    fin.close();
                    din.close();

                    this.out.writeInt(data.length);
                    this.out.write(data, 0, data.length);
            }

            this.out.flush();
        } catch (IOException x) {
            System.out.println("Error");
            System.out.println(x.getMessage());
        }
    }

}
