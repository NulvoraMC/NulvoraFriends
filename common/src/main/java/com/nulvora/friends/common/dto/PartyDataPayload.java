package com.nulvora.friends.common.dto;

import java.util.List;

public record PartyDataPayload(
    String playerUuid,
    String partyId,
    String leaderUuid,
    List<MemberEntry> members
) {
    public record MemberEntry(
        String uuid,
        String name,
        boolean online,
        String server
    ) {}
}
