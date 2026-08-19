package com.nulvora.friends.velocity.extension;

import com.google.gson.Gson;
import com.nulvora.friends.common.dto.extension.CommandOptionPayload;
import com.nulvora.friends.common.dto.extension.CommandResponsePayload;
import com.nulvora.friends.common.dto.extension.CommandSpecPayload;
import com.nulvora.friends.common.dto.extension.InvokeCommandPayload;
import com.nulvora.friends.common.dto.extension.RegisterAckPayload;
import com.nulvora.friends.common.dto.extension.RegisterCommandPayload;
import com.nulvora.friends.common.dto.extension.SubcommandPayload;
import com.nulvora.friends.common.dto.extension.UnregisterCommandsPayload;
import com.nulvora.friends.common.messaging.Channel;
import com.nulvora.friends.velocity.NulvoraFriendsPlugin;
import com.nulvora.friends.velocity.discord.DiscordBot;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.ErrorResponse;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

/**
 * Registro de comandos Discord dinámicos.
 *
 * <p>El registro en {@code commandToServer} y el ack a Paper solo ocurren
 * <b>después</b> de que JDA confirme el {@code upsertCommand}. Mientras JDA no
 * esté listo (o la guild no se pueda resolver aún), los registros se encolan
 * en {@link #deferredRegistrations} y se procesan en {@link #onDiscordReady()}.</p>
 */
public class ExtensionCommandRegistry {

    private static final java.util.List<String> BUILT_IN_COMMANDS = java.util.List.of("vincular", "desvincular", "amigos");

    private final NulvoraFriendsPlugin plugin;
    private final Gson gson = new Gson();
    private final ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor();
    private final MinecraftChannelIdentifier channelId = MinecraftChannelIdentifier.from(Channel.CHANNEL_NAME);

    private final Map<String, String> commandToServer = new ConcurrentHashMap<>();
    private final Map<String, DeferredRegistration> deferredRegistrations = new ConcurrentHashMap<>();
    private final Map<String, PendingInvocation> pendingInvocations = new ConcurrentHashMap<>();
    private final Map<String, SlashCommandInteractionEvent> pendingEventMap = new ConcurrentHashMap<>();
    private final Set<String> dynamicCommands = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean startupCleared = new AtomicBoolean(false);

    record DeferredRegistration(
        String requestId,
        String serverName,
        UUID carrierUuid,
        CommandSpecPayload spec
    ) {}

    record PendingInvocation(
        String commandName,
        boolean ephemeralDefault,
        long timestamp,
        ScheduledFuture<?> timeoutTask
    ) {}

    public ExtensionCommandRegistry(NulvoraFriendsPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleRegister(RegisterCommandPayload register, String serverName, Player carrier) {
        CommandSpecPayload spec = register.spec();
        String name = spec.name();
        String requestId = register.requestId();
        UUID carrierUuid = carrier != null ? carrier.getUniqueId() : null;

        if (BUILT_IN_COMMANDS.contains(name)) {
            sendAck(requestId, name, false, "Nombre de comando reservado: " + name, serverName, carrierUuid);
            return;
        }

        String existingServer = commandToServer.get(name);
        if (existingServer != null && !existingServer.equals(serverName)) {
            sendAck(requestId, name, false, "Comando ya registrado por servidor: " + existingServer, serverName, carrierUuid);
            return;
        }

        long guildId = plugin.config().discord().guildId();
        if (guildId == 0L) {
            sendAck(requestId, name, false,
                "El proxy no tiene 'discord.guild-id' configurado en config.json.", serverName, carrierUuid);
            return;
        }

        DiscordBot bot = plugin.discordBot().orElse(null);
        if (bot == null) {
            sendAck(requestId, name, false,
                "El bot de Discord está deshabilitado en este proxy.", serverName, carrierUuid);
            return;
        }

        if (!bot.isReady()) {
            deferredRegistrations.put(name, new DeferredRegistration(requestId, serverName, carrierUuid, spec));
            plugin.logger().info("Comando /" + name + " en espera de conexión con Discord (servidor: " + serverName + ")");
            return;
        }

        attemptUpsert(requestId, serverName, carrierUuid, spec);
    }

    public void handleUnregister(UnregisterCommandsPayload unregister) {
        java.util.List<String> commands = unregister.commands();

        if (commands.isEmpty()) {
            for (String cmd : new java.util.ArrayList<>(dynamicCommands)) {
                commandToServer.remove(cmd);
                deleteCommand(cmd);
            }
            dynamicCommands.clear();
        } else {
            for (String cmd : commands) {
                commandToServer.remove(cmd);
                dynamicCommands.remove(cmd);
                deleteCommand(cmd);
            }
        }
    }

    public void handleResponse(CommandResponsePayload response) {
        PendingInvocation pending = pendingInvocations.remove(response.invocationId());
        if (pending == null) {
            plugin.logger().warn("Respuesta para invocación desconocida: " + response.invocationId());
            return;
        }

        pending.timeoutTask().cancel(false);

        SlashCommandInteractionEvent event = pendingEventMap.remove(response.invocationId());
        if (event == null) {
            plugin.logger().warn("Evento de interacción no encontrado para: " + response.invocationId());
            return;
        }

        if (response.success()) {
            if (response.ephemeral()) {
                event.getHook().setEphemeral(true);
            }
            event.getHook().editOriginal(response.message() != null ? response.message() : " ").queue();
        } else {
            String errorMsg = response.message() != null ? response.message() : "Error al procesar el comando.";
            event.reply(errorMsg).setEphemeral(true).queue();
        }
    }

    public void handleInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        String serverName = commandToServer.get(commandName);

        if (serverName == null) {
            event.reply("Comando no disponible.").setEphemeral(true).queue();
            return;
        }

        String subcommand = event.getSubcommandName();
        Map<String, String> options = new HashMap<>();
        for (var option : event.getOptions()) {
            options.put(option.getName(), option.getAsString());
        }

        boolean ephemeralDefault = true;

        event.deferReply(ephemeralDefault).queue();

        String invocationId = UUID.randomUUID().toString();

        Optional<Player> carrier = findCarrierPlayer(serverName);
        if (carrier.isEmpty()) {
            event.getHook().editOriginal("El servidor **" + serverName + "** no está disponible en este momento.").queue();
            return;
        }

        long commandTimeoutMs = plugin.config().extensions().commandTimeoutMs();
        ScheduledFuture<?> timeoutTask = timeoutScheduler.schedule(() -> {
            pendingInvocations.remove(invocationId);
            pendingEventMap.remove(invocationId);
            event.getHook().editOriginal("El comando tardó demasiado en responder.").queue();
        }, commandTimeoutMs + 2000, TimeUnit.MILLISECONDS);

        pendingInvocations.put(invocationId,
            new PendingInvocation(commandName, ephemeralDefault, System.currentTimeMillis(), timeoutTask));
        pendingEventMap.put(invocationId, event);

        long discordUserId = event.getUser().getIdLong();
        String discordUsername = event.getUser().getName();

        plugin.db().getUuidByDiscordId(discordUserId).thenCompose(uuidOpt ->
            uuidOpt.map(uuid -> plugin.db().getName(uuid).thenApply(name -> new String[]{uuid.toString(), name}))
                .orElse(CompletableFuture.completedFuture(null))
        ).thenAccept(mcInfo -> {
            String mcUuid = mcInfo != null ? mcInfo[0] : null;
            String mcName = mcInfo != null ? mcInfo[1] : null;

            InvokeCommandPayload invoke = new InvokeCommandPayload(
                invocationId, commandName, subcommand, options,
                discordUserId, discordUsername,
                mcUuid, mcName
            );

            byte[] data = Channel.encode(Channel.MSG_EXT_INVOKE, gson.toJson(invoke));
            boolean sent = carrier.get().getCurrentServer()
                .map(sc -> sc.sendPluginMessage(channelId, data))
                .orElse(false);
            if (!sent) {
                plugin.logger().warn("No se pudo enviar la invocación de /" + commandName + " al servidor " + serverName);
            }
        });
    }

    // ─── Discord readiness ───────────────────────────────────────────────

    /**
     * Se llama cuando JDA emite {@code ReadyEvent}. La primera vez, limpia los
     * comandos de guild "stale" (dejados por una sesión previa del proxy) y a
     * continuación procesa los registros diferidos. En reconexiones posteriores
     * solo procesa los diferidos.
     */
    public void onDiscordReady() {
        if (!startupCleared.compareAndSet(false, true)) {
            flushDeferredRegistrations();
            return;
        }

        Guild guild = resolveGuild();
        if (guild == null) {
            plugin.logger().warn("No se pudieron limpiar comandos dinámicos: guild no encontrada para ID "
                + plugin.config().discord().guildId());
            flushDeferredRegistrations();
            return;
        }

        guild.retrieveCommands().queue(existing -> {
            Set<String> keep = new HashSet<>(BUILT_IN_COMMANDS);
            keep.addAll(deferredRegistrations.keySet());
            for (var cmd : existing) {
                if (!keep.contains(cmd.getName())) {
                    cmd.delete().queue(
                        success -> {},
                        error -> plugin.logger().warn("Error eliminando comando stale /" + cmd.getName() + ": " + error.getMessage())
                    );
                }
            }
            plugin.logger().info("Comandos dinámicos stale limpiados de Discord.");
            flushDeferredRegistrations();
        }, error -> {
            plugin.logger().warn("No se pudieron listar los comandos de Discord: " + error.getMessage());
            flushDeferredRegistrations();
        });
    }

    private void flushDeferredRegistrations() {
        Map<String, DeferredRegistration> toProcess = new HashMap<>(deferredRegistrations);
        deferredRegistrations.keySet().removeAll(toProcess.keySet());
        for (DeferredRegistration d : toProcess.values()) {
            attemptUpsert(d.requestId(), d.serverName(), d.carrierUuid(), d.spec());
        }
    }

    // ─── JDA helpers ─────────────────────────────────────────────────────

    private void attemptUpsert(String requestId, String serverName, UUID carrierUuid, CommandSpecPayload spec) {
        String name = spec.name();
        Guild guild = resolveGuild();
        if (guild == null) {
            sendAck(requestId, name, false,
                "Guild no encontrada para ID: " + plugin.config().discord().guildId(), serverName, carrierUuid);
            return;
        }

        SlashCommandData commandData = buildCommandData(spec);

        guild.upsertCommand(commandData).queue(
            success -> {
                commandToServer.put(name, serverName);
                dynamicCommands.add(name);
                sendAck(requestId, name, true, null, serverName, carrierUuid);
                plugin.logger().info("Comando Discord dinámico registrado: /" + name + " (servidor: " + serverName + ")");
            },
            error -> {
                String detail = error.getMessage();
                if (error instanceof ErrorResponseException ere
                    && ere.getErrorResponse() == ErrorResponse.MISSING_ACCESS) {
                    detail = "Falta acceso (MISSING_ACCESS). Reinvita al bot con el scope 'applications.commands'.";
                }
                plugin.logger().error("Error registrando /" + name + " en Discord", error);
                sendAck(requestId, name, false, detail, serverName, carrierUuid);
            }
        );
    }

    private Guild resolveGuild() {
        DiscordBot bot = plugin.discordBot().orElse(null);
        if (bot == null || bot.getJda().isEmpty()) return null;
        return bot.getJda().get().getGuildById(plugin.config().discord().guildId());
    }

    private SlashCommandData buildCommandData(CommandSpecPayload spec) {
        if (spec.subcommands() != null && !spec.subcommands().isEmpty()) {
            SubcommandData[] subArray = spec.subcommands().stream()
                .map(this::buildSubcommandData)
                .toArray(SubcommandData[]::new);
            return Commands.slash(spec.name(), spec.description()).addSubcommands(subArray);
        }

        SlashCommandData data = Commands.slash(spec.name(), spec.description());
        if (spec.options() != null) {
            for (CommandOptionPayload opt : spec.options()) {
                data.addOptions(buildOptionData(opt));
            }
        }
        return data;
    }

    private SubcommandData buildSubcommandData(SubcommandPayload sub) {
        SubcommandData data = new SubcommandData(sub.name(), sub.description());
        if (sub.options() != null) {
            for (CommandOptionPayload opt : sub.options()) {
                data.addOptions(buildOptionData(opt));
            }
        }
        return data;
    }

    private OptionData buildOptionData(CommandOptionPayload opt) {
        OptionType type = switch (opt.type()) {
            case "string" -> OptionType.STRING;
            case "integer" -> OptionType.INTEGER;
            case "boolean" -> OptionType.BOOLEAN;
            case "number" -> OptionType.NUMBER;
            case "user" -> OptionType.USER;
            default -> OptionType.STRING;
        };
        return new OptionData(type, opt.name(), opt.description(), opt.required());
    }

    private void deleteCommand(String commandName) {
        Guild guild = resolveGuild();
        if (guild == null) return;

        guild.retrieveCommands().queue(commands -> {
            for (var cmd : commands) {
                if (cmd.getName().equals(commandName)) {
                    cmd.delete().queue(
                        success -> plugin.logger().debug("Slash command eliminado: /" + commandName),
                        error -> plugin.logger().warn("Error eliminando /" + commandName)
                    );
                    break;
                }
            }
        });
    }

    // ─── Ack helpers ─────────────────────────────────────────────────────

    private void sendAck(String requestId, String command, boolean success, String error,
                          String serverName, UUID carrierUuid) {
        RegisterAckPayload ack = new RegisterAckPayload(requestId, command, success, error);
        byte[] data = Channel.encode(Channel.MSG_EXT_REGISTER_ACK, gson.toJson(ack));

        Optional<Player> carrier = carrierUuid != null ? plugin.proxy().getPlayer(carrierUuid) : Optional.empty();
        if (carrier.isPresent() && sendToBackend(carrier.get(), data)) return;

        Optional<Player> fallback = findCarrierPlayer(serverName);
        if (fallback.isPresent() && sendToBackend(fallback.get(), data)) return;

        plugin.logger().warn("No se pudo enviar el ack de /" + command + ": no hay jugadores en " + serverName + ".");
    }

    private boolean sendToBackend(Player player, byte[] data) {
        return player.getCurrentServer()
            .map(sc -> sc.sendPluginMessage(channelId, data))
            .orElse(false);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private Optional<Player> findCarrierPlayer(String serverName) {
        return plugin.proxy().getServer(serverName)
            .map(server -> server.getPlayersConnected().stream().findFirst())
            .orElse(Optional.empty());
    }

    private MessageEmbed buildEmbed(com.nulvora.friends.common.dto.extension.EmbedPayload payload) {
        EmbedBuilder builder = new EmbedBuilder();
        if (payload.title() != null) builder.setTitle(payload.title());
        if (payload.description() != null) builder.setDescription(payload.description());
        if (payload.color() != 0) builder.setColor(payload.color());
        if (payload.footer() != null) builder.setFooter(payload.footer());
        if (payload.imageUrl() != null) builder.setImage(payload.imageUrl());
        if (payload.thumbnailUrl() != null) builder.setThumbnail(payload.thumbnailUrl());

        if (payload.fields() != null) {
            for (var field : payload.fields()) {
                builder.addField(field.name(), field.value(), field.inline());
            }
        }

        return builder.build();
    }

    public Map<String, String> getCommandToServer() {
        return commandToServer;
    }

    public void shutdown() {
        timeoutScheduler.shutdownNow();
        commandToServer.clear();
        deferredRegistrations.clear();
        pendingInvocations.clear();
        pendingEventMap.clear();
        dynamicCommands.clear();
    }
}
