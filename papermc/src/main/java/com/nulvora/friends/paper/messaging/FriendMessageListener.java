package com.nulvora.friends.paper.messaging;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nulvora.friends.paper.NulvoraFriendsPaper;
import com.nulvora.friends.paper.cache.FriendCache;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class FriendMessageListener implements PluginMessageListener {

    private final NulvoraFriendsPaper plugin;

    public FriendMessageListener(NulvoraFriendsPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!channel.equals("nulfriends:main")) return;

        String raw = new String(message);
        try {
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            String type = json.get("type").getAsString();
            JsonObject payload = json.getAsJsonObject("payload");

            switch (type) {
                case "friends_data" -> handleFriendData(player, payload);
                case "friend_notification" -> handleNotification(player, payload);
                case "open_menu" -> plugin.friendsGUI().open(player);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error processing plugin message: " + e.getMessage());
        }
    }

    private void handleFriendData(Player player, JsonObject payload) {
        JsonArray friends = payload.getAsJsonArray("friends");
        List<FriendCache.CachedFriend> list = new ArrayList<>();

        for (int i = 0; i < friends.size(); i++) {
            JsonObject entry = friends.get(i).getAsJsonObject();
            UUID uuid = UUID.fromString(entry.get("uuid").getAsString());
            String name = entry.get("name").getAsString();
            boolean online = entry.get("online").getAsBoolean();
            String server = entry.has("server") && !entry.get("server").isJsonNull()
                ? entry.get("server").getAsString() : null;
            list.add(new FriendCache.CachedFriend(uuid, name, online, server));
        }

        plugin.friendCache().updateFriends(player.getUniqueId(), list);
    }

    private void handleNotification(Player player, JsonObject payload) {
        String type = payload.get("type").getAsString();
        String friendName = payload.get("friend_name").getAsString();
        String server = payload.has("server") && !payload.get("server").isJsonNull()
            ? payload.get("server").getAsString() : null;

        String message;
        switch (type) {
            case "join" -> message = colorize(
                plugin.getConfig().getString("messages.friend-join-network", "&a&l+ &e%player% &ase ha conectado a la red.")
                    .replace("%player%", friendName));
            case "leave" -> message = colorize(
                plugin.getConfig().getString("messages.friend-leave-network", "&c&l- &e%player% &ase ha desconectado.")
                    .replace("%player%", friendName));
            case "server_change" -> message = colorize(
                plugin.getConfig().getString("messages.friend-server-change", "&e%friend% &ahas entrado a &e%server%&a.")
                    .replace("%friend%", friendName)
                    .replace("%server%", server != null ? server : ""));
            default -> message = null;
        }

        if (message != null) {
            player.sendMessage(message);
        }
    }

    private String colorize(String message) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}
