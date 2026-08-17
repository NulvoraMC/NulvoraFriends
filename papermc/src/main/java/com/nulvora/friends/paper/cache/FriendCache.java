package com.nulvora.friends.paper.cache;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FriendCache {

    private final Map<UUID, List<CachedFriend>> cache = new ConcurrentHashMap<>();
    private final Map<UUID, List<CachedFriend>> pendingRequests = new ConcurrentHashMap<>();

    public void updateFriends(UUID player, List<CachedFriend> friends) {
        cache.put(player, friends);
    }

    public List<CachedFriend> getFriends(UUID player) {
        return cache.getOrDefault(player, List.of());
    }

    public void clear() {
        cache.clear();
        pendingRequests.clear();
    }

    public record CachedFriend(
        UUID uuid,
        String name,
        boolean online,
        String server
    ) {}
}
