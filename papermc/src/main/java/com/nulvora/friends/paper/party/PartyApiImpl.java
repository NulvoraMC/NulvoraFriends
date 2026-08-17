package com.nulvora.friends.paper.party;

import com.nulvora.friends.paper.api.party.PartyApi;
import com.nulvora.friends.paper.api.party.PartyMember;
import com.nulvora.friends.paper.api.party.PartySnapshot;
import com.nulvora.friends.paper.cache.PartyCache;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class PartyApiImpl implements PartyApi {

    private final PartyCache partyCache;

    public PartyApiImpl(PartyCache partyCache) {
        this.partyCache = partyCache;
    }

    @Override
    public boolean isInParty(@NotNull UUID playerUuid) {
        return partyCache.getParty(playerUuid) != null;
    }

    @Override
    @NotNull
    public Optional<PartySnapshot> getParty(@NotNull UUID playerUuid) {
        PartyCache.CachedParty cached = partyCache.getParty(playerUuid);
        if (cached == null) return Optional.empty();

        List<PartyMember> members = cached.members().stream()
            .map(m -> new PartyMember(m.uuid(), m.name(), m.online(), m.server()))
            .toList();

        return Optional.of(new PartySnapshot(cached.partyId(), cached.leader(), members));
    }

    @Override
    @NotNull
    public List<PartyMember> getMembers(@NotNull UUID playerUuid) {
        return getParty(playerUuid)
            .map(PartySnapshot::members)
            .orElse(List.of());
    }

    @Override
    @NotNull
    public Optional<PartyMember> getLeader(@NotNull UUID playerUuid) {
        return getParty(playerUuid).flatMap(snapshot -> {
            for (PartyMember member : snapshot.members()) {
                if (member.uuid().equals(snapshot.leader())) {
                    return Optional.of(member);
                }
            }
            return Optional.empty();
        });
    }

    @Override
    public boolean isLeader(@NotNull UUID playerUuid) {
        return getParty(playerUuid).map(s -> s.leader().equals(playerUuid)).orElse(false);
    }
}
