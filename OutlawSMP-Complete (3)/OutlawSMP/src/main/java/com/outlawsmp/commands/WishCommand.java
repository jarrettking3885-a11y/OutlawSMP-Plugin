package com.outlawsmp.commands;

import com.outlawsmp.OutlawSMP;
import com.outlawsmp.player.Blessing;
import com.outlawsmp.player.PlayerData;
import com.outlawsmp.player.Wish;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class WishCommand implements CommandExecutor, TabCompleter {

    private final OutlawSMP plugin;

    public WishCommand(OutlawSMP plugin) {
        this.plugin = plugin;
    }

    private String prefix() {
        return ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("prefix", "&c&lOutlaw&6&lSMP &8» &r"));
    }

    private ChatColor rarityColor(Blessing blessing) {
        return switch (blessing.getRarity()) {
            case COMMON -> ChatColor.GRAY;
            case RARE -> ChatColor.AQUA;
            case EPIC -> ChatColor.LIGHT_PURPLE;
            case LEGENDARY -> ChatColor.GOLD;
        };
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can do that.");
            return true;
        }

        PlayerData data = plugin.getPlayerManager().getCached(player);
        if (data == null) {
            player.sendMessage(prefix() + ChatColor.RED + "Your data is still loading, try again in a moment.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("gui")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("gui")) {
                plugin.getBlessingsGUI().open(player);
                return true;
            }
            showList(player, data);
            return true;
        }

        if (args[0].equalsIgnoreCase("redeem")) {
            if (!plugin.getWishManager().isNearShrine(player.getLocation())) {
                player.sendMessage(prefix() + ChatColor.RED + "You must be at the Wish Shrine to redeem tokens.");
                return true;
            }
            List<Wish> redeemed = plugin.getWishManager().redeemAll(player, data);
            if (redeemed.isEmpty()) {
                player.sendMessage(prefix() + ChatColor.GRAY + "You aren't carrying any unredeemed Wish Tokens.");
                return true;
            }
            player.sendMessage(prefix() + ChatColor.GREEN + "Redeemed " + redeemed.size() + " Wish(es):");
            for (Wish wish : redeemed) {
                player.sendMessage(ChatColor.GRAY + "  - " + rarityColor(wish.getBlessing()) + wish.getBlessing().getDisplayName());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("activate") || args[0].equalsIgnoreCase("deactivate")) {
            if (args.length < 2) {
                player.sendMessage(prefix() + ChatColor.RED + "Usage: /wish " + args[0].toLowerCase() + " <index>");
                return true;
            }
            if (!plugin.getWishManager().isNearSpawn(player)) {
                player.sendMessage(prefix() + ChatColor.RED + "You must be near spawn to change your active blessings.");
                return true;
            }

            int index;
            try {
                index = Integer.parseInt(args[1]) - 1;
            } catch (NumberFormatException e) {
                player.sendMessage(prefix() + ChatColor.RED + "That's not a valid Wish number.");
                return true;
            }

            List<Wish> wishes = data.getWishes();
            if (index < 0 || index >= wishes.size()) {
                player.sendMessage(prefix() + ChatColor.RED + "You don't have a Wish #" + (index + 1) + ".");
                return true;
            }

            Wish wish = wishes.get(index);

            if (args[0].equalsIgnoreCase("activate")) {
                if (data.isActive(wish)) {
                    player.sendMessage(prefix() + ChatColor.YELLOW + "That blessing is already active.");
                    return true;
                }
                boolean ok = data.activate(wish, plugin.getWishManager().maxActiveBlessings());
                if (!ok) {
                    player.sendMessage(prefix() + ChatColor.RED + "You already have "
                            + plugin.getWishManager().maxActiveBlessings() + " active blessings. Deactivate one first.");
                    return true;
                }
                wish.getBlessing().apply(player);
                player.sendMessage(prefix() + ChatColor.GREEN + "Activated " + rarityColor(wish.getBlessing())
                        + wish.getBlessing().getDisplayName() + ChatColor.GREEN + "!");
            } else {
                if (!data.isActive(wish)) {
                    player.sendMessage(prefix() + ChatColor.YELLOW + "That blessing isn't active.");
                    return true;
                }
                wish.getBlessing().remove(player);
                data.deactivate(wish);
                player.sendMessage(prefix() + ChatColor.YELLOW + "Deactivated " + rarityColor(wish.getBlessing())
                        + wish.getBlessing().getDisplayName() + ChatColor.YELLOW + ".");
            }
            return true;
        }

        player.sendMessage(prefix() + ChatColor.RED + "Usage: /wish [list|gui|redeem|activate <#>|deactivate <#>]");
        return true;
    }

    private void showList(Player player, PlayerData data) {
        List<Wish> wishes = data.getWishes();
        player.sendMessage(prefix() + ChatColor.GOLD + "Your Wishes (" + wishes.size() + "):");
        if (wishes.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "  You have no Wishes. All blessings are lost until you steal one back.");
            return;
        }
        for (int i = 0; i < wishes.size(); i++) {
            Wish wish = wishes.get(i);
            boolean active = data.isActive(wish);
            ChatColor color = rarityColor(wish.getBlessing());
            String status = active ? ChatColor.GREEN + "[ACTIVE]" : ChatColor.DARK_GRAY + "[inactive]";
            player.sendMessage(ChatColor.GRAY + "  #" + (i + 1) + " " + color + wish.getBlessing().getDisplayName()
                    + ChatColor.GRAY + " (" + wish.getBlessing().getRarity().name() + ") " + status);
        }
        player.sendMessage(ChatColor.GRAY + "Active: " + data.getActiveWishes().size() + "/"
                + plugin.getWishManager().maxActiveBlessings()
                + ChatColor.GRAY + " — use /wish activate <#> or /wish deactivate <#> near spawn.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("list");
            options.add("gui");
            options.add("redeem");
            options.add("activate");
            options.add("deactivate");
        } else if (args.length == 2 && sender instanceof Player player) {
            PlayerData data = plugin.getPlayerManager().getCached(player);
            if (data != null) {
                for (int i = 1; i <= data.getWishes().size(); i++) {
                    options.add(String.valueOf(i));
                }
            }
        }
        return options;
    }
}
