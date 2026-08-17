package com.nulvora.friends.paper.api.discord;

/**
 * Tipos de opciones disponibles para slash commands de Discord.
 *
 * <p>Cada tipo corresponde a un valor de option type de la API de Discord.
 * El string interno se usa para la serialización del protocolo plugin messaging.</p>
 *
 * @since 1.1.0
 */
public enum DiscordOptionType {

    /** Opción de texto libre. */
    STRING("string"),

    /** Opción numérica entera. */
    INTEGER("integer"),

    /** Opción booleana (true/false). */
    BOOLEAN("boolean"),

    /** Opción numérica de punto flotante. */
    NUMBER("number"),

    /** Opción que referencia un usuario de Discord. */
    USER("user");

    private final String internalName;

    DiscordOptionType(String internalName) {
        this.internalName = internalName;
    }

    /**
     * Retorna el nombre interno usado en la serialización del protocolo.
     *
     * @return el nombre string del tipo
     */
    public String getInternalName() {
        return internalName;
    }
}
