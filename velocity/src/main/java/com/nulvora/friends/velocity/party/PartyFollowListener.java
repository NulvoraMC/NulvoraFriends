package com.nulvora.friends.velocity.party;

import com.nulvora.friends.velocity.NulvoraFriendsPlugin;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import java.util.Optional;

public class PartyFollowListener {

    private final NulvoraFriendsPlugin plugin;

    public PartyFollowListener(NulvoraFriendsPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        if (plugin.partyService() == null) return;
        if (!plugin.config().party().follow().enabled()) return;

        Player player = event.getPlayer();

        Optional<String> previousServer = event.getPreviousServer()
            .map(sc -> sc.getServerInfo().getName());
        String targetServer = event.getServer().getServerInfo().getName();

        if (previousServer.isEmpty()) return;

        String from = previousServer.get();

        if (!plugin.config().serverNames().containsKey(from)) return;
        if (!plugin.config().serverNames().containsKey(targetServer)) return;

        if (!plugin.partyService().canFollow(player.getUniqueId())) return;

        String displayName = plugin.config().getServerName(targetServer);
        plugin.partyService().warpMembers(player.getUniqueId(), targetServer, displayName);
    }
}
