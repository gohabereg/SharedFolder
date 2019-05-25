package com.gohabereg.net.io;

import com.gohabereg.watcher.Watcher;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.ArrayList;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

public class Receiver implements Runnable {
    private ArrayList<Path> filesNotToCheck;
    private Watcher watcher;
    private DataInputStream reader;

    public Receiver(DataInputStream reader, ArrayList<Path> filesNotToCheck, Watcher watcher) {
        this.reader = reader;
        this.filesNotToCheck = filesNotToCheck;
        this.watcher = watcher;
    }

    public void run() {
        System.out.println("Run receiver");

        while (true) {
            try {
                if (reader.available() == 0) {
                    continue;
                }

                int eventType = reader.readByte();

                System.out.println("Receive event " + eventType + " type");

                String  name = reader.readUTF();

                filesNotToCheck.add(Paths.get(name));


                switch (eventType) {
                    case 0:
                    case 1:
                        int size = reader.readInt();


                        byte[] data = new byte[size];
                        reader.read(data, 0, size);

                        writeFile(name, size, data);

                        break;

                    case 2:
                        deleteFile(name);
                        break;

                    case 3:
                        createDirectory(name);
                        break;
                }
            } catch (IOException e) {
                System.out.println("Error");
                System.out.println(e.getMessage());
            }
        }
    }

    private void writeFile(String name, int size, byte[] data) {
        try {
            Path path = Paths.get(name);
            Path parentDirs = path.getParent();

            if (Files.notExists(parentDirs)) {
                Files.createDirectories(parentDirs);
            }

            if (Files.notExists(path)) {
                Files.createFile(path);
            }

            FileOutputStream out = new FileOutputStream(name);

            System.out.println("Writing to file " + name + " with size of " + size + " ...");

            out.write(data, 0, size);

            out.flush();
            out.close();

            System.out.println("Done.");
        } catch (Exception e) {
            System.out.println("Can't write the file");
            System.out.println(e.getMessage());
        }
    }

    private void deleteFile(String name) {
        try {
            System.out.println("Deleting " + name + " ...");

            if (Files.isDirectory(Paths.get(name))) {
                FileVisitor visitor = new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        if (exc != null) {
                            throw exc;
                        }
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                };
                Files.walkFileTree(Paths.get(name), visitor);
            } else {
                File file = new File(name);

                file.delete();
            }

            System.out.println("Done.");
        } catch (IOException e) {
            System.out.println("Can't delete " + name);
            System.out.println(e.getMessage());
        }
    }

    private void createDirectory(String name) {
        try {
            Path dir = Paths.get(name);

            Files.createDirectory(dir);

            if (Files.isDirectory(dir, NOFOLLOW_LINKS)) {
                this.watcher.registerAll(dir);
            }
        } catch (IOException e) {
            System.out.println("Can't create directory");
            System.out.println(e.getMessage());
        }
    }
}
