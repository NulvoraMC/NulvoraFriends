package com.nulvora.friends.paper.api.party;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * API para consultar la party de un jugador.
 *
 * <p>Los datos provienen de un caché actualizado por el plugin Velocity
 * a través de Redis. Solo hay datos de jugadores conectados
 * a este servidor backend.</p>
 *
 * @since 1.2.0
 */
public interface PartyApi {

    /**
     * Comprueba si un jugador pertenece a alguna party.
     *
     * @param playerUuid el UUID del jugador
     * @return {@code true} si el jugador está en una party
     */
    boolean isInParty(@NotNull UUID playerUuid);

    /**
     * Obtiene la party completa de un jugador.
     *
     * @param playerUuid el UUID del jugador
     * @return un {@link Optional} con la snapshot de la party, o vacío si no está en ninguna
     */
    @NotNull
    Optional<PartySnapshot> getParty(@NotNull UUID playerUuid);

    /**
     * Obtiene la lista de miembros de la party de un jugador.
     *
     * @param playerUuid el UUID del jugador
     * @return lista de miembros (vacía si no está en party)
     */
    @NotNull
    List<PartyMember> getMembers(@NotNull UUID playerUuid);

    /**
     * Obtiene el líder de la party de un jugador.
     *
     * @param playerUuid el UUID del jugador
     * @return un {@link Optional} con el miembro líder, o vacío si no está en party
     */
    @NotNull
    Optional<PartyMember> getLeader(@NotNull UUID playerUuid);

    /**
     * Comprueba si un jugador es el líder de su party.
     *
     * @param playerUuid el UUID del jugador
     * @return {@code true} si el jugador es líder
     */
    boolean isLeader(@NotNull UUID playerUuid);

    /**
     * Solicita al proxy que mueva todos los miembros de la party al servidor
     * donde se encuentra el líder. El mensaje se envía de forma asíncrona y
     * {@code true} no garantiza que el warp ocurra, solo que la petición salió.
     *
     * <p>Devuelve {@code false} sin hacer nada si el jugador no está en party,
     * no es el líder, o no hay ningún jugador online que pueda transportar
     * el mensaje al proxy.</p>
     *
     * @param leaderUuid el UUID del líder de la party
     * @return {@code true} si la petición se envió correctamente
     * @since 1.3.0
     */
    boolean requestWarpToMyServer(@NotNull UUID leaderUuid);
}
