package com.gohabereg.net;

import com.gohabereg.net.io.*;
import java.net.*;
import java.io.*;

public class Client extends Thread {
    DataOutputStream out;

    public Client(String host, Integer port) {
        try (
            Socket socket = new Socket(host, port);
            OutputStream out = socket.getOutputStream();
        ) {
            this.out = new DataOutputStream(out);
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + host);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " + host);
            System.exit(1);
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

                    BufferedInputStream bin = new BufferedInputStream(fin);

                    DataInputStream din = new DataInputStream(bin);

                    din.readFully(data, 0, data.length);

                    this.out.writeInt(data.length);
                    this.out.write(data, 0, data.length);
            }
        } catch (IOException x) {

        }
    }

}
