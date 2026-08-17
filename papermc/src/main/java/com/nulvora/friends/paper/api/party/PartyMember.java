package com.nulvora.friends.paper.api.party;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Representa un miembro de una party.
 *
 * @param uuid   el UUID del jugador
 * @param name   el nombre del jugador
 * @param online si el jugador está online
 * @param server el nombre del servidor donde está (puede ser {@code null})
 * @since 1.2.0
 */
public record PartyMember(
    @NotNull UUID uuid,
    @NotNull String name,
    boolean online,
    @Nullable String server
) {}
