package com.nulvora.friends.paper.api;

import com.nulvora.friends.paper.api.party.PartyApi;
import org.jetbrains.annotations.NotNull;

/**
 * Punto de entrada principal de la API de NulvoraFriends para extensiones.
 *
 * <p>Las extensiones obtienen esta instancia mediante {@link #get()}, que
 * retorna un singleton válido solo cuando el plugin NulvoraFriends-Paper
 * está activo en el servidor.</p>
 *
 * <p>Ejemplo de uso:</p>
 * <pre>{@code
 * PartyApi party = NulvoraFriendsApi.get().party();
 * Optional<PartySnapshot> snapshot = party.getParty(playerUuid);
 * }</pre>
 *
 * @since 1.1.0
 */
public final class NulvoraFriendsApi {

    private static NulvoraFriendsApi instance;

    private final PartyApi partyApi;

    /**
     * Crea una nueva instancia de la API. Uso interno del plugin.
     *
     * @param partyApi la API de party
     */
    public NulvoraFriendsApi(PartyApi partyApi) {
        this.partyApi = partyApi;
    }

    /**
     * Obtiene la instancia singleton de la API.
     *
     * @return la instancia de la API
     * @throws IllegalStateException si el plugin NulvoraFriends-Paper no está activo
     */
    @NotNull
    public static NulvoraFriendsApi get() {
        if (instance == null) {
            throw new IllegalStateException(
                "NulvoraFriends API no disponible. Asegúrate de que NulvoraFriends-Paper está activo."
            );
        }
        return instance;
    }

    /**
     * Establece la instancia singleton. Uso interno del plugin.
     *
     * @param api la instancia a establecer
     */
    public static void setInstance(NulvoraFriendsApi api) {
        instance = api;
    }

    /**
     * Limpia la instancia singleton. Uso interno del plugin.
     */
    public static void clearInstance() {
        instance = null;
    }

    /**
     * Retorna la API de party para consultar información de parties.
     *
     * @return el {@link PartyApi}
     * @since 1.2.0
     */
    @NotNull
    public PartyApi party() {
        return partyApi;
    }
}
