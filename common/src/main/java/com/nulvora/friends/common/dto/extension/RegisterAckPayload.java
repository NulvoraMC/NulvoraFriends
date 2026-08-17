package com.nulvora.friends.common.dto.extension;

import com.google.gson.annotations.SerializedName;

/**
 * DTO enviado desde Velocity hacia PaperMC como respuesta a un
 * {@link RegisterCommandPayload}. Indica si el registro fue exitoso o no.
 *
 * @since 1.1.0
 */
public record RegisterAckPayload(
    @SerializedName("request_id") String requestId,
    @SerializedName("command") String command,
    @SerializedName("success") boolean success,
    @SerializedName("error") String error
) {}
