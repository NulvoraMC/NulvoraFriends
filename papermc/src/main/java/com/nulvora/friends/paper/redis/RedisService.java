package com.nulvora.friends.paper.redis;

import com.nulvora.friends.common.dto.FriendDataPayload;
import com.nulvora.friends.common.dto.FriendNotificationPayload;
import com.nulvora.friends.common.dto.PartyDataPayload;
import com.nulvora.friends.common.redis.RedisProtocol;
import com.nulvora.friends.paper.NulvoraFriendsPaper;
import com.nulvora.friends.paper.cache.FriendCache;
import com.nulvora.friends.paper.cache.PartyCache;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisPubSub;

public final class RedisService implements AutoCloseable {
    private final NulvoraFriendsPaper plugin;
    private final JedisPooled client;
    private final String namespace;
    private final int database;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final Thread subscriberThread;
    private volatile Jedis subscriber;

    public RedisService(NulvoraFriendsPaper plugin) {
        this.plugin = plugin;
        host = plugin.getConfig().getString("redis.host", "localhost");
        port = plugin.getConfig().getInt("redis.port", 6379);
        username = plugin.getConfig().getString("redis.username", "");
        password = plugin.getConfig().getString("redis.password", "");
        database = plugin.getConfig().getInt("redis.database", 0);
        namespace = plugin.getConfig().getString("redis.namespace", "nulfriends");
        var builder = DefaultJedisClientConfig.builder().database(database);
        if (!username.isBlank()) builder.user(username);
        if (!password.isBlank()) builder.password(password);
        client = new JedisPooled(new HostAndPort(host, port), builder.build());
        client.ping();
        subscriberThread = Thread.ofPlatform().name("nulfriends-redis-events").daemon(true).start(this::subscribe);
    }

    public void publishRequest(String type, Object payload) {
        client.publish(namespace + ":requests", RedisProtocol.envelope(type, payload));
    }

    public void refresh(UUID playerId) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            applyCached(client.get(key(RedisProtocol.FRIEND_DATA, playerId)));
            applyCached(client.get(key(RedisProtocol.PARTY_DATA, playerId)));
        });
    }

    private String key(String kind, UUID playerId) { return namespace + ":cache:" + kind + ":" + playerId; }

    private void subscribe() {
        try (Jedis jedis = new Jedis(host, port)) {
            subscriber = jedis;
            if (!password.isBlank()) {
                if (username.isBlank()) jedis.auth(password); else jedis.auth(username, password);
            }
            jedis.select(database);
            jedis.subscribe(new JedisPubSub() {
                @Override public void onMessage(String channel, String message) { applyCached(message); }
            }, namespace + ":events");
        } catch (Exception e) {
            if (!Thread.currentThread().isInterrupted()) plugin.getLogger().severe("Redis subscription stopped: " + e.getMessage());
        }
    }

    private void applyCached(String json) {
        if (json == null) return;
        try {
            RedisProtocol.Message message = RedisProtocol.decode(json);
            switch (message.type()) {
                case RedisProtocol.FRIEND_DATA -> applyFriends(RedisProtocol.gson().fromJson(message.payload(), FriendDataPayload.class));
                case RedisProtocol.PARTY_DATA -> applyParty(RedisProtocol.gson().fromJson(message.payload(), PartyDataPayload.class));
                case RedisProtocol.FRIEND_NOTIFICATION -> notifyPlayer(RedisProtocol.gson().fromJson(message.payload(), FriendNotificationPayload.class));
                case RedisProtocol.OPEN_MENU -> openMenu(message.payload().get("player").getAsString());
                default -> { }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid Redis event: " + e.getMessage());
        }
    }

    private void applyFriends(FriendDataPayload payload) {
        List<FriendCache.CachedFriend> friends = payload.friends().stream().map(friend ->
            new FriendCache.CachedFriend(UUID.fromString(friend.uuid()), friend.name(), friend.online(), friend.server())).toList();
        plugin.friendCache().updateFriends(UUID.fromString(payload.playerUuid()), friends);
    }

    private void applyParty(PartyDataPayload payload) {
        UUID player = UUID.fromString(payload.playerUuid());
        if (payload.partyId() == null) { plugin.partyCache().remove(player); return; }
        List<PartyCache.CachedPartyMember> members = payload.members().stream().map(member ->
            new PartyCache.CachedPartyMember(UUID.fromString(member.uuid()), member.name(), member.online(), member.server())).toList();
        plugin.partyCache().updateParty(player, new PartyCache.CachedParty(
            UUID.fromString(payload.partyId()), UUID.fromString(payload.leaderUuid()), members));
    }

    private void notifyPlayer(FriendNotificationPayload payload) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Player player = plugin.getServer().getPlayer(UUID.fromString(payload.playerUuid()));
            if (player == null) return;
            String path = switch (payload.type()) {
                case "join" -> "messages.friend-join-network";
                case "leave" -> "messages.friend-leave-network";
                case "server_change" -> "messages.friend-server-change";
                default -> null;
            };
            if (path == null) return;
            String text = plugin.getConfig().getString(path, "")
                .replace("%player%", payload.friendName()).replace("%friend%", payload.friendName())
                .replace("%server%", payload.serverName() == null ? "" : payload.serverName());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', text));
        });
    }

    private void openMenu(String uuid) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Player player = plugin.getServer().getPlayer(UUID.fromString(uuid));
            if (player != null) plugin.friendsGUI().open(player);
        });
    }

    @Override public void close() {
        if (subscriber != null) subscriber.close();
        subscriberThread.interrupt();
        client.close();
    }
}
