package com.nulvora.friends.paper.api.discord;

import com.nulvora.friends.common.dto.extension.CommandOptionPayload;

/**
 * Representa una opción de un slash command de Discord.
 *
 * <p>Use las fáctorias estáticas para crear opciones de cada tipo:</p>
 * <pre>{@code
 * DiscordCommandOption.string("arena", "Nombre de la arena", true);
 * DiscordCommandOption.integer("page", "Página de resultados", false);
 * }</pre>
 *
 * @since 1.1.0
 */
public final class DiscordCommandOption {

    private final CommandOptionPayload payload;

    private DiscordCommandOption(CommandOptionPayload payload) {
        this.payload = payload;
    }

    /**
     * Crea una opción de tipo cadena de texto.
     *
     * @param name        nombre de la opción (debe cumplir {@code ^[a-z0-9_-]{1,32}$})
     * @param description descripción de la opción (1-100 caracteres)
     * @param required    si la opción es obligatoria
     * @return la opción creada
     */
    public static DiscordCommandOption string(String name, String description, boolean required) {
        return new DiscordCommandOption(new CommandOptionPayload(name, description, "string", required));
    }

    /**
     * Crea una opción de tipo entero.
     *
     * @param name        nombre de la opción (debe cumplir {@code ^[a-z0-9_-]{1,32}$})
     * @param description descripción de la opción (1-100 caracteres)
     * @param required    si la opción es obligatoria
     * @return la opción creada
     */
    public static DiscordCommandOption integer(String name, String description, boolean required) {
        return new DiscordCommandOption(new CommandOptionPayload(name, description, "integer", required));
    }

    /**
     * Crea una opción de tipo booleano.
     *
     * @param name        nombre de la opción (debe cumplir {@code ^[a-z0-9_-]{1,32}$})
     * @param description descripción de la opción (1-100 caracteres)
     * @param required    si la opción es obligatoria
     * @return la opción creada
     */
    public static DiscordCommandOption bool(String name, String description, boolean required) {
        return new DiscordCommandOption(new CommandOptionPayload(name, description, "boolean", required));
    }

    /**
     * Crea una opción de tipo número (punto flotante).
     *
     * @param name        nombre de la opción (debe cumplir {@code ^[a-z0-9_-]{1,32}$})
     * @param description descripción de la opción (1-100 caracteres)
     * @param required    si la opción es obligatoria
     * @return la opción creada
     */
    public static DiscordCommandOption number(String name, String description, boolean required) {
        return new DiscordCommandOption(new CommandOptionPayload(name, description, "number", required));
    }

    /**
     * Crea una opción de tipo usuario de Discord.
     *
     * @param name        nombre de la opción (debe cumplir {@code ^[a-z0-9_-]{1,32}$})
     * @param description descripción de la opción (1-100 caracteres)
     * @param required    si la opción es obligatoria
     * @return la opción creada
     */
    public static DiscordCommandOption user(String name, String description, boolean required) {
        return new DiscordCommandOption(new CommandOptionPayload(name, description, "user", required));
    }

    /**
     * Retorna el DTO interno de la opción para serialización.
     *
     * @return el payload de la opción
     */
    public CommandOptionPayload toPayload() {
        return payload;
    }
}
