package com.nulvora.friends.common.dto.extension;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * DTO enviado desde PaperMC hacia Velocity para des-registrar comandos.
 * Si la lista está vacía, se des-registran todos los comandos del servidor origen.
 * Se envía al desactivar una extensión ({@code PluginDisableEvent}) o al
 * deshabilitarse el propio plugin PaperMC.
 *
 * @since 1.1.0
 */
public record UnregisterCommandsPayload(
    @SerializedName("commands") List<String> commands
) {}
