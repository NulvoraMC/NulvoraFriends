package com.nulvora.friends.paper.listener;

import com.nulvora.friends.paper.NulvoraFriendsPaper;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class NotificationListener implements Listener {

    private final NulvoraFriendsPaper plugin;

    public NotificationListener(NulvoraFriendsPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.redis().refresh(player.getUniqueId());
        plugin.getConfig().getString("sounds.friend-join", "");
        String soundName = plugin.getConfig().getString("sounds.friend-join", "");
        if (!soundName.isEmpty()) {
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}
