package com.nulvora.friends.paper.api.discord;

import com.nulvora.friends.common.dto.extension.EmbedFieldPayload;
import com.nulvora.friends.common.dto.extension.EmbedPayload;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa un embed de Discord que se puede adjuntar como respuesta a un slash command.
 *
 * <p>Use el {@link Builder} para construir instancias:</p>
 * <pre>{@code
 * DiscordEmbed embed = DiscordEmbed.builder()
 *     .title("BedWars Status")
 *     .description("Estado actual de las arenas")
 *     .color(0x57F287)
 *     .field("Arena", "BedWars-1", true)
 *     .field("Jugadores", "8/12", true)
 *     .footer("NulvoraFriends • v1.1.0")
 *     .build();
 * }</pre>
 *
 * <p>Restricciones de Discord:</p>
 * <ul>
 *   <li>Máximo 25 campos</li>
 *   <li>Título: 256 caracteres máximo</li>
 *   <li>Descripción: 4096 caracteres máximo</li>
 *   <li>Nombre de campo: 256 caracteres máximo</li>
 *   <li>Valor de campo: 1024 caracteres máximo</li>
 * </ul>
 *
 * @since 1.1.0
 */
public final class DiscordEmbed {

    private final EmbedPayload payload;

    private DiscordEmbed(EmbedPayload payload) {
        this.payload = payload;
    }

    /**
     * Crea un nuevo builder para un embed vacío.
     *
     * @return un nuevo builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Retorna el DTO interno del embed para serialización.
     *
     * @return el payload del embed
     */
    public EmbedPayload toPayload() {
        return payload;
    }

    public static final class Builder {
        private String title;
        private String description;
        private int color;
        private final List<EmbedFieldPayload> fields = new ArrayList<>();
        private String footer;
        private String imageUrl;
        private String thumbnailUrl;

        private Builder() {}

        /**
         * Establece el título del embed.
         *
         * @param title el título (máximo 256 caracteres)
         * @return este builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Establece la descripción del embed.
         *
         * @param description la descripción (máximo 4096 caracteres)
         * @return este builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Establece el color del embed en formato RGB entero.
         *
         * @param color el color (por ejemplo {@code 0x57F287} para verde)
         * @return este builder
         */
        public Builder color(int color) {
            this.color = color;
            return this;
        }

        /**
         * Añade un campo al embed.
         *
         * @param name   el nombre del campo
         * @param value  el valor del campo
         * @param inline si el campo se muestra en línea
         * @return este builder
         */
        public Builder field(String name, String value, boolean inline) {
            this.fields.add(new EmbedFieldPayload(name, value, inline));
            return this;
        }

        /**
         * Establece el texto del pie de página.
         *
         * @param footer el texto del pie
         * @return este builder
         */
        public Builder footer(String footer) {
            this.footer = footer;
            return this;
        }

        /**
         * Establece la URL de la imagen del embed.
         *
         * @param imageUrl la URL de la imagen
         * @return este builder
         */
        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        /**
         * Establece la URL de la miniatura del embed.
         *
         * @param thumbnailUrl la URL de la miniatura
         * @return este builder
         */
        public Builder thumbnailUrl(String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
            return this;
        }

        /**
         * Construye el embed.
         *
         * @return el embed construido
         */
        public DiscordEmbed build() {
            return new DiscordEmbed(new EmbedPayload(
                title,
                description,
                color,
                Collections.unmodifiableList(fields),
                footer,
                imageUrl,
                thumbnailUrl
            ));
        }
    }
}
