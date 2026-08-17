package com.nulvora.friends.velocity.party;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public class Party {

    private final UUID id;
    private UUID leader;
    private final LinkedHashSet<UUID> members = new LinkedHashSet<>();
    private long lastFollowAt;

    public Party(UUID id, UUID leader) {
        this.id = id;
        this.leader = leader;
        this.members.add(leader);
    }

    public UUID id() { return id; }

    public UUID leader() { return leader; }

    public void setLeader(UUID leader) { this.leader = leader; }

    public boolean isLeader(UUID uuid) { return leader.equals(uuid); }

    public int size() { return members.size(); }

    public boolean contains(UUID uuid) { return members.contains(uuid); }

    public boolean addMember(UUID uuid) { return members.add(uuid); }

    public boolean removeMember(UUID uuid) { return members.remove(uuid); }

    public List<UUID> getMembers() { return Collections.unmodifiableList(new ArrayList<>(members)); }

    public UUID getOldestMember() {
        return members.stream().findFirst().orElse(null);
    }

    public long lastFollowAt() { return lastFollowAt; }

    public void setLastFollowAt(long ms) { this.lastFollowAt = ms; }
}
