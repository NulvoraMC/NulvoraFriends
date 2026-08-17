package com.nulvora.friends.common.dto.extension;

import com.google.gson.annotations.SerializedName;

/**
 * DTO que describe una opción de un slash command de Discord.
 * Se usa tanto en comandos raíz como en subcomandos.
 *
 * @since 1.1.0
 */
public record CommandOptionPayload(
    @SerializedName("name") String name,
    @SerializedName("description") String description,
    @SerializedName("type") String type,
    @SerializedName("required") boolean required
) {}
