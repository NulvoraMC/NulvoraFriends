package com.nulvora.friends.paper.extension;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nulvora.friends.common.dto.extension.CommandSpecPayload;
import com.nulvora.friends.common.dto.extension.InvokeCommandPayload;
import com.nulvora.friends.common.dto.extension.RegisterAckPayload;
import com.nulvora.friends.common.dto.extension.RegisterCommandPayload;
import com.nulvora.friends.common.dto.extension.UnregisterCommandsPayload;
import com.nulvora.friends.common.messaging.Channel;
import com.nulvora.friends.paper.NulvoraFriendsPaper;
import com.nulvora.friends.paper.api.discord.DiscordCommand;
import com.nulvora.friends.paper.api.discord.DiscordCommandContext;
import com.nulvora.friends.paper.api.discord.DiscordCommandHandler;
import com.nulvora.friends.paper.api.discord.DiscordCommandRegistry;
import com.nulvora.friends.paper.api.discord.DiscordCommandResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Implementación interna de {@link DiscordCommandRegistry}.
 *
 * <p>Gestiona el registro, validación, ejecución y des-registro de comandos
 * de Discord registrados por extensiones. Coordina la comunicación con el
 * plugin Velocity a través de plugin messaging.</p>
 *
 * <p>Componentes principales:</p>
 * <ul>
 *   <li>Almacén de comandos registrados (nombre → plugin propietario + handler)</li>
 *   <li>Cola de registros pendientes (se envían cuando un jugador entra al servidor)</li>
 *   <li>Invocaciones pendientes (se ejecutan con timeout configurado)</li>
 *   <li>Auto-desregistro al desactivar una extensión</li>
 * </ul>
 *
 * @since 1.1.0
 */
public class DiscordCommandManager implements DiscordCommandRegistry, Listener {

    private static final Pattern COMMAND_NAME_PATTERN = Pattern.compile("^[a-z0-9_-]{1,32}$");
    private static final Pattern OPTION_NAME_PATTERN = Pattern.compile("^[a-z0-9_-]{1,32}$");
    private static final int MAX_DESCRIPTION_LENGTH = 100;
    private static final int MAX_OPTIONS = 25;
    private static final int MAX_SUBCOMMANDS = 25;
    private static final long DEFAULT_RESPONSE_TIMEOUT_MS = 8000;

    private static final List<String> BUILT_IN_COMMANDS = List.of("vincular", "desvincular", "amigos");

    private final NulvoraFriendsPaper plugin;
    private final Gson gson = new Gson();
    private final ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor();

    /** Comando nombre → (plugin propietario, handler). */
    private final Map<String, RegisteredCommand> commands = new ConcurrentHashMap<>();

    /** RequestId → pending registration ack future. */
    private final Map<String, CompletableFuture<Void>> pendingAcks = new ConcurrentHashMap<>();

    /** InvocationId → pending invocation context. */
    private final Map<String, PendingInvocation> pendingInvocations = new ConcurrentHashMap<>();

    /** Cola de registros pendientes (se flush al entrar un jugador). */
    private final List<RegisterCommandPayload> pendingRegistrations =
        java.util.Collections.synchronizedList(new ArrayList<>());

    /** Cola de des-registros pendientes. */
    private final List<UnregisterCommandsPayload> pendingUnregistrations =
        java.util.Collections.synchronizedList(new ArrayList<>());

    private long responseTimeoutMs = DEFAULT_RESPONSE_TIMEOUT_MS;

    record RegisteredCommand(Plugin owner, DiscordCommandHandler handler, CommandSpecPayload spec) {}
    record PendingInvocation(DiscordCommandResponse defaultResponse, ScheduledFuture<?> timeoutTask) {}

    /**
     * Crea un nuevo {@code DiscordCommandManager}.
     *
     * @param plugin la instancia del plugin PaperMC
     */
    public DiscordCommandManager(NulvoraFriendsPaper plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Establece el timeout máximo (en milisegundos) para la respuesta de un handler.
     * Si el handler no completa el futuro dentro de este tiempo, se envía una
     * respuesta de error genérica.
     *
     * @param ms el timeout en milisegundos (mínimo 1000)
     */
    public void setResponseTimeoutMs(long ms) {
        this.responseTimeoutMs = Math.max(1000, ms);
    }

    // ─── API: register / unregister ──────────────────────────────────────

    @Override
    public CompletableFuture<Void> register(DiscordCommand command, DiscordCommandHandler handler) {
        CommandSpecPayload spec = command.toPayload();
        CompletableFuture<Void> future = new CompletableFuture<>();

        // Validar nombre
        String name = spec.name();
        if (name == null || !COMMAND_NAME_PATTERN.matcher(name).matches()) {
            future.completeExceptionally(new IllegalArgumentException(
                "Nombre de comando inválido: '" + name + "'. Debe cumplir ^[a-z0-9_-]{1,32}$"));
            return future;
        }

        // Validar built-in
        if (BUILT_IN_COMMANDS.contains(name)) {
            future.completeExceptionally(new IllegalArgumentException(
                "Nombre de comando reservado: '" + name + "'. No puede colisionar con comandos built-in."));
            return future;
        }

        // Validar descripción
        if (spec.description() == null || spec.description().isEmpty()
            || spec.description().length() > MAX_DESCRIPTION_LENGTH) {
            future.completeExceptionally(new IllegalArgumentException(
                "Descripción inválida: debe tener 1-" + MAX_DESCRIPTION_LENGTH + " caracteres."));
            return future;
        }

        // Validar opciones
        if (spec.options() != null && spec.options().size() > MAX_OPTIONS) {
            future.completeExceptionally(new IllegalArgumentException(
                "Demasiadas opciones: máximo " + MAX_OPTIONS + "."));
            return future;
        }

        // Validar subcomandos
        if (spec.subcommands() != null && spec.subcommands().size() > MAX_SUBCOMMANDS) {
            future.completeExceptionally(new IllegalArgumentException(
                "Demasiados subcomandos: máximo " + MAX_SUBCOMMANDS + "."));
            return future;
        }

        // Validar nombres de opciones
        if (spec.options() != null) {
            for (var opt : spec.options()) {
                if (opt.name() == null || !OPTION_NAME_PATTERN.matcher(opt.name()).matches()) {
                    future.completeExceptionally(new IllegalArgumentException(
                        "Nombre de opción inválido: '" + opt.name() + "'"));
                    return future;
                }
            }
        }

        // Validar nombres de subcomandos
        if (spec.subcommands() != null) {
            for (var sub : spec.subcommands()) {
                if (sub.name() == null || !OPTION_NAME_PATTERN.matcher(sub.name()).matches()) {
                    future.completeExceptionally(new IllegalArgumentException(
                        "Nombre de subcomando inválido: '" + sub.name() + "'"));
                    return future;
                }
            }
        }

        // Almacenar
        String ownerPlugin = plugin.getName();
        commands.put(name, new RegisteredCommand(plugin, handler, spec));

        // Encolar para enviar al proxy
        String requestId = UUID.randomUUID().toString();
        pendingAcks.put(requestId, future);
        RegisterCommandPayload payload = new RegisterCommandPayload(requestId, ownerPlugin, spec);
        pendingRegistrations.add(payload);

        plugin.getLogger().info("Comando Discord registrado: /" + name + " (por " + ownerPlugin + ")");

        // Intentar flush inmediato
        flushPending();

        return future;
    }

    @Override
    public CompletableFuture<Void> unregister(String commandName) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (!commands.containsKey(commandName)) {
            future.completeExceptionally(new IllegalArgumentException(
                "Comando no registrado: '" + commandName + "'"));
            return future;
        }

        commands.remove(commandName);
        pendingUnregistrations.add(new UnregisterCommandsPayload(List.of(commandName)));
        flushPending();

        plugin.getLogger().info("Comando Discord des-registrado: /" + commandName);
        future.complete(null);
        return future;
    }

    @Override
    public CompletableFuture<Void> unregisterAll() {
        List<String> toUnregister = new ArrayList<>(commands.keySet());
        commands.clear();

        if (!toUnregister.isEmpty()) {
            pendingUnregistrations.add(new UnregisterCommandsPayload(toUnregister));
            flushPending();
        }

        return CompletableFuture.completedFuture(null);
    }

    // ─── Recepción de acks desde Velocity ────────────────────────────────

    /**
     * Procesa un ack de registro recibido desde Velocity.
     *
     * @param ack el payload del ack
     */
    public void handleRegisterAck(RegisterAckPayload ack) {
        CompletableFuture<Void> future = pendingAcks.remove(ack.requestId());
        if (future == null) return;

        if (ack.success()) {
            future.complete(null);
            plugin.getLogger().info("Comando /" + ack.command() + " registrado en Discord correctamente.");
        } else {
            // Revertir el registro local
            commands.remove(ack.command());
            future.completeExceptionally(new IllegalStateException(
                "Registro rechazado por Velocity: " + ack.error()));
            plugin.getLogger().warning("Registro de /" + ack.command() + " rechazado: " + ack.error());
        }
    }

    // ─── Recepción de invocaciones desde Velocity ────────────────────────

    /**
     * Procesa una invocación de comando recibida desde Velocity.
     * Ejecuta el handler de la extensión con timeout.
     *
     * @param invoke el payload de la invocación
     */
    public void handleInvoke(InvokeCommandPayload invoke) {
        RegisteredCommand registered = commands.get(invoke.command());
        if (registered == null) {
            plugin.getLogger().warning("Invocación para comando no registrado: " + invoke.command());
            return;
        }

        DiscordCommandContext context = new DiscordCommandContext(
            invoke.command(),
            invoke.subcommand(),
            invoke.options(),
            invoke.discordUserId(),
            invoke.discordUsername()
        );

        // Ejecutar handler con timeout
        CompletableFuture<DiscordCommandResponse> responseFuture =
            CompletableFuture.supplyAsync(() -> {
                try {
                    return registered.handler().handle(context)
                        .toCompletableFuture()
                        .get(responseTimeoutMs, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING,
                        "Handler de /" + invoke.command() + " falló: " + e.getMessage(), e);
                    return DiscordCommandResponse.error(
                        "Ocurrió un error al procesar el comando.");
                }
            });

        // Timeout scheduler
        ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
            responseFuture.complete(DiscordCommandResponse.error(
                "El comando tardó demasiado en responder."));
        }, responseTimeoutMs + 2000, TimeUnit.MILLISECONDS);

        // Cuando la respuesta llega, enviarla
        responseFuture.thenAccept(response -> {
            timeoutTask.cancel(false);
            pendingInvocations.remove(invoke.invocationId());
            sendResponse(invoke.invocationId(), response);
        });
    }

    /**
     * Envía la respuesta de una invocación de vuelta a Velocity.
     *
     * @param invocationId el ID de la invocación
     * @param response la respuesta del handler
     */
    private void sendResponse(String invocationId, DiscordCommandResponse response) {
        // Buscar un jugador carrier para enviar al proxy
        Player carrier = findCarrierPlayer();
        if (carrier == null) {
            plugin.getLogger().warning(
                "No hay jugadores online para enviar respuesta de invocación " + invocationId);
            return;
        }

        var payload = response.toPayload();
        // Asignar invocationId al payload para serialización
        var responseWithId = new com.nulvora.friends.common.dto.extension.CommandResponsePayload(
            invocationId,
            payload.success(),
            payload.message(),
            payload.ephemeral(),
            payload.embeds()
        );

        byte[] data = Channel.encode(Channel.MSG_EXT_RESPONSE, Channel.toJson(responseWithId));
        carrier.sendPluginMessage(plugin, Channel.CHANNEL_NAME, data);
    }

    // ─── Flush de colas pendientes ───────────────────────────────────────

    /**
     * Intenta enviar las colas de registros/des-registros pendientes.
     * Se llama al entrar un jugador o al registrar/des-registrar un comando.
     */
    public void flushPending() {
        Player carrier = findCarrierPlayer();
        if (carrier == null) return;

        // Enviar registros pendientes
        synchronized (pendingRegistrations) {
            var iterator = pendingRegistrations.iterator();
            while (iterator.hasNext()) {
                RegisterCommandPayload payload = iterator.next();
                byte[] data = Channel.encode(Channel.MSG_EXT_REGISTER, Channel.toJson(payload));
                carrier.sendPluginMessage(plugin, Channel.CHANNEL_NAME, data);
                iterator.remove();
            }
        }

        // Enviar des-registros pendientes
        synchronized (pendingUnregistrations) {
            var iterator = pendingUnregistrations.iterator();
            while (iterator.hasNext()) {
                UnregisterCommandsPayload payload = iterator.next();
                byte[] data = Channel.encode(Channel.MSG_EXT_UNREGISTER, Channel.toJson(payload));
                carrier.sendPluginMessage(plugin, Channel.CHANNEL_NAME, data);
                iterator.remove();
            }
        }
    }

    /**
     * Busca un jugador online para usar como carrier de plugin messaging.
     *
     * @return un jugador online, o {@code null} si no hay ninguno
     */
    private Player findCarrierPlayer() {
        return plugin.getServer().getOnlinePlayers().stream().findFirst().orElse(null);
    }

    // ─── Eventos Bukkit ──────────────────────────────────────────────────

    /**
     * Cuando un jugador entra al servidor, flush la cola de registros pendientes.
     * Esto cubre el caso donde el plugin se activó sin jugadores conectados.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, this::flushPending, 20L);
    }

    /**
     * Cuando una extensión se desactiva, des-registra automáticamente todos sus comandos.
     */
    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        Plugin disabled = event.getPlugin();
        if (disabled.getName().equals(plugin.getName())) return;

        List<String> toRemove = new ArrayList<>();
        for (var entry : commands.entrySet()) {
            if (entry.getValue().owner().getName().equals(disabled.getName())) {
                toRemove.add(entry.getKey());
            }
        }

        if (!toRemove.isEmpty()) {
            for (String cmd : toRemove) {
                commands.remove(cmd);
            }
            pendingUnregistrations.add(new UnregisterCommandsPayload(toRemove));
            flushPending();
            plugin.getLogger().info("Des-registrados " + toRemove.size()
                + " comandos de la extensión " + disabled.getName() + " (plugin desactivado).");
        }
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────

    /**
     * Desactiva el manager, cancelando timeouts pendientes.
     */
    public void shutdown() {
        timeoutScheduler.shutdownNow();
        commands.clear();
        pendingAcks.clear();
        pendingInvocations.clear();
        pendingRegistrations.clear();
        pendingUnregistrations.clear();
    }
}
