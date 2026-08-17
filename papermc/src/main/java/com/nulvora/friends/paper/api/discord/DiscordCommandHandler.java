package com.nulvora.friends.paper.api.discord;

import java.util.concurrent.CompletionStage;

/**
 * Interfaz funcional que representa el manejador de un slash command registrado
 * por una extensión. Cuando un usuario ejecuta el comando en Discord, el sistema
 * invoca este handler con el contexto de la ejecución.
 *
 * <p>El handler <strong>DEBE</strong> retornar un {@link CompletionStage} que se complete
 * con la respuesta a enviar a Discord. Si el future no se completa dentro del timeout
 * configurado (por defecto 8 segundos), el sistema envía automáticamente un mensaje
 * de error genérico.</p>
 *
 * <p>El handler se ejecuta de forma asíncrona en un hilo del pool de ejecución.
 * <strong>No debe bloquear el hilo principal del servidor.</strong></p>
 *
 * <p>Ejemplo:</p>
 * <pre>{@code
 * DiscordCommandHandler handler = ctx -> CompletableFuture.supplyAsync(() -> {
 *     String arena = ctx.getString("arena").orElse("todas");
 *     return DiscordCommandResponse.of("Arena: " + arena + " - Online", true);
 * });
 * }</pre>
 *
 * @since 1.1.0
 */
@FunctionalInterface
public interface DiscordCommandHandler {

    /**
     * Maneja la ejecución de un slash command.
     *
     * @param context el contexto de la ejecución (opciones, usuario, etc.)
     * @return un {@link CompletionStage} que se completa con la respuesta a Discord
     */
    CompletionStage<DiscordCommandResponse> handle(DiscordCommandContext context);
}
