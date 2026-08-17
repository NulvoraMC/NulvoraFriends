package com.nulvora.friends.velocity.messaging;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nulvora.friends.common.dto.JoinRequestPayload;
import com.nulvora.friends.common.messaging.Channel;
import com.nulvora.friends.velocity.NulvoraFriendsPlugin;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.Player;
import java.util.UUID;

public class PluginMessageListener {

    private final NulvoraFriendsPlugin plugin;

    public PluginMessageListener(NulvoraFriendsPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().getId().equals(Channel.CHANNEL_NAME)) return;
        if (!(event.getSource() instanceof Player player)) return;

        String raw = Channel.decode(event.getData());
        JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
        String type = json.get("type").getAsString();

        switch (type) {
            case Channel.MSG_JOIN_REQUEST -> handleJoinRequest(player, json.get("payload").toString());
            case Channel.MSG_PING -> handlePing(player);
        }
    }

    private void handleJoinRequest(Player player, String payload) {
        JoinRequestPayload request = Channel.gson().fromJson(payload, JoinRequestPayload.class);
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
        player.sendPluginMessage(MinecraftChannelIdentifier.from(Channel.CHANNEL_NAME), pong);
    }
}
