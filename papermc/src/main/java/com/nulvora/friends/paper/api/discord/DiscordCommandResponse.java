package com.nulvora.friends.paper.api.discord;

import com.nulvora.friends.common.dto.extension.CommandResponsePayload;
import com.nulvora.friends.common.dto.extension.EmbedPayload;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa la respuesta que una extensión envía a Discord cuando se ejecuta
 * un slash command registrado. Contiene un mensaje de texto plano (con markdown
 * de Discord), un flag ephemeral y una lista de embeds opcionales.
 *
 * <p>Use las fáctorias estáticas o el {@link Builder} para construir respuestas:</p>
 * <pre>{@code
 * // Respuesta simple
 * DiscordCommandResponse.of("Arena: BedWars-1 - Online", true);
 *
 * // Respuesta con embed
 * DiscordCommandResponse.builder("Estado de arenas")
 *     .ephemeral(false)
 *     .embed(DiscordEmbed.builder()
 *         .title("BedWars Status")
 *         .description("Todas las arenas operativas")
 *         .color(0x57F287)
 *         .build())
 *     .build();
 * }</pre>
 *
 * @since 1.1.0
 */
public final class DiscordCommandResponse {

    private final CommandResponsePayload payload;

    private DiscordCommandResponse(CommandResponsePayload payload) {
        this.payload = payload;
    }

    /**
     * Crea una respuesta simple con un mensaje de texto.
     *
     * @param message   el mensaje a enviar (soporta markdown de Discord)
     * @param ephemeral si la respuesta solo es visible para el usuario que ejecutó el comando
     * @return la respuesta creada
     */
    public static DiscordCommandResponse of(String message, boolean ephemeral) {
        return new DiscordCommandResponse(
            new CommandResponsePayload(null, true, message, ephemeral, List.of())
        );
    }

    /**
     * Crea una respuesta de error con un mensaje.
     *
     * @param message el mensaje de error
     * @return la respuesta creada (ephemeral por defecto)
     */
    public static DiscordCommandResponse error(String message) {
        return new DiscordCommandResponse(
            new CommandResponsePayload(null, false, message, true, List.of())
        );
    }

    /**
     * Crea un nuevo builder para una respuesta.
     *
     * @param message el mensaje de texto principal
     * @return un nuevo builder
     */
    public static Builder builder(String message) {
        return new Builder(message);
    }

    /**
     * Retorna el DTO interno de la respuesta para serialización.
     *
     * @return el payload de la respuesta
     */
    public CommandResponsePayload toPayload() {
        return payload;
    }

    public static final class Builder {
        private String message;
        private boolean ephemeral = true;
        private boolean success = true;
        private final List<DiscordEmbed> embeds = new ArrayList<>();

        private Builder(String message) {
            this.message = message;
        }

        /**
         * Establece si la respuesta es ephemeral.
         *
         * @param ephemeral true para que solo el usuario que ejecutó el comando pueda verla
         * @return este builder
         */
        public Builder ephemeral(boolean ephemeral) {
            this.ephemeral = ephemeral;
            return this;
        }

        /**
         * Establece si la respuesta indica éxito o error.
         *
         * @param success true para éxito, false para error
         * @return este builder
         */
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        /**
         * Añade un embed a la respuesta.
         *
         * @param embed el embed a añadir
         * @return este builder
         */
        public Builder embed(DiscordEmbed embed) {
            this.embeds.add(embed);
            return this;
        }

        /**
         * Construye la respuesta.
         *
         * @return la respuesta construida
         */
        public DiscordCommandResponse build() {
            List<EmbedPayload> embedPayloads = new ArrayList<>();
            for (DiscordEmbed embed : embeds) {
                embedPayloads.add(embed.toPayload());
            }
            return new DiscordCommandResponse(
                new CommandResponsePayload(null, success, message, ephemeral, Collections.unmodifiableList(embedPayloads))
            );
        }
    }
}
