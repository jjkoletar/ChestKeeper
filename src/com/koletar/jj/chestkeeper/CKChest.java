package com.koletar.jj.chestkeeper;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jjkoletar
 */
public class CKChest implements ConfigurationSerializable {
    private static final int SMALL_CHEST_SIZE = 27;
    private static final int LARGE_CHEST_SIZE = 54;
    private ItemStack[] contents;
    private Inventory inventory;
    private boolean modified;
    private String title;

    public CKChest(String title, boolean isLargeChest) {
        contents = new ItemStack[isLargeChest ? LARGE_CHEST_SIZE : SMALL_CHEST_SIZE];
        this.title = title;
    }

    public CKChest(Map<String, Object> me) {
        if (me.size() - 2 != SMALL_CHEST_SIZE && me.size() - 2 != LARGE_CHEST_SIZE) { //Minus two offsets for the == and _title
            throw new IllegalArgumentException("Size of item list is not the size of a large or small chest");
        }
        contents = new ItemStack[me.size() - 1];
        for (Map.Entry<String, Object> entry : me.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("_title")) {
                title = entry.getValue().toString();
                continue;
            }
            int i = -1;
            try {
                i = Integer.valueOf(entry.getKey());
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("A key wasn't an integer");
            }
            ItemStack is;
            try {
                is = (ItemStack) entry.getValue();
            } catch (ClassCastException cce) {
                throw new IllegalArgumentException("A value wasn't an itemstack");
            }
            try {
                contents[i] = is;
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                throw new IllegalArgumentException("A key was out of bounds with the array");
            }
        }
    }

    public Map<String, Object> serialize() {
        Map<String, Object> me = new HashMap<String, Object>();
        for (int i = 0; i < contents.length; i++) {
            me.put(String.valueOf(i), contents[i] == null ? new ItemStack(0) : contents[i]);
        }
        me.put("_title", title);
        return me;
    }

    public Inventory getInventory(int magic) {
        if (inventory == null) {
            inventory = Bukkit.createInventory(null, contents.length, title + makeMagic(magic));
            ChestKeeper.trace("Title is: " + title + makeMagic(magic));
        }
        if (modified) {
            return inventory;
        }
        inventory.setContents(contents);
        modified = true;
        return inventory;
    }

    public boolean save() {
        if (inventory == null || inventory.getViewers().size() > 1) {
            return false;
        }
        if (!modified) {
            return true;
        }
        contents = inventory.getContents();
        modified = false;
        return true;
    }

    public boolean isModified() {
        return modified;
    }

    public void kick() {
        if (inventory != null) {
            for (HumanEntity he : inventory.getViewers()) {
                he.closeInventory();
            }
        }
    }

    private static String makeMagic(int magic) {
        StringBuilder sb = new StringBuilder();
        char[] digits = String.valueOf(magic).toCharArray();
        for (int i = 0; i < digits.length; i++) {
            sb.append("\u00A7");
            sb.append(digits[i]);
        }
        return sb.toString();
    }
}
