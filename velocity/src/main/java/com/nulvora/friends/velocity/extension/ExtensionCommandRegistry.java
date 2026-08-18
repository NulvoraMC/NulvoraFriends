package com.nulvora.friends.velocity.extension;

import com.google.gson.Gson;
import com.nulvora.friends.common.dto.extension.CommandOptionPayload;
import com.nulvora.friends.common.dto.extension.CommandResponsePayload;
import com.nulvora.friends.common.dto.extension.CommandSpecPayload;
import com.nulvora.friends.common.dto.extension.InvokeCommandPayload;
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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

public class ExtensionCommandRegistry {

    private static final java.util.List<String> BUILT_IN_COMMANDS = java.util.List.of("vincular", "desvincular", "amigos");

    private final NulvoraFriendsPlugin plugin;
    private final Gson gson = new Gson();
    private final ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor();

    private final Map<String, String> commandToServer = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Void>> pendingAcks = new ConcurrentHashMap<>();
    private final Map<String, PendingInvocation> pendingInvocations = new ConcurrentHashMap<>();
    private final Map<String, SlashCommandInteractionEvent> pendingEventMap = new ConcurrentHashMap<>();
    private final Set<String> dynamicCommands = ConcurrentHashMap.newKeySet();

    record PendingInvocation(
        String commandName,
        boolean ephemeralDefault,
        long timestamp,
        ScheduledFuture<?> timeoutTask
    ) {}

    public ExtensionCommandRegistry(NulvoraFriendsPlugin plugin) {
        this.plugin = plugin;
    }

    public void handleRegister(RegisterCommandPayload register, String serverName) {
        CommandSpecPayload spec = register.spec();
        String name = spec.name();
        String requestId = register.requestId();

        if (BUILT_IN_COMMANDS.contains(name)) {
            sendAck(requestId, name, false, "Nombre de comando reservado: " + name);
            return;
        }

        if (commandToServer.containsKey(name)) {
            String existingServer = commandToServer.get(name);
            sendAck(requestId, name, false, "Comando ya registrado por servidor: " + existingServer);
            return;
        }

        commandToServer.put(name, serverName);
        dynamicCommands.add(name);
        upsertCommand(spec);
        sendAck(requestId, name, true, null);

        plugin.logger().info("Comando Discord dinámico registrado: /" + name + " (servidor: " + serverName + ")");
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
            carrier.get().sendPluginMessage(MinecraftChannelIdentifier.from(Channel.CHANNEL_NAME), data);
        });
    }

    // ─── JDA helpers ─────────────────────────────────────────────────────

    private void upsertCommand(CommandSpecPayload spec) {
        DiscordBot bot = plugin.discordBot().orElse(null);
        if (bot == null || bot.getJda().isEmpty()) return;

        Guild guild = bot.getJda().get().getGuildById(plugin.config().discord().guildId());
        if (guild == null) {
            plugin.logger().warn("Guild no encontrada para ID: " + plugin.config().discord().guildId());
            return;
        }

        SlashCommandData commandData = buildCommandData(spec);

        guild.upsertCommand(commandData).queue(
            success -> plugin.logger().debug("Slash command upserted: /" + spec.name()),
            error -> plugin.logger().warn("Error upserting /" + spec.name() + ": " + error.getMessage())
        );
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
        DiscordBot bot = plugin.discordBot().orElse(null);
        if (bot == null || bot.getJda().isEmpty()) return;

        Guild guild = bot.getJda().get().getGuildById(plugin.config().discord().guildId());
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

    public void clearDynamicCommands() {
        DiscordBot bot = plugin.discordBot().orElse(null);
        if (bot == null || bot.getJda().isEmpty()) return;

        Guild guild = bot.getJda().get().getGuildById(plugin.config().discord().guildId());
        if (guild == null) return;

        guild.retrieveCommands().queue(commands -> {
            Set<String> builtInNames = new HashSet<>(BUILT_IN_COMMANDS);
            for (var cmd : commands) {
                if (!builtInNames.contains(cmd.getName())) {
                    cmd.delete().queue();
                }
            }
        });
    }

    // ─── Ack helpers ─────────────────────────────────────────────────────

    private void sendAck(String requestId, String command, boolean success, String error) {
        com.nulvora.friends.common.dto.extension.RegisterAckPayload ack =
            new com.nulvora.friends.common.dto.extension.RegisterAckPayload(requestId, command, success, error);

        for (Player player : plugin.proxy().getAllPlayers()) {
            byte[] data = Channel.encode(Channel.MSG_EXT_REGISTER_ACK, gson.toJson(ack));
            player.sendPluginMessage(MinecraftChannelIdentifier.from(Channel.CHANNEL_NAME), data);
            return;
        }
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
        pendingAcks.clear();
        pendingInvocations.clear();
        pendingEventMap.clear();
        dynamicCommands.clear();
    }
}
