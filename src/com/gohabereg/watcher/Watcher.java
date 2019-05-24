package com.gohabereg.watcher;

import com.gohabereg.net.Client;

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.util.*;

public class Watcher {
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

    private void registerAll(Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public Watcher(Path dir, boolean recursive) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
        this.recursive = recursive;
        this.client = new Client("localhost", 8000);

        if (recursive) {
            System.out.format("Scanning %s ...\n", dir);
            registerAll(dir);
            System.out.println("Done.");
        } else {
            register(dir);
        }

        this.trace = true;
    }

    public void processEvents() {
        for (;;) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch(InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!");
                continue;
            }


            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                if (kind == OVERFLOW) {
                    continue;
                }

                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);


                System.out.format("%s: %s\n", kind.name(), child);

                if (!Files.isDirectory(child)) {
                    switch (kind.name()) {
                        case "ENTRY_CREATE":
                            client.sendEvent((byte)0, child.toString());
                            break;
                        case "ENTRY_MODIFY":
                            client.sendEvent((byte)1, child.toString());
                            break;
                        case "ENTRY_DELETE":
                            client.sendEvent((byte)2, child.toString());
                            break;
                    }
                }

                if (recursive && (kind == ENTRY_CREATE)) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
                    } catch (IOException x) {

                    }
                }

                boolean valid = key.reset();

                if (!valid) {
                    keys.remove(key);

                    if (keys.isEmpty()) {
                        break;
                    }
                }
            }
        }
    }

}
