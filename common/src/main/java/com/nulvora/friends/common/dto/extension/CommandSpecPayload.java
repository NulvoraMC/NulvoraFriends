package com.nulvora.friends.common.dto.extension;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * DTO que representa la especificación completa de un slash command
 * registrado por una extensión. Contiene nombre, descripción, opciones
 * por defecto, opciones a nivel de comando y subcomandos.
 *
 * <p>El nombre debe cumplir {@code ^[a-z0-9_-]{1,32}$} (restricción de Discord).
 * No puede coincidir con comandos built-in ({@code vincular}, {@code desvincular}, {@code amigos}).</p>
 *
 * @since 1.1.0
 */
public record CommandSpecPayload(
    @SerializedName("name") String name,
    @SerializedName("description") String description,
    @SerializedName("ephemeral_default") boolean ephemeralDefault,
    @SerializedName("options") List<CommandOptionPayload> options,
    @SerializedName("subcommands") List<SubcommandPayload> subcommands
) {}
