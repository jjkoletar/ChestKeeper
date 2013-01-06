package com.koletar.jj.chestkeeper;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author jjkoletar
 */
public class CKUser implements ConfigurationSerializable {
    private String username;
    private String defaultChest;
    private SortedMap<String, CKChest> chests;
    private HashMap<Inventory, CKChest> inventoryPairings;

    public CKUser(String username) {
        this.username = username;
    }

    public CKUser(Map<String, Object> me) {
        chests = new TreeMap<String, CKChest>();
        inventoryPairings = new HashMap<Inventory, CKChest>();
        for (Map.Entry<String, Object> entry : me.entrySet()) {
            if (entry.getKey().equals("defaultChest")) {
                defaultChest = entry.getValue() == null ? "" : entry.getValue().toString();
            } else if (entry.getKey().equals("username")) {
                username = entry.getValue().toString();
            } else if (entry.getValue() instanceof CKChest) {
                chests.put(entry.getKey(), (CKChest) entry.getValue());
            }
        }
    }

    public Map<String, Object> serialize() {
        Map<String, Object> me = new HashMap<String, Object>();
        me.put("defaultChest", defaultChest);
        me.put("username", username);
        for (Map.Entry<String, CKChest> entry : chests.entrySet()) {
            me.put(entry.getKey(), entry.getValue());
        }
        return me;
    }

    public boolean equals(Object o) {
        return o instanceof CKUser && ((CKUser) o).username.equals(username);
    }

    public int hashCode() {
        return username.hashCode() + 1;
    }

    public boolean createChest(boolean isLargeChest) {
        return createChest(isLargeChest ? "large" + (chests.size() + 1) : "normal" + (chests.size() + 1), isLargeChest);
    }

    public boolean createChest(String name, boolean isLargeChest) {
        if (name.equalsIgnoreCase("defaultChest") || name.equalsIgnoreCase("username") || chests.containsKey(name.toLowerCase())) {
            return false;
        }
        chests.put(name.toLowerCase(), new CKChest(isLargeChest));
        return true;
    }

    public Inventory getChest() {                                //TODO: remember to validate renaming the default chest
        String key = (defaultChest == null || defaultChest.equals("")) && chests.size() > 0 ? chests.firstKey() : defaultChest;
        return getChest(key);
    }

    public Inventory getChest(String name) {
        String lowerName = name.toLowerCase();
        CKChest chest = chests.get(lowerName);
        if (chest == null) {
            return null;
        }
        Inventory inventory = chest.getInventory(lowerName);
        inventoryPairings.put(inventory, chest);
        return inventory;
    }

    public boolean save(Inventory inventory) {
        if (inventoryPairings.containsKey(inventory)) {
            return inventoryPairings.get(inventory).save();
        }
        return false;
    }

    public boolean isModified() {
        boolean combo = true;
        for (CKChest chest : inventoryPairings.values()) {
            if (chest.isModified()) {
                combo = false;
            }
        }
        return combo;
    }

    public void forceClean() {
        for (CKChest chest : inventoryPairings.values()) {
            chest.kick();
            chest.save();
        }
    }

    public String getUsername() {
        return username;
    }
}
