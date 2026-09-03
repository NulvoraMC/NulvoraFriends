package com.nulvora.friends.velocity.redis;

import com.nulvora.friends.common.dto.JoinRequestPayload;
import com.nulvora.friends.common.dto.PartyWarpRequestPayload;
import com.nulvora.friends.common.redis.RedisProtocol;
import com.nulvora.friends.velocity.NulvoraFriendsPlugin;
import java.util.UUID;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.JedisPubSub;

public final class RedisService implements AutoCloseable {
    private final NulvoraFriendsPlugin plugin;
    private final JedisPooled client;
    private final String namespace;
    private final Thread subscriberThread;
    private volatile Jedis subscriber;

    public RedisService(NulvoraFriendsPlugin plugin) {
        this.plugin = plugin;
        var cfg = plugin.config().redis();
        this.namespace = cfg.namespace();
        var builder = DefaultJedisClientConfig.builder().database(cfg.database());
        if (!cfg.username().isBlank()) builder.user(cfg.username());
        if (!cfg.password().isBlank()) builder.password(cfg.password());
        this.client = new JedisPooled(new HostAndPort(cfg.host(), cfg.port()), builder.build());
        client.ping();
        subscriberThread = Thread.ofPlatform().name("nulfriends-redis-requests").daemon(true).start(this::subscribe);
    }

    public void cacheAndPublish(String kind, UUID player, Object payload) {
        String json = RedisProtocol.envelope(kind, payload);
        client.setex(key(kind, player), plugin.config().redis().cacheTtlSeconds(), json);
        client.publish(eventsChannel(), json);
    }

    public void publish(String kind, Object payload) {
        client.publish(eventsChannel(), RedisProtocol.envelope(kind, payload));
    }

    public String key(String kind, UUID player) { return namespace + ":cache:" + kind + ":" + player; }
    public String eventsChannel() { return namespace + ":events"; }
    public String requestsChannel() { return namespace + ":requests"; }

    private void subscribe() {
        var cfg = plugin.config().redis();
        try (Jedis jedis = new Jedis(cfg.host(), cfg.port())) {
            subscriber = jedis;
            if (!cfg.password().isBlank()) {
                if (cfg.username().isBlank()) jedis.auth(cfg.password());
                else jedis.auth(cfg.username(), cfg.password());
            }
            jedis.select(cfg.database());
            jedis.subscribe(new JedisPubSub() {
                @Override public void onMessage(String channel, String message) { handleRequest(message); }
            }, requestsChannel());
        } catch (Exception e) {
            if (!Thread.currentThread().isInterrupted()) plugin.logger().error("Redis subscription stopped", e);
        }
    }

    private void handleRequest(String json) {
        try {
            RedisProtocol.Message message = RedisProtocol.decode(json);
            switch (message.type()) {
                case RedisProtocol.JOIN_REQUEST -> handleJoin(message);
                case RedisProtocol.PARTY_WARP -> handlePartyWarp(message);
                default -> { }
            }
        } catch (Exception e) {
            plugin.logger().warn("Invalid Redis request: " + e.getMessage());
        }
    }

    private void handleJoin(RedisProtocol.Message message) {
        JoinRequestPayload request = RedisProtocol.gson().fromJson(message.payload(), JoinRequestPayload.class);
        plugin.proxy().getPlayer(UUID.fromString(request.playerUuid())).ifPresent(player ->
            plugin.proxy().getPlayer(UUID.fromString(request.targetUuid())).flatMap(p -> p.getCurrentServer())
                .ifPresent(sc -> player.createConnectionRequest(sc.getServer()).fireAndForget()));
    }

    private void handlePartyWarp(RedisProtocol.Message message) {
        PartyWarpRequestPayload request = RedisProtocol.gson().fromJson(message.payload(), PartyWarpRequestPayload.class);
        UUID leaderId = UUID.fromString(request.requesterUuid());
        plugin.proxy().getPlayer(leaderId).flatMap(p -> p.getCurrentServer()).ifPresent(sc ->
            plugin.partyServiceOpt().filter(service -> service.isLeader(leaderId)).ifPresent(service -> {
                String server = sc.getServerInfo().getName();
                service.warpMembers(leaderId, server, plugin.config().getServerName(server));
            }));
    }

    @Override public void close() {
        if (subscriber != null) subscriber.close();
        subscriberThread.interrupt();
        client.close();
    }
}
