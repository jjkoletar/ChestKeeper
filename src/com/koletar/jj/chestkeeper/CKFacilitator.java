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
    private Map<String, CKUser> openChests;

    public CKFacilitator(ChestKeeper plugin) {
        this.plugin = plugin;
        openChests = new HashMap<String, CKUser>();
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
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("b") || args[0].equalsIgnoreCase("buy")) {
                    if (!validatePlayer(sender)) {
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("small") || args[1].equalsIgnoreCase("normal")) {
                        buyChest((Player) sender,  false);
                    } else if (args[1].equalsIgnoreCase("large") || args[1].equalsIgnoreCase("double")) {
                        buyChest((Player) sender, true);
                    }
                }
            }
        }
        return false;
    }

    private void openDefaultChest(Player p) {
        openDefaultChest(p, plugin.getUser(p.getName()));
    }

    private void openDefaultChest(Player p, CKUser user) {
        openChest(p, user, user.openChest());
    }

    private void openChest(Player p, CKUser user, Inventory chest) {
        trace("Sending player " + p.getName() + " " + user.getUsername() + "'s " + chest.getName());
        openChests.put(chest.getTitle(), user);
        p.openInventory(chest);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        boolean isOurs = openChests.containsKey(inventory.getTitle());
        boolean isOutOfView = inventory.getViewers().size() - 1 == 0;
        trace("Inventory closed: " + inventory.getName() + ", isOurs: " + isOurs + ", isOutOfView: " + isOutOfView);
        if (isOurs && isOutOfView) {
            if (openChests.get(inventory.getTitle()).save(inventory)) {
                trace("Save successful, queueing");
                plugin.queueUser(openChests.get(inventory.getTitle()));
            } else {
                trace("Save failed");
            }
        }
    }

    private void buyChest(Player p, boolean isLargeChest) {
        //TODO: economy & success message
        plugin.getUser(p.getName()).createChest(isLargeChest);
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
