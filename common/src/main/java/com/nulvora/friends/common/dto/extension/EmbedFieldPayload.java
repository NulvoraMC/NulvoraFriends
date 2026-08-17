package com.nulvora.friends.common.dto.extension;

import com.google.gson.annotations.SerializedName;

/**
 * DTO que representa un campo dentro de un embed de Discord.
 *
 * @since 1.1.0
 */
public record EmbedFieldPayload(
    @SerializedName("name") String name,
    @SerializedName("value") String value,
    @SerializedName("inline") boolean inline
) {}
