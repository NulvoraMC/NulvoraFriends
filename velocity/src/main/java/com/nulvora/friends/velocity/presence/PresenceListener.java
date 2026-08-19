package com.nulvora.friends.velocity.presence;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nulvora.friends.common.dto.FriendDataPayload;
import com.nulvora.friends.common.dto.FriendNotificationPayload;
import com.nulvora.friends.common.dto.JoinRequestPayload;
import com.nulvora.friends.common.messaging.Channel;
import com.nulvora.friends.velocity.NulvoraFriendsPlugin;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PresenceListener {

    private final NulvoraFriendsPlugin plugin;
    private final MinecraftChannelIdentifier channel;

    public PresenceListener(NulvoraFriendsPlugin plugin) {
        this.plugin = plugin;
        this.channel = MinecraftChannelIdentifier.from(Channel.CHANNEL_NAME);
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        plugin.db().upsertPlayer(player.getUniqueId(), player.getUsername());
        plugin.db().updateLastSeen(player.getUniqueId());

        plugin.db().getFriendIds(player.getUniqueId()).thenAccept(friendIds -> {
            for (UUID friendId : friendIds) {
                plugin.proxy().getPlayer(friendId).ifPresent(friend -> {
                    sendFriendDataToBackend(friend);
                    plugin.db().getName(player.getUniqueId()).thenAccept(name -> {
                        if (name != null) sendNotification(friend, "join", player.getUniqueId(), name, null);
                    });
                });
            }
            sendFriendDataToBackend(player);
        });
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        plugin.db().updateLastSeen(player.getUniqueId());
        plugin.db().getFriendIds(player.getUniqueId()).thenAccept(friendIds -> {
            for (UUID friendId : friendIds) {
                plugin.proxy().getPlayer(friendId).ifPresent(friend -> {
                    sendFriendDataToBackend(friend);
                    plugin.db().getName(player.getUniqueId()).thenAccept(name -> {
                        if (name != null) sendNotification(friend, "leave", player.getUniqueId(), name, null);
                    });
                });
            }
        });
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String serverName = event.getServer().getServerInfo().getName();
        plugin.db().getFriendIds(player.getUniqueId()).thenAccept(friendIds -> {
            for (UUID friendId : friendIds) {
                plugin.proxy().getPlayer(friendId).ifPresent(friend -> {
                    sendFriendDataToBackend(friend);
                    plugin.db().getName(player.getUniqueId()).thenAccept(name -> {
                        if (name != null) sendNotification(friend, "server_change", player.getUniqueId(), name, serverName);
                    });
                });
            }
            sendFriendDataToBackend(player);
        });
    }

    public void sendFriendDataToBackend(Player player) {
        UUID uuid = player.getUniqueId();
        plugin.db().getFriendIds(uuid).thenAccept(friendIds -> {
            List<CompletableFuture<FriendDataPayload.FriendEntry>> futures = new ArrayList<>();
            for (UUID id : friendIds) {
                futures.add(plugin.db().getName(id).thenApply(name -> {
                    Optional<Player> onlineFriend = plugin.proxy().getPlayer(id);
                    String server = onlineFriend
                        .flatMap(Player::getCurrentServer)
                        .map(sc -> sc.getServerInfo().getName())
                        .orElse(null);
                    boolean online = onlineFriend.isPresent();
                    return new FriendDataPayload.FriendEntry(id.toString(), name, online, server);
                }));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
                List<FriendDataPayload.FriendEntry> entries = futures.stream()
                    .map(CompletableFuture::join).toList();
                FriendDataPayload payload = new FriendDataPayload(uuid.toString(), entries);
                byte[] data = Channel.encode(Channel.MSG_FRIEND_DATA, Channel.toJson(payload));
                player.getCurrentServer().ifPresent(sc -> sc.sendPluginMessage(channel, data));
            });
        });
    }

    public void sendNotification(Player target, String type, UUID friendUuid, String friendName, String server) {
        FriendNotificationPayload payload = new FriendNotificationPayload(
            target.getUniqueId().toString(), type, friendUuid.toString(), friendName, server);
        byte[] data = Channel.encode(Channel.MSG_FRIEND_NOTIFICATION, Channel.toJson(payload));
        target.getCurrentServer().ifPresent(sc -> sc.sendPluginMessage(channel, data));
    }

    public void sendOpenMenu(Player player) {
        byte[] data = Channel.encode("open_menu", "{}");
        player.getCurrentServer().ifPresent(sc -> sc.sendPluginMessage(channel, data));
    }

    public void sendJoinRequest(Player target, UUID requesterUuid) {
        plugin.db().getName(requesterUuid).thenAccept(name -> {
            if (name != null) {
                JoinRequestPayload payload = new JoinRequestPayload(target.getUniqueId().toString(), requesterUuid.toString());
                byte[] data = Channel.encode(Channel.MSG_JOIN_REQUEST, Channel.toJson(payload));
                target.getCurrentServer().ifPresent(sc -> sc.sendPluginMessage(channel, data));
            }
        });
    }
}
