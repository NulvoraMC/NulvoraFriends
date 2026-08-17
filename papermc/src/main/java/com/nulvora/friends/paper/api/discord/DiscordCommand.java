package com.nulvora.friends.paper.api.discord;

import com.nulvora.friends.common.dto.extension.CommandOptionPayload;
import com.nulvora.friends.common.dto.extension.CommandSpecPayload;
import com.nulvora.friends.common.dto.extension.SubcommandPayload;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa la especificación completa de un slash command registrado
 * por una extensión en Discord.
 *
 * <p>Un comando tiene un nombre raíz (por ejemplo {@code "bedwars"}), una descripción,
 * un comportamiento ephemeral por defecto, opciones directas y subcomandos.</p>
 *
 * <p>Use el {@link Builder} para construir instancias:</p>
 * <pre>{@code
 * DiscordCommand cmd = DiscordCommand.builder("bedwars", "Comandos de BedWars")
 *     .ephemeralDefault(true)
 *     .subcommand(DiscordSubcommand.builder("status", "Estado de las arenas")
 *         .option(DiscordCommandOption.string("arena", "Nombre de la arena", false))
 *         .build())
 *     .build();
 * }</pre>
 *
 * <p>Restricciones de Discord:</p>
 * <ul>
 *   <li>El nombre debe cumplir {@code ^[a-z0-9_-]{1,32}$}</li>
 *   <li>Descripción: 1-100 caracteres</li>
 *   <li>Máximo 25 opciones por comando</li>
 *   <li>Máximo 25 subcomandos</li>
 * </ul>
 *
 * @since 1.1.0
 */
public final class DiscordCommand {

    private final CommandSpecPayload payload;

    private DiscordCommand(CommandSpecPayload payload) {
        this.payload = payload;
    }

    /**
     * Crea un nuevo builder para un comando.
     *
     * @param name        nombre del comando raíz (debe cumplir {@code ^[a-z0-9_-]{1,32}$})
     * @param description descripción del comando (1-100 caracteres)
     * @return un nuevo builder
     */
    public static Builder builder(String name, String description) {
        return new Builder(name, description);
    }

    /**
     * Retorna el DTO interno del comando para serialización.
     *
     * @return el payload del comando
     */
    public CommandSpecPayload toPayload() {
        return payload;
    }

    public static final class Builder {
        private final String name;
        private final String description;
        private boolean ephemeralDefault = true;
        private final List<DiscordCommandOption> options = new ArrayList<>();
        private final List<DiscordSubcommand> subcommands = new ArrayList<>();

        private Builder(String name, String description) {
            this.name = name;
            this.description = description;
        }

        /**
         * Establece si las respuestas a este comando son ephemeral por defecto.
         * El valor puede ser sobreescrito por la extensión en cada respuesta individual.
         *
         * @param ephemeral true para respuestas ephemeral por defecto
         * @return este builder
         */
        public Builder ephemeralDefault(boolean ephemeral) {
            this.ephemeralDefault = ephemeral;
            return this;
        }

        /**
         * Añade una opción directa al comando raíz.
         *
         * @param option la opción a añadir
         * @return este builder
         */
        public Builder option(DiscordCommandOption option) {
            this.options.add(option);
            return this;
        }

        /**
         * Añade un subcomando al comando.
         *
         * @param subcommand el subcomando a añadir
         * @return este builder
         */
        public Builder subcommand(DiscordSubcommand subcommand) {
            this.subcommands.add(subcommand);
            return this;
        }

        /**
         * Construye el comando.
         *
         * @return el comando construido
         */
        public DiscordCommand build() {
            List<CommandOptionPayload> optionPayloads = new ArrayList<>();
            for (DiscordCommandOption opt : options) {
                optionPayloads.add(opt.toPayload());
            }
            List<SubcommandPayload> subPayloads = new ArrayList<>();
            for (DiscordSubcommand sub : subcommands) {
                subPayloads.add(sub.toPayload());
            }
            return new DiscordCommand(new CommandSpecPayload(
                name,
                description,
                ephemeralDefault,
                Collections.unmodifiableList(optionPayloads),
                Collections.unmodifiableList(subPayloads)
            ));
        }
    }
}
