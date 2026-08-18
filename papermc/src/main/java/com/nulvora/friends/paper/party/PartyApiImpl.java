package com.nulvora.friends.paper.party;

import com.nulvora.friends.common.dto.PartyWarpRequestPayload;
import com.nulvora.friends.common.messaging.Channel;
import com.nulvora.friends.paper.NulvoraFriendsPaper;
import com.nulvora.friends.paper.api.party.PartyApi;
import com.nulvora.friends.paper.api.party.PartyMember;
import com.nulvora.friends.paper.api.party.PartySnapshot;
import com.nulvora.friends.paper.cache.PartyCache;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PartyApiImpl implements PartyApi {

    private final PartyCache partyCache;
    private final NulvoraFriendsPaper plugin;

    public PartyApiImpl(PartyCache partyCache, NulvoraFriendsPaper plugin) {
        this.partyCache = partyCache;
        this.plugin = plugin;
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

    @Override
    public boolean requestWarpToMyServer(@NotNull UUID leaderUuid) {
        if (!isInParty(leaderUuid) || !isLeader(leaderUuid)) return false;

        Player carrier = plugin.getServer().getOnlinePlayers().stream().findFirst().orElse(null);
        if (carrier == null) return false;

        PartyWarpRequestPayload request = new PartyWarpRequestPayload(leaderUuid.toString());
        byte[] data = Channel.encode(Channel.MSG_PARTY_WARP, Channel.toJson(request));
        carrier.sendPluginMessage(plugin, Channel.CHANNEL_NAME, data);
        return true;
    }
}
