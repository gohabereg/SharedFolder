package com.gohabereg.net.io;

import java.io.*;
import java.nio.file.*;

public class CustomReader implements Runnable {
    private DataInputStream reader;

    public CustomReader(DataInputStream reader) {
        this.reader = reader;
    }

    public void run() {
        System.out.println("Run reader");

        while (true) {
            try {
                System.out.println("Waiting for data");

                int eventType = reader.readByte();

                System.out.println("Receive event " + eventType + " type");

                String name;

                switch (eventType) {
                    case 0:
                    case 1:
                        name = reader.readUTF();
                        int size = reader.readInt();


                        byte[] data = new byte[size];
                        reader.read(data, 0, size);

                        writeFile(name, size, data);

                        break;

                    case 2:
                        name = reader.readUTF();

                        deleteFile(name);
                        break;
                }


            } catch (IOException e) {
            }

        }
    }

    private void writeFile(String name, int size, byte[] data) {
        try {
            FileOutputStream out = new FileOutputStream("output/" + name);

            System.out.println("Writing to file " + name + " with size of " + size + " ...");

            System.out.println(data);

            out.write(data, 0, size);

            out.flush();

            System.out.println("Done.");
        } catch (Exception e) {
            System.out.println("Can't write the file");
            System.out.println(e.getMessage());
        }
    }

    private void deleteFile(String name) {
        System.out.println("Deleting file " + name + " ...");

        File file = new File(name);

        file.delete();

        System.out.println("Done.");
    }
}
