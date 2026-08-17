package com.nulvora.friends.paper.api.party;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Snapshot inmutable de una party.
 *
 * @param partyId  el UUID de la party
 * @param leader   el UUID del líder
 * @param members  la lista de miembros
 * @since 1.2.0
 */
public record PartySnapshot(
    @NotNull UUID partyId,
    @NotNull UUID leader,
    @NotNull List<PartyMember> members
) {}
