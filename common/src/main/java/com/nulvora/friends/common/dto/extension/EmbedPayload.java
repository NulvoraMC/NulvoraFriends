package com.nulvora.friends.common.dto.extension;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * DTO que representa un embed de Discord enviado como respuesta a un slash command.
 * Los campos de color usan formato RGB entero (por ejemplo {@code 0x57F287} para verde).
 *
 * @since 1.1.0
 */
public record EmbedPayload(
    @SerializedName("title") String title,
    @SerializedName("description") String description,
    @SerializedName("color") int color,
    @SerializedName("fields") List<EmbedFieldPayload> fields,
    @SerializedName("footer") String footer,
    @SerializedName("image_url") String imageUrl,
    @SerializedName("thumbnail_url") String thumbnailUrl
) {}
