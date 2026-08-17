package com.nulvora.friends.common.dto.extension;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * DTO que describe un subcomando dentro de un slash command.
 * Por ejemplo, en {@code /bedwars status}, {@code status} sería un {@code SubcommandPayload}
 * anidado bajo el comando raíz {@code bedwars}.
 *
 * @since 1.1.0
 */
public record SubcommandPayload(
    @SerializedName("name") String name,
    @SerializedName("description") String description,
    @SerializedName("options") List<CommandOptionPayload> options
) {}
