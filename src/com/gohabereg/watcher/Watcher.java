package com.gohabereg.watcher;

import com.gohabereg.net.Client;

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.util.*;

public class Watcher implements Runnable {
    private final ArrayList<Path> filesNotToCheck;
    private WatchService watcher;
    private Map<WatchKey, Path> keys;
    private boolean recursive;
    private boolean trace = false;
    private Client client;

    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }

    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

        if (trace) {
            Path prev = keys.get(key);
            if (prev == null) {
                System.out.format("register: %s\n", dir);
            } else {
                if (!dir.equals(prev)) {
                    System.out.format("update: %s -> %s\n", prev, dir);
                }
            }
        }

        keys.put(key, dir);
    }

    public void registerAll(Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public Watcher(Path dir, boolean recursive, ArrayList<Path> filesNotToCheck, Client client) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
        this.recursive = recursive;
        this.client = client;
        this.filesNotToCheck = filesNotToCheck;

        if (recursive) {
            System.out.format("Scanning %s ...\n", dir);
            registerAll(dir);
            System.out.println("Done.");
        } else {
            register(dir);
        }

        this.trace = true;
    }

    @Override
    public void run() {
        while (true) {
            WatchKey key;

            try {
                key = watcher.take();
            } catch(ClosedWatchServiceException x) {
                System.out.println("Closed");
                return;
            } catch(InterruptedException x) {
                System.out.println("Interrupted");
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized");
                continue;
            }

            List<WatchEvent<?>> events = key.pollEvents();

            for (WatchEvent<?> event: events) {
                WatchEvent.Kind kind = event.kind();

                if (kind == OVERFLOW) {
                    continue;
                }

                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                if (!filesNotToCheck.contains(child)) {

                    System.out.format("%s (isDirectory: %b): %s\n", kind.name(), Files.isDirectory(child), child);

                    byte type;

                    switch (kind.name()) {
                        case "ENTRY_CREATE":
                            type = 0;
                            if (Files.isDirectory(child)) {
                                type = 3;
                            }

                            client.sendEvent(type, child.toString());
                            break;
                        case "ENTRY_MODIFY":
                            type = 1;
                            if (Files.exists(child) && !Files.isDirectory(child)) {
                                client.sendEvent(type, child.toString());
                            }
                            break;
                        case "ENTRY_DELETE":
                            type = 2;
                            client.sendEvent(type, child.toString());
                            break;
                    }

                    if (recursive && (kind == ENTRY_CREATE)) {
                        try {
                            if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                                registerAll(child);
                            }
                        } catch (IOException x) {

                        }
                    }

                } else {
                    filesNotToCheck.remove(child);
                }

                boolean valid = key.reset();

                if (!valid) {
                    keys.remove(key);

                    if (keys.isEmpty()) {
                        System.out.println("Keys is empty");
                        break;
                    }
                }
            }
        }
    }

}
