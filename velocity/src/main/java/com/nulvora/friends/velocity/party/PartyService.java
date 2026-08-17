package com.nulvora.friends.velocity.party;

import com.nulvora.friends.common.dto.PartyDataPayload;
import com.nulvora.friends.common.messaging.Channel;
import com.nulvora.friends.velocity.NulvoraFriendsPlugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PartyService {

    private final NulvoraFriendsPlugin plugin;
    private final Map<UUID, Party> parties = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> byPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Long>> invites = new ConcurrentHashMap<>();

    public PartyService(NulvoraFriendsPlugin plugin) {
        this.plugin = plugin;
    }

    // ─── Party operations ───────────────────────────────────────────────

    public Optional<Party> getParty(UUID playerUuid) {
        UUID partyId = byPlayer.get(playerUuid);
        if (partyId == null) return Optional.empty();
        return Optional.ofNullable(parties.get(partyId));
    }

    public Optional<Party> getPartyById(UUID partyId) {
        return Optional.ofNullable(parties.get(partyId));
    }

    public boolean isLeader(UUID playerUuid) {
        return getParty(playerUuid).filter(p -> p.isLeader(playerUuid)).isPresent();
    }

    public CompletableFuture<Party> createParty(UUID leaderUuid) {
        Optional<Party> existing = getParty(leaderUuid);
        if (existing.isPresent()) {
            return CompletableFuture.failedFuture(new IllegalStateException("already_in_party"));
        }

        UUID partyId = UUID.randomUUID();
        Party party = new Party(partyId, leaderUuid);
        parties.put(partyId, party);
        byPlayer.put(leaderUuid, partyId);

        pushPartyData(leaderUuid);
        return CompletableFuture.completedFuture(party);
    }

    public CompletableFuture<Void> invite(UUID inviterUuid, UUID targetUuid) {
        Optional<Party> partyOpt = getParty(inviterUuid);
        if (partyOpt.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("not_in_party"));
        }

        Party party = partyOpt.get();
        if (!party.isLeader(inviterUuid)) {
            return CompletableFuture.failedFuture(new IllegalStateException("not_leader"));
        }

        int maxSize = plugin.config().party().maxSize();
        if (party.size() >= maxSize) {
            return CompletableFuture.failedFuture(new IllegalStateException("party_full"));
        }

        if (party.contains(targetUuid)) {
            return CompletableFuture.failedFuture(new IllegalStateException("already_in_party"));
        }

        Optional<Party> targetParty = getParty(targetUuid);
        if (targetParty.isPresent()) {
            return CompletableFuture.failedFuture(new IllegalStateException("target_in_party"));
        }

        long expireMs = System.currentTimeMillis() + (plugin.config().party().inviteExpireSeconds() * 1000L);
        invites.computeIfAbsent(targetUuid, k -> new ConcurrentHashMap<>()).put(party.id(), expireMs);

        plugin.proxy().getPlayer(targetUuid).ifPresent(target -> {
            plugin.db().getName(inviterUuid).thenAccept(name -> {
                if (name != null) {
                    target.sendMessage(colorize(
                        plugin.config().messages().partyInvitationReceived()
                            .replace("%player%", name)));
                }
            });
        });

        plugin.proxy().getPlayer(inviterUuid).ifPresent(inviter -> {
            plugin.db().getName(targetUuid).thenAccept(name -> {
                if (name != null) {
                    inviter.sendMessage(colorize(
                        plugin.config().messages().partyInvitationSent()
                            .replace("%player%", name)));
                }
            });
        });

        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Boolean> accept(UUID playerUuid, UUID partyId) {
        Map<UUID, Long> playerInvites = invites.get(playerUuid);
        if (playerInvites == null) return CompletableFuture.completedFuture(false);

        Long expiry = playerInvites.remove(partyId);
        if (expiry == null) return CompletableFuture.completedFuture(false);

        if (System.currentTimeMillis() > expiry) {
            playerInvites.remove(partyId);
            plugin.proxy().getPlayer(playerUuid).ifPresent(p ->
                p.sendMessage(colorize(plugin.config().messages().partyInviteExpired())));
            return CompletableFuture.completedFuture(false);
        }

        Optional<Party> partyOpt = getPartyById(partyId);
        if (partyOpt.isEmpty()) return CompletableFuture.completedFuture(false);

        Party party = partyOpt.get();
        int maxSize = plugin.config().party().maxSize();
        if (party.size() >= maxSize) {
            plugin.proxy().getPlayer(playerUuid).ifPresent(p ->
                p.sendMessage(colorize(
                    plugin.config().messages().partyFull().replace("%max%", String.valueOf(maxSize)))));
            return CompletableFuture.completedFuture(false);
        }

        if (!playerInvites.isEmpty()) {
            playerInvites.clear();
        }

        byPlayer.put(playerUuid, partyId);
        party.addMember(playerUuid);

        plugin.db().getName(party.leader()).thenAccept(leaderName -> {
            plugin.proxy().getPlayer(playerUuid).ifPresent(p -> {
                p.sendMessage(colorize(
                    plugin.config().messages().partyJoined()
                        .replace("%leader%", leaderName != null ? leaderName : "?")));
            });
        });

        plugin.db().getName(playerUuid).thenAccept(name -> {
            if (name != null) {
                broadcastToParty(party,
                    plugin.config().messages().partyPlayerJoined().replace("%player%", name),
                    playerUuid);
            }
        });

        pushPartyData(playerUuid);
        pushPartyDataToAll(party);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> deny(UUID playerUuid, UUID partyId) {
        Map<UUID, Long> playerInvites = invites.get(playerUuid);
        if (playerInvites == null) return CompletableFuture.completedFuture(false);
        Long removed = playerInvites.remove(partyId);
        return CompletableFuture.completedFuture(removed != null);
    }

    public CompletableFuture<Boolean> leave(UUID playerUuid) {
        Optional<Party> partyOpt = getParty(playerUuid);
        if (partyOpt.isEmpty()) return CompletableFuture.failedFuture(new IllegalStateException("not_in_party"));

        Party party = partyOpt.get();

        party.removeMember(playerUuid);
        byPlayer.remove(playerUuid);

        pushEmptyPartyData(playerUuid);

        if (party.size() == 0) {
            parties.remove(party.id());
            return CompletableFuture.completedFuture(true);
        }

        if (party.isLeader(playerUuid)) {
            UUID newLeader = party.getOldestMember();
            party.setLeader(newLeader);
            plugin.db().getName(newLeader).thenAccept(name -> {
                if (name != null) {
                    broadcastToParty(party,
                        plugin.config().messages().partyLeaderChanged().replace("%player%", name));
                }
            });
            pushPartyDataToAll(party);
        } else {
            plugin.db().getName(playerUuid).thenAccept(name -> {
                if (name != null) {
                    broadcastToParty(party,
                        plugin.config().messages().partyPlayerLeft().replace("%player%", name));
                }
            });
            pushPartyDataToAll(party);
        }

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> kick(UUID leaderUuid, UUID targetUuid) {
        Optional<Party> partyOpt = getParty(leaderUuid);
        if (partyOpt.isEmpty()) return CompletableFuture.failedFuture(new IllegalStateException("not_in_party"));

        Party party = partyOpt.get();
        if (!party.isLeader(leaderUuid)) {
            return CompletableFuture.failedFuture(new IllegalStateException("not_leader"));
        }

        if (!party.contains(targetUuid) || targetUuid.equals(leaderUuid)) {
            return CompletableFuture.failedFuture(new IllegalStateException("invalid_target"));
        }

        party.removeMember(targetUuid);
        byPlayer.remove(targetUuid);

        plugin.proxy().getPlayer(targetUuid).ifPresent(p ->
            p.sendMessage(colorize(plugin.config().messages().partyKicked())));

        plugin.db().getName(targetUuid).thenAccept(name -> {
            if (name != null) {
                broadcastToParty(party,
                    plugin.config().messages().partyPlayerKicked().replace("%player%", name));
            }
        });

        pushEmptyPartyData(targetUuid);
        pushPartyDataToAll(party);
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<Boolean> disband(UUID leaderUuid) {
        Optional<Party> partyOpt = getParty(leaderUuid);
        if (partyOpt.isEmpty()) return CompletableFuture.failedFuture(new IllegalStateException("not_in_party"));

        Party party = partyOpt.get();
        if (!party.isLeader(leaderUuid)) {
            return CompletableFuture.failedFuture(new IllegalStateException("not_leader"));
        }

        broadcastToParty(party, plugin.config().messages().partyDisbanded());

        for (UUID member : party.getMembers()) {
            byPlayer.remove(member);
            pushEmptyPartyData(member);
        }
        parties.remove(party.id());
        return CompletableFuture.completedFuture(true);
    }

    // ─── Lifecycle ──────────────────────────────────────────────────────

    public java.util.List<UUID> getPendingInvites(UUID playerUuid) {
        Map<UUID, Long> playerInvites = invites.get(playerUuid);
        if (playerInvites == null) return java.util.List.of();
        long now = System.currentTimeMillis();
        return playerInvites.entrySet().stream()
            .filter(e -> e.getValue() > now)
            .map(Map.Entry::getKey)
            .toList();
    }

    public CompletableFuture<Party> inviteOrCreate(UUID inviterUuid, UUID targetUuid) {
        Optional<Party> existing = getParty(inviterUuid);
        if (existing.isPresent()) {
            return invite(inviterUuid, targetUuid).thenApply(v -> existing.get());
        }
        return createParty(inviterUuid).thenCompose(party ->
            invite(inviterUuid, targetUuid).thenApply(v -> party));
    }

    public void handleDisconnect(UUID playerUuid) {
        Optional<Party> partyOpt = getParty(playerUuid);
        if (partyOpt.isEmpty()) return;

        Party party = partyOpt.get();
        party.removeMember(playerUuid);
        byPlayer.remove(playerUuid);

        if (party.size() == 0) {
            parties.remove(party.id());
            return;
        }

        if (party.isLeader(playerUuid)) {
            UUID newLeader = party.getOldestMember();
            party.setLeader(newLeader);
            plugin.db().getName(newLeader).thenAccept(name -> {
                if (name != null) {
                    broadcastToParty(party,
                        plugin.config().messages().partyLeaderChanged().replace("%player%", name));
                }
            });
        } else {
            plugin.db().getName(playerUuid).thenAccept(name -> {
                if (name != null) {
                    broadcastToParty(party,
                        plugin.config().messages().partyPlayerLeft().replace("%player%", name));
                }
            });
        }

        pushPartyDataToAll(party);
    }

    public void handleLogin(UUID playerUuid) {
        pushEmptyPartyData(playerUuid);
    }

    // ─── Push to backends ───────────────────────────────────────────────

    public void pushPartyDataToAll(Party party) {
        for (UUID member : party.getMembers()) {
            pushPartyData(member);
        }
    }

    public void pushPartyData(UUID playerUuid) {
        Optional<Party> partyOpt = getParty(playerUuid);
        if (partyOpt.isEmpty()) {
            pushEmptyPartyData(playerUuid);
            return;
        }

        Party party = partyOpt.get();
        Player player = plugin.proxy().getPlayer(playerUuid).orElse(null);
        if (player == null) return;

        List<CompletableFuture<PartyDataPayload.MemberEntry>> futures = new ArrayList<>();
        for (UUID id : party.getMembers()) {
            futures.add(plugin.db().getName(id).thenApply(name -> {
                Player online = plugin.proxy().getPlayer(id).orElse(null);
                String server = online != null
                    ? online.getCurrentServer()
                        .map(sc -> sc.getServerInfo().getName())
                        .orElse(null)
                    : null;
                return new PartyDataPayload.MemberEntry(
                    id.toString(), name, online != null, server);
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
            List<PartyDataPayload.MemberEntry> entries = futures.stream()
                .map(CompletableFuture::join).toList();
            PartyDataPayload payload = new PartyDataPayload(
                playerUuid.toString(),
                party.id().toString(),
                party.leader().toString(),
                entries);
            byte[] data = Channel.encode(Channel.MSG_PARTY_DATA, Channel.toJson(payload));
            player.sendPluginMessage(
                MinecraftChannelIdentifier.from(Channel.CHANNEL_NAME), data);
        });
    }

    public void pushEmptyPartyData(UUID playerUuid) {
        Player player = plugin.proxy().getPlayer(playerUuid).orElse(null);
        if (player == null) return;

        PartyDataPayload payload = new PartyDataPayload(
            playerUuid.toString(), null, null, List.of());
        byte[] data = Channel.encode(Channel.MSG_PARTY_DATA, Channel.toJson(payload));
        player.sendPluginMessage(
            MinecraftChannelIdentifier.from(Channel.CHANNEL_NAME), data);
    }

    // ─── Follow ─────────────────────────────────────────────────────────

    public boolean canFollow(UUID playerUuid) {
        if (!plugin.config().party().follow().enabled()) return false;
        Optional<Party> partyOpt = getParty(playerUuid);
        if (partyOpt.isEmpty()) return false;
        Party party = partyOpt.get();
        if (!party.isLeader(playerUuid)) return false;
        if (party.size() < 2) return false;

        long cooldown = plugin.config().party().follow().cooldownMs();
        long now = System.currentTimeMillis();
        if (now - party.lastFollowAt() < cooldown) return false;

        party.setLastFollowAt(now);
        return true;
    }

    public List<Player> getWarpTargets(UUID leaderUuid, String targetServer) {
        Optional<Party> partyOpt = getParty(leaderUuid);
        if (partyOpt.isEmpty()) return List.of();

        Party party = partyOpt.get();
        List<Player> targets = new ArrayList<>();

        for (UUID memberUuid : party.getMembers()) {
            if (memberUuid.equals(leaderUuid)) continue;
            plugin.proxy().getPlayer(memberUuid).ifPresent(member -> {
                member.getCurrentServer().ifPresent(sc -> {
                    String currentServer = sc.getServerInfo().getName();
                    if (!currentServer.equals(targetServer)) {
                        targets.add(member);
                    }
                });
            });
        }
        return targets;
    }

    public void warpMembers(UUID leaderUuid, String targetServer, String displayName) {
        List<Player> targets = getWarpTargets(leaderUuid, targetServer);
        plugin.proxy().getServer(targetServer).ifPresent(targetServerObj -> {
            String msg = plugin.config().messages().partyFollow()
                .replace("%server%", displayName);
            for (Player member : targets) {
                member.createConnectionRequest(targetServerObj).fireAndForget();
                member.sendMessage(colorize(msg));
            }
        });
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    private void broadcastToParty(Party party, String message) {
        broadcastToParty(party, message, null);
    }

    private void broadcastToParty(Party party, String message, UUID exclude) {
        for (UUID member : party.getMembers()) {
            if (member.equals(exclude)) continue;
            plugin.proxy().getPlayer(member).ifPresent(p ->
                p.sendMessage(colorize(message)));
        }
    }

    private net.kyori.adventure.text.Component colorize(String message) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacyAmpersand().deserialize(message);
    }
}
