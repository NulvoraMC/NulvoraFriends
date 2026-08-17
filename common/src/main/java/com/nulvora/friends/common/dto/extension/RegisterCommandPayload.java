package com.nulvora.friends.common.dto.extension;

import com.google.gson.annotations.SerializedName;

/**
 * DTO enviado desde PaperMC hacia Velocity para registrar un nuevo slash command
 * proporcionado por una extensión. Incluye un {@code requestId} para correlacionar
 * con el {@link RegisterAckPayload} de respuesta.
 *
 * @since 1.1.0
 */
public record RegisterCommandPayload(
    @SerializedName("request_id") String requestId,
    @SerializedName("plugin_name") String pluginName,
    @SerializedName("spec") CommandSpecPayload spec
) {}
