package com.nulvora.friends.paper.cache;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PartyCache {

    private final Map<UUID, CachedParty> cache = new ConcurrentHashMap<>();

    public void updateParty(UUID playerUuid, CachedParty party) {
        cache.put(playerUuid, party);
    }

    public void remove(UUID playerUuid) {
        cache.remove(playerUuid);
    }

    public CachedParty getParty(UUID playerUuid) {
        return cache.get(playerUuid);
    }

    public void clear() {
        cache.clear();
    }

    public record CachedParty(
        UUID partyId,
        UUID leader,
        List<CachedPartyMember> members
    ) {}

    public record CachedPartyMember(
        UUID uuid,
        String name,
        boolean online,
        String server
    ) {}
}
