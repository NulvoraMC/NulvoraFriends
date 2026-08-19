package com.nulvora.friends.velocity.messaging;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nulvora.friends.common.dto.JoinRequestPayload;
import com.nulvora.friends.common.dto.PartyWarpRequestPayload;
import com.nulvora.friends.common.dto.extension.CommandResponsePayload;
import com.nulvora.friends.common.dto.extension.RegisterCommandPayload;
import com.nulvora.friends.common.dto.extension.UnregisterCommandsPayload;
import com.nulvora.friends.common.messaging.Channel;
import com.nulvora.friends.velocity.NulvoraFriendsPlugin;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.Player;
import java.util.Optional;
import java.util.UUID;

public class PluginMessageListener {

    private final NulvoraFriendsPlugin plugin;

    public PluginMessageListener(NulvoraFriendsPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().getId().equals(Channel.CHANNEL_NAME)) return;

        Player player;
        if (event.getSource() instanceof Player p) {
            player = p;
        } else if (event.getSource() instanceof ServerConnection sc) {
            player = sc.getPlayer();
        } else {
            return;
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        String raw = Channel.decode(event.getData());
        JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
        String type = json.get("type").getAsString();

        switch (type) {
            case Channel.MSG_JOIN_REQUEST -> handleJoinRequest(player, json.get("payload").toString());
            case Channel.MSG_PING -> handlePing(player);
            case Channel.MSG_EXT_REGISTER -> handleRegister(player, json.get("payload").toString());
            case Channel.MSG_EXT_UNREGISTER -> handleUnregister(json.get("payload").toString());
            case Channel.MSG_EXT_RESPONSE -> handleResponse(json.get("payload").toString());
            case Channel.MSG_PARTY_WARP -> handlePartyWarp(json.get("payload").toString());
        }
    }

    private void handleRegister(Player player, String payload) {
        if (plugin.extensionRegistry() == null) return;
        RegisterCommandPayload register = Channel.gson().fromJson(payload, RegisterCommandPayload.class);
        Optional<ServerConnection> serverConnection = player.getCurrentServer();
        if (serverConnection.isEmpty()) {
            plugin.logger().warn("Registro de /" + register.spec().name()
                + " ignorado: no se pudo determinar el servidor de origen.");
            return;
        }

        String serverName = serverConnection.get().getServerInfo().getName();
        plugin.extensionRegistry().handleRegister(register, serverName, player);
    }

    private void handleUnregister(String payload) {
        if (plugin.extensionRegistry() == null) return;
        UnregisterCommandsPayload unregister = Channel.gson().fromJson(payload, UnregisterCommandsPayload.class);
        plugin.extensionRegistry().handleUnregister(unregister);
    }

    private void handleResponse(String payload) {
        if (plugin.extensionRegistry() == null) return;
        CommandResponsePayload response = Channel.gson().fromJson(payload, CommandResponsePayload.class);
        plugin.extensionRegistry().handleResponse(response);
    }

    private void handleJoinRequest(Player player, String payload) {
        JoinRequestPayload request =
            Channel.gson().fromJson(payload, JoinRequestPayload.class);
        UUID target = UUID.fromString(request.targetUuid());
        UUID requester = UUID.fromString(request.playerUuid());

        plugin.proxy().getPlayer(target).ifPresent(targetPlayer ->
            targetPlayer.getCurrentServer().ifPresent(sc ->
                plugin.proxy().getServer(sc.getServerInfo().getName()).ifPresent(server -> {
                    player.createConnectionRequest(server).fireAndForget();
                    plugin.db().getName(requester).thenAccept(name -> {
                        if (name != null) {
                            player.sendMessage(
                                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                                    .legacyAmpersand()
                                    .deserialize("&aConectandote al servidor de &e" + name + "&a...")
                            );
                        }
                    });
                })
            )
        );
    }

    private void handlePing(Player player) {
        byte[] pong = Channel.encode(Channel.MSG_PONG, "{}");
        player.getCurrentServer().ifPresent(sc ->
            sc.sendPluginMessage(MinecraftChannelIdentifier.from(Channel.CHANNEL_NAME), pong));
    }

    private void handlePartyWarp(String payload) {
        if (plugin.partyService() == null) return;

        PartyWarpRequestPayload request =
            Channel.gson().fromJson(payload, PartyWarpRequestPayload.class);
        UUID leaderUuid = UUID.fromString(request.requesterUuid());

        plugin.proxy().getPlayer(leaderUuid).ifPresent(leader -> {
            if (!plugin.partyService().isLeader(leaderUuid)) return;

            String currentServer = leader.getCurrentServer()
                .map(sc -> sc.getServerInfo().getName())
                .orElse(null);
            if (currentServer == null) return;

            String displayName = plugin.config().getServerName(currentServer);
            plugin.partyService().warpMembers(leaderUuid, currentServer, displayName);
        });
    }
}
