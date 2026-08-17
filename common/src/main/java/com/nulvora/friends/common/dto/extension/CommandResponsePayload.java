package com.nulvora.friends.common.dto.extension;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * DTO enviado desde PaperMC hacia Velocity como respuesta de una extensión
 * a un slash command ejecutado en Discord. Correlaciona con un
 * {@link InvokeCommandPayload} mediante {@code invocationId}.
 *
 * <p>La respuesta contiene un mensaje de texto plano (con markdown de Discord),
 * un flag {@code ephemeral} que puede sobreescribir el valor por defecto del spec,
 * y una lista de embeds que se adjuntan como followups si son necesarios.</p>
 *
 * @since 1.1.0
 */
public record CommandResponsePayload(
    @SerializedName("invocation_id") String invocationId,
    @SerializedName("success") boolean success,
    @SerializedName("message") String message,
    @SerializedName("ephemeral") boolean ephemeral,
    @SerializedName("embeds") List<EmbedPayload> embeds
) {}
