package com.outlawsmp.gui;

import com.outlawsmp.OutlawSMP;
import com.outlawsmp.player.Blessing;
import com.outlawsmp.player.PlayerData;
import com.outlawsmp.player.Wish;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple Blessings GUI for viewing and toggling active Wishes.
 */
public class BlessingsGUI {

    private final OutlawSMP plugin;

    public BlessingsGUI(OutlawSMP plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        PlayerData data = plugin.getPlayerManager().getCached(player);
        if (data == null) {
            player.sendMessage(ChatColor.RED + "Data not loaded yet.");
            return;
        }

        int size = 9 * Math.max(1, (data.getWishes().size() + 8) / 9);
        Inventory gui = Bukkit.createInventory(null, size, ChatColor.GOLD + "Your Blessings");

        List<Wish> wishes = data.getWishes();
        for (int i = 0; i < wishes.size(); i++) {
            Wish wish = wishes.get(i);
            boolean active = data.isActive(wish);
            Blessing blessing = wish.getBlessing();

            ItemStack item = new ItemStack(Material.NETHER_STAR);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName((active ? ChatColor.GREEN : ChatColor.GRAY) + blessing.getDisplayName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Rarity: " + blessing.getRarity().name());
                lore.add("");
                lore.add(active ? ChatColor.GREEN + "▶ ACTIVE" : ChatColor.YELLOW + "Click to activate");
                lore.add(ChatColor.GRAY + "Slot: " + (i + 1));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            gui.setItem(i, item);
        }

        player.openInventory(gui);
        // Listener would be added in main class for click handling (future)
    }
}
