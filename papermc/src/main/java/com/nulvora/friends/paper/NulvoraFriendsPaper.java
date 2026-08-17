package com.nulvora.friends.paper;

import com.nulvora.friends.paper.cache.FriendCache;
import com.nulvora.friends.paper.gui.FriendsGUI;
import com.nulvora.friends.paper.listener.NotificationListener;
import com.nulvora.friends.paper.messaging.FriendMessageListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class NulvoraFriendsPaper extends JavaPlugin {

    private FriendCache friendCache;
    private FriendsGUI friendsGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        friendCache = new FriendCache();
        friendsGUI = new FriendsGUI(this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "nulfriends:main");
        getServer().getMessenger().registerIncomingPluginChannel(this, "nulfriends:main", new FriendMessageListener(this));

        getServer().getPluginManager().registerEvents(new NotificationListener(this), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.nulvora.friends.paper.placeholder.FriendsPlaceholder(this).register();
            getLogger().info("PlaceholderAPI integration enabled.");
        }

        getCommand("amigos").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                friendsGUI.open(player);
            } else {
                sender.sendMessage("Este comando solo puede ser ejecutado por jugadores.");
            }
            return true;
        });

        getLogger().info("NulvoraFriends-Paper enabled.");
    }

    @Override
    public void onDisable() {
        if (friendCache != null) {
            friendCache.clear();
        }
        getLogger().info("NulvoraFriends-Paper disabled.");
    }

    public FriendCache friendCache() { return friendCache; }
    public FriendsGUI friendsGUI() { return friendsGUI; }
}
