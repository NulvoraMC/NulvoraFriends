package com.nulvora.friends.paper.placeholder;

import com.nulvora.friends.paper.NulvoraFriendsPaper;
import org.bukkit.Bukkit;

public final class PlaceholderAPIHook {

    private PlaceholderAPIHook() {
    }

    public static void register(NulvoraFriendsPaper plugin) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            new FriendsPlaceholder(plugin).register();
            plugin.getLogger().info("PlaceholderAPI integration enabled.");
        } catch (NoClassDefFoundError e) {
            plugin.getLogger().info("PlaceholderAPI classes not available, skipping placeholder registration.");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register PlaceholderAPI placeholders: " + e.getMessage());
        }
    }
}
