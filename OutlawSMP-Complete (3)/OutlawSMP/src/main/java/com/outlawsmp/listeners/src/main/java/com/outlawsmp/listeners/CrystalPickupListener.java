package com.outlawsmp.listeners;
 
import com.outlawsmp.OutlawSMP;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
 
/**
 * Intercepts pickup of the physical Wish Crystal item spawned by
 * {@link com.outlawsmp.managers.CrystalManager}.
 *
 * The crystal item is built on a real {@code Material.END_CRYSTAL}, which is
 * a placeable, explosive vanilla item - it must never be allowed to actually
 * enter a player's inventory as an ordinary item, or whoever grabs it first
 * can place and detonate it. This listener cancels the pickup, removes the
 * item entity from the world, and grants the intended reward directly via
 * {@link com.outlawsmp.managers.CrystalManager#claim}.
 *
 * Previously nothing called {@code CrystalManager.claim()} at all, so Wish
 * Crystals spawned, broadcast their coordinates, and showed a boss bar, but
 * picking one up did nothing except hand the player a live End Crystal item.
 */
public class CrystalPickupListener implements Listener {
 
    private final OutlawSMP plugin;
 
    public CrystalPickupListener(OutlawSMP plugin) {
        this.plugin = plugin;
    }
 
    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
 
        ItemStack item = event.getItem().getItemStack();
        if (!plugin.getCrystalManager().isCrystal(item)) {
            return;
        }
 
        // Stop the raw, explosive End Crystal item from ever reaching the
        // player's inventory, and remove it from the world so a second
        // player can't also grab it in the same tick.
        event.setCancelled(true);
        event.getItem().remove();
 
        plugin.getCrystalManager().claim(player);
    }
}
