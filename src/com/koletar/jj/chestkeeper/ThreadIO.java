package com.koletar.jj.chestkeeper;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author jjkoletar
 */
public class ThreadIO implements Runnable {
    private final Set<CKUser> ioQueue;
    private final File saveFolder;

    public ThreadIO(final Set<CKUser> ioQueue, final File saveFolder) {
        this.ioQueue = ioQueue;
        this.saveFolder = saveFolder;
    }

    public void run() {
        while (true) {
            final Set<CKUser> toWrite;
            synchronized (ioQueue) {
                while (ioQueue.isEmpty()) {
                    try {
                        ioQueue.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace(); //TODO: i think this is supposed to be handled better
                    }
                }
                toWrite = new HashSet<CKUser>(ioQueue);
            }
            //TODO: what if a user opens a chest during IO?
            for (CKUser user : toWrite) {
                try {
                    File saveFile = new File(saveFolder, ChestKeeper.getFileName(user.getUsername()));
                    YamlConfiguration save = YamlConfiguration.loadConfiguration(saveFile);
                    save.set("user", user);
                    save.save(saveFile);
                } catch (IOException e) {
                    ChestKeeper.logger.severe("Unable to save a user to disk! Username: " + user.getUsername());
                    e.printStackTrace();
                }
            }
        }
    }
}
