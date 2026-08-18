package com.nulvora.friends.common.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO enviado desde un backend PaperMC hacia Velocity para solicitar
 * el warp de todos los miembros de una party al servidor del líder.
 *
 * <p>El servidor destino no viaja en el payload. Velocity lo resuelve
 * a partir del servidor en el que está conectado el solicitante.</p>
 *
 * @since 1.3.0
 */
public record PartyWarpRequestPayload(
    @SerializedName("requester_uuid") String requesterUuid
) {}
