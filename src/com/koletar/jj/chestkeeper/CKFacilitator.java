package com.koletar.jj.chestkeeper;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

import static com.koletar.jj.chestkeeper.ChestKeeper.trace;

/**
 * @author jjkoletar
 */
public class CKFacilitator implements CommandExecutor, Listener {
    private ChestKeeper plugin;
    private Map<Inventory, CKUser> openChests;

    public CKFacilitator(ChestKeeper plugin) {
        this.plugin = plugin;
        openChests = new HashMap<Inventory, CKUser>();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("chestkeeper")) {
            if (args.length == 0) {
                //todo: help
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("c") || args[0].equalsIgnoreCase("o") || args[0].equalsIgnoreCase("open")) {
                    if (!validatePlayer(sender)) {
                        return true;
                    }
                    openDefaultChest((Player) sender);
                }
            }
        }
        return false;
    }

    private void openDefaultChest(Player p) {
        openDefaultChest(p, plugin.getUser(p.getName()));
    }

    private void openDefaultChest(Player p, CKUser user) {
        openChest(p, user, user.getChest());
    }

    private void openChest(Player p, CKUser user, Inventory chest) {
        trace("Sending player " + p.getName() + " " + user.getUsername() + "'s " + chest.getName());
        openChests.put(chest, user);
        p.openInventory(chest);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        boolean isOurs = openChests.containsKey(inventory);
        boolean isOutOfView = inventory.getViewers().size() == 0;
        trace("Inventory closed: " + inventory.getName() + ", isOurs: " + isOurs + ", isOutOfView: " + isOutOfView);
        if (isOurs && isOutOfView) {
            if (openChests.get(inventory).save(inventory)) {
                trace("Save successful, queueing");
                //TODO: add ckuser to the ioqueue
            } else {
                trace("Save failed");
            }
        }
    }

    private static String[] trimArgs(String[] in) {
        String[] out = new String[in.length - 1];
        System.arraycopy(in, 1, out, 0, in.length - 1);
        return out;
    }

    private static boolean validatePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players may use this command.");
            return false;
        }
        return true;
    }
}
