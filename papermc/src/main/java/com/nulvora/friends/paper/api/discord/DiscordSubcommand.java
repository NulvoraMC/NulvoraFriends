package com.nulvora.friends.paper.api.discord;

import com.nulvora.friends.common.dto.extension.CommandOptionPayload;
import com.nulvora.friends.common.dto.extension.SubcommandPayload;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa un subcomando dentro de un slash command de Discord.
 *
 * <p>Por ejemplo, en {@code /bedwars status}, el subcomando sería {@code "status"}
 * bajo el comando raíz {@code "bedwars"}.</p>
 *
 * <p>Use el {@link Builder} para construir instancias:</p>
 * <pre>{@code
 * DiscordSubcommand sub = DiscordSubcommand.builder("status", "Estado de las arenas")
 *     .option(DiscordCommandOption.string("arena", "Nombre de la arena", false))
 *     .build();
 * }</pre>
 *
 * @since 1.1.0
 */
public final class DiscordSubcommand {

    private final SubcommandPayload payload;

    private DiscordSubcommand(SubcommandPayload payload) {
        this.payload = payload;
    }

    /**
     * Crea un nuevo builder para un subcomando.
     *
     * @param name        nombre del subcomando (debe cumplir {@code ^[a-z0-9_-]{1,32}$})
     * @param description descripción del subcomando (1-100 caracteres)
     * @return un nuevo builder
     */
    public static Builder builder(String name, String description) {
        return new Builder(name, description);
    }

    /**
     * Retorna el DTO interno del subcomando para serialización.
     *
     * @return el payload del subcomando
     */
    public SubcommandPayload toPayload() {
        return payload;
    }

    public static final class Builder {
        private final String name;
        private final String description;
        private final List<DiscordCommandOption> options = new ArrayList<>();

        private Builder(String name, String description) {
            this.name = name;
            this.description = description;
        }

        /**
         * Añade una opción al subcomando.
         *
         * @param option la opción a añadir
         * @return este builder
         */
        public Builder option(DiscordCommandOption option) {
            this.options.add(option);
            return this;
        }

        /**
         * Construye el subcomando.
         *
         * @return el subcomando construido
         */
        public DiscordSubcommand build() {
            List<CommandOptionPayload> payloads = new ArrayList<>();
            for (DiscordCommandOption opt : options) {
                payloads.add(opt.toPayload());
            }
            return new DiscordSubcommand(
                new SubcommandPayload(name, description, Collections.unmodifiableList(payloads))
            );
        }
    }
}
