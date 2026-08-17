package com.nulvora.friends.common.dto.extension;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

/**
 * DTO enviado desde Velocity hacia PaperMC cuando un usuario ejecuta un slash command
 * registrado por una extensión. Incluye un {@code invocationId} para correlacionar
 * con el {@link CommandResponsePayload} de respuesta.
 *
 * <p>Las opciones se envían como un mapa de nombre → valor serializado como String.</p>
 *
 * @since 1.1.0
 */
public record InvokeCommandPayload(
    @SerializedName("invocation_id") String invocationId,
    @SerializedName("command") String command,
    @SerializedName("subcommand") String subcommand,
    @SerializedName("options") Map<String, String> options,
    @SerializedName("discord_user_id") long discordUserId,
    @SerializedName("discord_username") String discordUsername
) {}
