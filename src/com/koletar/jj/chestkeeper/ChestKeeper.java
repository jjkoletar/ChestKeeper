package com.koletar.jj.chestkeeper;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author jjkoletar
 */
public class ChestKeeper extends JavaPlugin {
    public static Logger logger;
    private static final boolean TRACE = true;
    private Map<String, CKUser> users;
    private List<String> fileUsers;
    private final Map<String, String> ioQueue = new HashMap<String, String>();
    private CKFacilitator facilitator;
    private ThreadIO io;
    private BukkitTask task;
    private YamlConfiguration serializer;

    public static final class Config {
        private static int maxNumberOfChests = 10;

        public static int getMaxNumberOfChests() {
            return maxNumberOfChests;
        }

        private static void setMaxNumberOfChests(int i) {
            maxNumberOfChests = i;
        }

        public static void writeMaxNumberOfChests(BufferedWriter out) throws IOException {
            out.write("# Maximum number of chests a user may own. Users with the chestkeeper.override permission are not bound by this value. -1 = no limit");
            out.newLine();
            out.write("maxNumberOfChests: 10");
            out.newLine();
        }
    }

    private static final class DataFilter implements FilenameFilter {
        public boolean accept(File file, String s) {
            return s.endsWith(".yml");
        }
    }

    protected static String getFileName(String username) {
        return username.toLowerCase() + ".yml";
    }

    static {
        ConfigurationSerialization.registerClass(CKUser.class);
        ConfigurationSerialization.registerClass(CKChest.class);
    }

    public void onEnable() {
        logger = getLogger();
        logger.info("ChestKeeper v" + getDescription().getVersion() + " enabling...");
        users = new HashMap<String, CKUser>();
        fileUsers = new LinkedList<String>();
        try {
            loadConfiguration();
        } catch (IOException e) {
            logger.severe("Unable to load configuration!");
            e.printStackTrace();
        }
        loadData();
        facilitator = new CKFacilitator(this);
        serializer = new YamlConfiguration();
        io = new ThreadIO(ioQueue, new File(getDataFolder(), "data"));
        task = getServer().getScheduler().runTaskAsynchronously(this, io);
        getCommand("chestkeeper").setExecutor(facilitator);
        getServer().getPluginManager().registerEvents(facilitator, this);
        logger.info("ChestKeeper v" + getDescription().getVersion() + " enabled!");
    }

    public void onDisable() {
        logger.info("ChestKeeper disabling...");

        for (CKUser user : users.values()) {
            user.forceClean();
        }
        io.shutdown();

        logger.info("ChestKeeper disabled!");
    }

    public static void trace(String message) {
        if (TRACE) {
            logger.info("[Trace] " + message);
        }
    }

    private void loadConfiguration() throws IOException {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(configFile));
            out.write("# ChestKeeper configuration file");
            out.newLine();
            Config.writeMaxNumberOfChests(out);
            out.close();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        BufferedWriter out = new BufferedWriter(new FileWriter(configFile, true));
        if (config.contains("maxNumberOfChests")) {
            Config.setMaxNumberOfChests(config.getInt("maxNumberOfChests"));
        } else {
            Config.writeMaxNumberOfChests(out);
        }
        out.close();
    }

    private void loadData() {
        File dataFolder = new File(getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }
        for (File file : dataFolder.listFiles(new DataFilter())) {
            trace(file.getName());
            fileUsers.add(file.getName().replace(".yml", ""));
        }
    }

    private void loadUser(String username) {
        if (!users.containsKey(username) && fileUsers.contains(username)) {
            trace("loading " + username);
            File userFile = new File(new File(getDataFolder(), "data"), getFileName(username));
            YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(userFile);
            if (userConfig.contains("user")) {
                Object o = userConfig.get("user");
                if (o instanceof CKUser) {
                    users.put(username, (CKUser) o);
                    fileUsers.remove(username);
                } else {
                    logger.severe("Error loading user " + username + ", wasn't a ckuser object");
                }
            }
        }
    }

    /**
     * Get a user by a full username, and create them if they don't exist.
     * @param username Username to get
     * @return CKUser of the username
     */
    protected CKUser getUser(String username) {
        if (fileUsers.contains(username.toLowerCase())) {
            loadUser(username.toLowerCase());
        }
        if (users.containsKey(username.toLowerCase())) {
            return users.get(username.toLowerCase());
        } else {
            CKUser user = new CKUser(username.toLowerCase());
            users.put(username.toLowerCase(), user);
            return user;
        }
    }

    /**
     * Attempt to match a user by either a full or partial username.
     * @param username Username to match
     * @return null if no user was found, else the CKUser
     */
    protected CKUser matchUser(String username) {
        String matchedUser = null;
        for (String fileUser : fileUsers) {
            boolean matched = fileUser.contains(username.toLowerCase());
            if (matchedUser == null && matched) {
                matchedUser = fileUser;
            } else if (matchedUser != null && matched) {
                //Ended up matching two users
                return null;
            }
        }
        for (String memoryUser : users.keySet()) {
            boolean matched = fileUsers.contains(username.toLowerCase());
            if (matchedUser == null && matched) {
                matchedUser = memoryUser;
            } else if (matchedUser != null && matched) {
                return null;
            }
        }
        return matchedUser == null ? null : getUser(matchedUser);
    }

    /**
     * Add a modified CKUser to the io queue
     * @param user CKUser to save
     */
    protected void queueUser(CKUser user) {
        trace("queuing user " + user.getUsername());
        synchronized (ioQueue) {
            serializer.set("user", user);
            ioQueue.put(user.getUsername(), serializer.saveToString());
            ioQueue.notifyAll();
        }
    }
}
