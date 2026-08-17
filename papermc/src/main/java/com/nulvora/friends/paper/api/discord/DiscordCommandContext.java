package com.nulvora.friends.paper.api.discord;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Contexto de ejecución de un slash command de Discord.
 * Proporciona acceso a la información del comando ejecutado, las opciones
 * proporcionadas por el usuario y los datos del usuario de Discord.
 *
 * <p>Este objeto se entrega al {@link DiscordCommandHandler} cuando se
 * invoca un comando registrado por una extensión.</p>
 *
 * @since 1.1.0
 */
public final class DiscordCommandContext {

    private final String command;
    private final String subcommand;
    private final Map<String, String> options;
    private final long discordUserId;
    private final String discordUsername;

    public DiscordCommandContext(String command, String subcommand,
                                 Map<String, String> options,
                                 long discordUserId, String discordUsername) {
        this.command = command;
        this.subcommand = subcommand;
        this.options = Collections.unmodifiableMap(options);
        this.discordUserId = discordUserId;
        this.discordUsername = discordUsername;
    }

    /**
     * Retorna el nombre del comando raíz ejecutado.
     *
     * @return el nombre del comando (por ejemplo {@code "bedwars"})
     */
    public String command() {
        return command;
    }

    /**
     * Retorna el nombre del subcomando ejecutado, o {@code null} si se ejecutó
     * el comando raíz directamente.
     *
     * @return el nombre del subcomando, o {@code null}
     */
    public String subcommand() {
        return subcommand;
    }

    /**
     * Retorna el valor de una opción como cadena de texto.
     *
     * @param name el nombre de la opción
     * @return un {@link Optional} con el valor, o vacío si no fue proporcionado
     */
    public Optional<String> getString(String name) {
        return Optional.ofNullable(options.get(name));
    }

    /**
     * Retorna el valor de una opción como entero.
     *
     * @param name el nombre de la opción
     * @return un {@link Optional} con el valor, o vacío si no fue proporcionado o no es numérico
     */
    public Optional<Integer> getInt(String name) {
        String val = options.get(name);
        if (val == null) return Optional.empty();
        try {
            return Optional.of(Integer.parseInt(val));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Retorna el valor de una opción como número de punto flotante.
     *
     * @param name el nombre de la opción
     * @return un {@link Optional} con el valor, o vacío si no fue proporcionado o no es numérico
     */
    public Optional<Double> getDouble(String name) {
        String val = options.get(name);
        if (val == null) return Optional.empty();
        try {
            return Optional.of(Double.parseDouble(val));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Retorna el valor de una opción como booleano.
     *
     * @param name el nombre de la opción
     * @return un {@link Optional} con el valor, o vacío si no fue proporcionado
     */
    public Optional<Boolean> getBoolean(String name) {
        String val = options.get(name);
        if (val == null) return Optional.empty();
        return Optional.of(Boolean.parseBoolean(val));
    }

    /**
     * Retorna el ID de usuario de Discord que ejecutó el comando.
     *
     * @return el ID del usuario
     */
    public long discordUserId() {
        return discordUserId;
    }

    /**
     * Retorna el nombre de usuario de Discord que ejecutó el comando.
     *
     * @return el nombre del usuario
     */
    public String discordUsername() {
        return discordUsername;
    }

    /**
     * Retorna todas las opciones proporcionadas como un mapa inmutable.
     *
     * @return mapa de nombre → valor serializado como String
     */
    public Map<String, String> options() {
        return options;
    }
}
