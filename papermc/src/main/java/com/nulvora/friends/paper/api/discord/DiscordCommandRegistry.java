package com.nulvora.friends.paper.api.discord;

import java.util.concurrent.CompletableFuture;

/**
 * Interfaz para el registro de comandos de Discord por parte de extensiones.
 *
 * <p>Las extensiones usan esta interfaz para registrar, des-registrar y consultar
 * comandos de Discord que serán manejados por su lógica. Los comandos registrados
 * se sincronizan automáticamente con Discord a través del plugin Velocity.</p>
 *
 * <p>Ejemplo completo:</p>
 * <pre>{@code
 * DiscordCommand cmd = DiscordCommand.builder("bedwars", "Comandos de BedWars")
 *     .ephemeralDefault(true)
 *     .subcommand(DiscordSubcommand.builder("status", "Estado de las arenas")
 *         .option(DiscordCommandOption.string("arena", "Nombre de la arena", false))
 *         .build())
 *     .build();
 *
 * DiscordCommandHandler handler = ctx -> CompletableFuture.supplyAsync(() -> {
 *     String arena = ctx.getString("arena").orElse("todas");
 *     return DiscordCommandResponse.builder("Arena: " + arena)
 *         .ephemeral(true)
 *         .embed(DiscordEmbed.builder()
 *             .title("BedWars Status")
 *             .color(0x57F287)
 *             .field("Arena", arena, true)
 *             .build())
 *         .build();
 * });
 *
 * NulvoraFriendsApi.get().discord().register(cmd, handler)
 *     .thenAccept(v -> getLogger().info("Comando registrado!"))
 *     .exceptionally(ex -> { getLogger().error("Error: " + ex.getMessage()); return null; });
 * }</pre>
 *
 * <p>Restricciones:</p>
 * <ul>
 *   <li>El nombre del comando no puede colisionar con comandos built-in (vincular, desvincular, amigos)</li>
 *   <li>El nombre no puede colisionar con comandos ya registrados por otro servidor</li>
 *   <li>Los comandos se registran a nivel de guild (propagación instantánea)</li>
 *   <li>Al desactivar la extensión, sus comandos se des-registran automáticamente</li>
 * </ul>
 *
 * @since 1.1.0
 */
public interface DiscordCommandRegistry {

    /**
     * Registra un nuevo slash command de Discord con su manejador.
     *
     * <p>El futuro se completa con éxito si el registro fue aceptado por Velocity,
     * o con una excepción si hubo un error (nombre duplicado, nombre built-in,
     * servidor no disponible, etc.).</p>
     *
     * <p>El registro se encola y se envía al proxy cuando un jugador se conecta
     * a este servidor (requerido por la restricción de plugin messaging).</p>
     *
     * @param command la especificación del comando
     * @param handler el manejador que procesará las ejecuciones del comando
     * @return un {@link CompletableFuture} que se completa con éxito o error
     */
    CompletableFuture<Void> register(DiscordCommand command, DiscordCommandHandler handler);

    /**
     * Des-registra un slash command de Discord por su nombre.
     *
     * @param commandName el nombre del comando a des-registrar
     * @return un {@link CompletableFuture} que se completa con éxito o error
     */
    CompletableFuture<Void> unregister(String commandName);

    /**
     * Des-registra todos los comandos registrados por esta extensión.
     *
     * @return un {@link CompletableFuture} que se completa con éxito o error
     */
    CompletableFuture<Void> unregisterAll();
}
