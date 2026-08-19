package com.nulvora.friends.velocity.discord;

import com.nulvora.friends.velocity.NulvoraFriendsPlugin;
import com.nulvora.friends.velocity.config.NulvoraConfig;
import com.nulvora.friends.velocity.extension.ExtensionCommandRegistry;
import com.velocitypowered.api.proxy.Player;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import org.jetbrains.annotations.NotNull;

public class DiscordBot extends ListenerAdapter {

    private final NulvoraFriendsPlugin plugin;
    private JDA jda;
    private volatile boolean ready = false;

    public DiscordBot(NulvoraFriendsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        NulvoraConfig.DiscordConfig config = plugin.config().discord();
        if (!config.enabled() || config.token().isBlank()) {
            plugin.logger().warn("Discord deshabilitado o token vacío. Bot no iniciado.");
            return;
        }

        plugin.logger().info("Iniciando bot de Discord...");

        jda = JDABuilder.createDefault(config.token())
            .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
            .addEventListeners(this)
            .build();

        jda.upsertCommand("vincular", "Vincula tu cuenta de Discord con Minecraft")
            .addOption(OptionType.STRING, "codigo", "Codigo generado en /vincular de Minecraft", true)
            .queue(
                v -> plugin.logger().info("Comando /vincular registrado."),
                e -> plugin.logger().error("Error registrando /vincular: " + e.getMessage())
            );

        jda.upsertCommand("desvincular", "Desvincula tu cuenta de Discord").queue(
            v -> plugin.logger().info("Comando /desvincular registrado."),
            e -> plugin.logger().error("Error registrando /desvincular: " + e.getMessage())
        );

        jda.upsertCommand("amigos", "Muestra tu lista de amigos y su estado")
            .addOption(OptionType.USER, "usuario", "Ver amigos de otro usuario vinculado", false)
            .queue(
                v -> plugin.logger().info("Comando /amigos registrado."),
                e -> plugin.logger().error("Error registrando /amigos: " + e.getMessage())
            );
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        ready = true;
        plugin.logger().info("Bot de Discord conectado como " + event.getJDA().getSelfUser().getAsTag());
        plugin.extensionRegistryOpt().ifPresent(ExtensionCommandRegistry::onDiscordReady);
    }

    public void stop() {
        if (jda != null) jda.shutdownNow();
    }

    public boolean isReady() { return ready; }

    public Optional<JDA> getJda() { return Optional.ofNullable(jda); }

    public void removeLinkedRole(long discordId) {
        if (jda == null) return;
        Guild g = jda.getGuildById(plugin.config().discord().guildId());
        if (g == null) return;
        Role role = g.getRoleById(plugin.config().discord().linkedRoleId());
        if (role == null) return;
        g.retrieveMemberById(discordId).queue(member -> g.removeRoleFromMember(member, role).queue());
    }

    public void addLinkedRole(long discordId) {
        if (jda == null) return;
        Guild g = jda.getGuildById(plugin.config().discord().guildId());
        if (g == null) return;
        Role role = g.getRoleById(plugin.config().discord().linkedRoleId());
        if (role == null) return;
        g.retrieveMemberById(discordId).queue(member -> g.addRoleToMember(member, role).queue());
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        // Built-in commands
        switch (event.getName()) {
            case "vincular" -> handleLink(event);
            case "desvincular" -> handleUnlink(event);
            case "amigos" -> handleFriendsList(event);
            default -> plugin.extensionRegistryOpt().ifPresentOrElse(
                registry -> registry.handleInteraction(event),
                () -> event.reply("Comando no disponible.").setEphemeral(true).queue()
            );
        }
    }

    private void handleLink(SlashCommandInteractionEvent event) {
        String code = event.getOption("codigo", opt -> opt.getAsString().toUpperCase());
        if (code.isBlank()) {
            event.reply("Proporciona un codigo valido.").setEphemeral(true).queue();
            return;
        }
        event.deferReply(true).queue();
        plugin.db().consumeLinkCode(code).thenAccept(uuidOpt -> {
            if (uuidOpt.isEmpty()) {
                event.getHook().editOriginal("Codigo invalido o expirado.").queue();
                return;
            }
            UUID uuid = uuidOpt.get();
            long discordId = event.getUser().getIdLong();
            plugin.db().linkAccounts(uuid, discordId).thenAccept(success -> {
                if (success) {
                    plugin.db().getName(uuid).thenAccept(name -> {
                        addLinkedRole(discordId);
                        String displayName = name != null ? name : uuid.toString();
                        event.getHook().editOriginal("Cuenta vinculada con **" + displayName + "** (" + uuid + ")").queue();
                    });
                } else {
                    event.getHook().editOriginal("Error al vincular la cuenta.").queue();
                }
            });
        });
    }

    private void handleUnlink(SlashCommandInteractionEvent event) {
        long discordId = event.getUser().getIdLong();
        plugin.db().unlinkByDiscordId(discordId).thenAccept(success -> {
            if (success) {
                removeLinkedRole(discordId);
                event.reply("Cuenta desvinculada correctamente.").setEphemeral(true).queue();
            } else {
                event.reply("Tu cuenta de Discord no esta vinculada.").setEphemeral(true).queue();
            }
        });
    }

    private void handleFriendsList(SlashCommandInteractionEvent event) {
        long discordId = event.getUser().getIdLong();
        event.deferReply(true).queue();

        plugin.db().getUuidByDiscordId(discordId).thenAccept(uuidOpt -> {
            if (uuidOpt.isEmpty()) {
                event.getHook().editOriginal("Tu cuenta no esta vinculada. Usa `/vincular` en Minecraft primero.").queue();
                return;
            }
            UUID uuid = uuidOpt.get();
            plugin.friendService().getFriendIds(uuid).thenAccept(friendIds -> {
                plugin.db().getName(uuid).thenAccept(myName -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("**Lista de amigos de ").append(myName != null ? myName : uuid).append("**\n\n");
                    if (friendIds.isEmpty()) {
                        sb.append("_No tienes amigos aun._");
                    } else {
                        for (UUID friendId : friendIds) {
                            CompletableFuture<String> nameFuture = plugin.db().getName(friendId);
                            nameFuture.thenAccept(name -> {
                                if (name == null) return;
                                String serverName = plugin.proxy().getPlayer(friendId)
                                    .flatMap(Player::getCurrentServer)
                                    .map(sc -> plugin.config().getServerName(sc.getServerInfo().getName()))
                                    .orElse(null);
                                sb.append(serverName != null ? "\uD83D\uDFE2 " : "\u26AA ")
                                  .append(serverName != null ? "**" + name + "**" : name)
                                  .append(serverName != null ? " \u2014 " + serverName : " \u2014 no esta jugando")
                                  .append("\n");
                            }).join();
                        }
                    }
                    event.getHook().editOriginal(sb.toString()).queue();
                });
            });
        });
    }
}
