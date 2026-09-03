package com.nulvora.friends.velocity.presence;

import com.nulvora.friends.common.dto.FriendDataPayload;
import com.nulvora.friends.common.dto.FriendNotificationPayload;
import com.nulvora.friends.common.dto.JoinRequestPayload;
import com.nulvora.friends.common.redis.RedisProtocol;
import com.nulvora.friends.velocity.NulvoraFriendsPlugin;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PresenceListener {

    private final NulvoraFriendsPlugin plugin;
    public PresenceListener(NulvoraFriendsPlugin plugin) {
        this.plugin = plugin;
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
                plugin.redis().cacheAndPublish(RedisProtocol.FRIEND_DATA, uuid, payload);
            });
        });
    }

    public void sendNotification(Player target, String type, UUID friendUuid, String friendName, String server) {
        FriendNotificationPayload payload = new FriendNotificationPayload(
            target.getUniqueId().toString(), type, friendUuid.toString(), friendName, server);
        plugin.redis().publish(RedisProtocol.FRIEND_NOTIFICATION, payload);
    }

    public void sendOpenMenu(Player player) {
        plugin.redis().publish(RedisProtocol.OPEN_MENU,
            java.util.Map.of("player", player.getUniqueId().toString()));
    }

    public void sendJoinRequest(Player target, UUID requesterUuid) {
        plugin.db().getName(requesterUuid).thenAccept(name -> {
            if (name != null) {
                JoinRequestPayload payload = new JoinRequestPayload(target.getUniqueId().toString(), requesterUuid.toString());
                plugin.redis().publish(RedisProtocol.JOIN_REQUEST, payload);
            }
        });
    }
}
