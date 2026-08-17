package com.nulvora.friends.velocity.party;

import com.nulvora.friends.velocity.NulvoraFriendsPlugin;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;

public class PartyListener {

    private final NulvoraFriendsPlugin plugin;

    public PartyListener(NulvoraFriendsPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        if (plugin.partyService() == null) return;
        Player player = event.getPlayer();
        plugin.partyService().handleLogin(player.getUniqueId());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (plugin.partyService() == null) return;
        Player player = event.getPlayer();
        plugin.partyService().handleDisconnect(player.getUniqueId());
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        if (plugin.partyService() == null) return;
        Player player = event.getPlayer();
        plugin.partyService().getParty(player.getUniqueId()).ifPresent(party -> {
            plugin.partyService().pushPartyDataToAll(party);
        });
    }
}
