package com.nulvora.friends.velocity.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.nulvora.friends.velocity.NulvoraFriendsPlugin;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class AmigosCommand {

    private final NulvoraFriendsPlugin plugin;

    public AmigosCommand(NulvoraFriendsPlugin plugin) {
        this.plugin = plugin;
    }

    public BrigadierCommand create() {
        LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder.<CommandSource>literal("amigos")
            .executes(ctx -> {
                if (ctx.getSource() instanceof Player player) {
                    executeList(player);
                } else {
                    ctx.getSource().sendMessage(colorize("&cSolo jugadores."));
                }
                return 1;
            });

        builder.then(LiteralArgumentBuilder.<CommandSource>literal("add")
            .then(RequiredArgumentBuilder.<CommandSource, String>argument("jugador", StringArgumentType.word())
                .executes(ctx -> {
                    if (ctx.getSource() instanceof Player player) {
                        executeAdd(player, ctx.getArgument("jugador", String.class));
                    }
                    return 1;
                })));

        builder.then(LiteralArgumentBuilder.<CommandSource>literal("aceptar")
            .then(RequiredArgumentBuilder.<CommandSource, String>argument("jugador", StringArgumentType.word())
                .executes(ctx -> {
                    if (ctx.getSource() instanceof Player player) {
                        executeAccept(player, ctx.getArgument("jugador", String.class));
                    }
                    return 1;
                })));

        builder.then(LiteralArgumentBuilder.<CommandSource>literal("rechazar")
            .then(RequiredArgumentBuilder.<CommandSource, String>argument("jugador", StringArgumentType.word())
                .executes(ctx -> {
                    if (ctx.getSource() instanceof Player player) {
                        executeDeny(player, ctx.getArgument("jugador", String.class));
                    }
                    return 1;
                })));

        builder.then(LiteralArgumentBuilder.<CommandSource>literal("eliminar")
            .then(RequiredArgumentBuilder.<CommandSource, String>argument("jugador", StringArgumentType.word())
                .executes(ctx -> {
                    if (ctx.getSource() instanceof Player player) {
                        executeRemove(player, ctx.getArgument("jugador", String.class));
                    }
                    return 1;
                })));

        builder.then(LiteralArgumentBuilder.<CommandSource>literal("join")
            .then(RequiredArgumentBuilder.<CommandSource, String>argument("amigo", StringArgumentType.word())
                .executes(ctx -> {
                    if (ctx.getSource() instanceof Player player) {
                        executeJoin(player, ctx.getArgument("amigo", String.class));
                    }
                    return 1;
                })));

        builder.then(LiteralArgumentBuilder.<CommandSource>literal("solicitudes")
            .executes(ctx -> {
                if (ctx.getSource() instanceof Player player) {
                    executeRequests(player);
                }
                return 1;
            }));

        builder.then(LiteralArgumentBuilder.<CommandSource>literal("menu")
            .executes(ctx -> {
                if (ctx.getSource() instanceof Player player) {
                    plugin.presence().sendOpenMenu(player);
                }
                return 1;
            }));

        return new BrigadierCommand(builder.build());
    }

    private void executeList(Player player) {
        plugin.friendService().getFriendIds(player.getUniqueId()).thenAccept(friendIds -> {
            if (friendIds.isEmpty()) {
                player.sendMessage(colorize("&7No tienes amigos. Usa &e/amigos add <jugador>"));
                return;
            }
            player.sendMessage(colorize(""));
            player.sendMessage(colorize("&6&lTus amigos &7(" + friendIds.size() + "/" + plugin.config().maxFriends() + ")"));
            for (UUID id : friendIds) {
                plugin.db().getName(id).thenAccept(name -> {
                    if (name == null) return;
                    String serverName = plugin.proxy().getPlayer(id)
                        .flatMap(Player::getCurrentServer)
                        .map(sc -> plugin.config().getServerName(sc.getServerInfo().getName()))
                        .orElse(null);
                    Component line;
                    if (serverName != null) {
                        line = colorize("&a\u25cf &e" + name + " &7- &a" + serverName);
                    } else {
                        line = colorize("&7\u25cb &8" + name + " &7- no esta jugando");
                    }
                    player.sendMessage(line);
                });
            }
            player.sendMessage(colorize(""));
        });
    }

    private void executeAdd(Player player, String targetName) {
        if (player.getUsername().equalsIgnoreCase(targetName)) {
            player.sendMessage(colorize(plugin.config().messages().selfRequest()));
            return;
        }
        plugin.proxy().getPlayer(targetName).ifPresentOrElse(target -> {
            plugin.friendService().sendRequest(player.getUniqueId(), target.getUniqueId()).thenAccept(sent -> {
                if (!sent) {
                    plugin.db().areFriends(player.getUniqueId(), target.getUniqueId()).thenAccept(friends -> {
                        player.sendMessage(colorize(friends
                            ? plugin.config().messages().alreadyFriends()
                            : "&cNo se pudo enviar la solicitud."));
                    });
                    return;
                }
                player.sendMessage(colorize(plugin.config().messages().friendRequestSent().replace("%player%", targetName)));
                target.sendMessage(colorize(plugin.config().messages().friendRequestReceived().replace("%player%", player.getUsername())));
            });
        }, () -> player.sendMessage(colorize(plugin.config().messages().playerOffline().replace("%player%", targetName))));
    }

    private void executeAccept(Player player, String senderName) {
        plugin.proxy().getPlayer(senderName).ifPresentOrElse(sender -> {
            plugin.friendService().acceptRequest(sender.getUniqueId(), player.getUniqueId()).thenAccept(accepted -> {
                if (accepted) {
                    player.sendMessage(colorize(plugin.config().messages().friendAccepted().replace("%player%", senderName)));
                    sender.sendMessage(colorize(plugin.config().messages().friendAccepted().replace("%player%", player.getUsername())));
                    plugin.presence().sendFriendDataToBackend(player);
                    plugin.presence().sendFriendDataToBackend(sender);
                } else {
                    player.sendMessage(colorize("&cNo tienes solicitud pendiente de &e" + senderName));
                }
            });
        }, () -> player.sendMessage(colorize(plugin.config().messages().playerOffline().replace("%player%", senderName))));
    }

    private void executeDeny(Player player, String senderName) {
        plugin.proxy().getPlayer(senderName).ifPresentOrElse(sender -> {
            plugin.friendService().denyRequest(sender.getUniqueId(), player.getUniqueId()).thenAccept(denied -> {
                if (denied) {
                    player.sendMessage(colorize("&7Solicitud de &e" + senderName + " &7rechazada."));
                    sender.sendMessage(colorize("&e" + player.getUsername() + " &crechazo tu solicitud."));
                } else {
                    player.sendMessage(colorize("&cNo tienes solicitud pendiente de &e" + senderName));
                }
            });
        }, () -> player.sendMessage(colorize("&cJugador no encontrado.")));
    }

    private void executeRemove(Player player, String friendName) {
        plugin.proxy().getPlayer(friendName).ifPresentOrElse(friend -> {
            plugin.friendService().removeFriend(player.getUniqueId(), friend.getUniqueId()).thenAccept(removed -> {
                if (removed) {
                    player.sendMessage(colorize(plugin.config().messages().friendRemoved().replace("%player%", friendName)));
                    friend.sendMessage(colorize(plugin.config().messages().friendRemovedBy().replace("%player%", player.getUsername())));
                    plugin.presence().sendFriendDataToBackend(player);
                    plugin.presence().sendFriendDataToBackend(friend);
                } else {
                    player.sendMessage(colorize(plugin.config().messages().notFriends().replace("%player%", friendName)));
                }
            });
        }, () -> player.sendMessage(colorize(plugin.config().messages().playerNotFound().replace("%player%", friendName))));
    }

    private void executeJoin(Player player, String friendName) {
        plugin.proxy().getPlayer(friendName).ifPresentOrElse(friend -> {
            friend.getCurrentServer().ifPresentOrElse(sc -> {
                plugin.proxy().getServer(sc.getServerInfo().getName()).ifPresent(server -> {
                    player.createConnectionRequest(server).fireAndForget();
                    player.sendMessage(colorize("&aConectandote al servidor de &e" + friendName + "&a..."));
                });
            }, () -> player.sendMessage(colorize(plugin.config().messages().playerOffline().replace("%player%", friendName))));
        }, () -> player.sendMessage(colorize(plugin.config().messages().playerOffline().replace("%player%", friendName))));
    }

    private void executeRequests(Player player) {
        plugin.friendService().getPendingRequests(player.getUniqueId()).thenAccept(requests -> {
            if (requests.isEmpty()) {
                player.sendMessage(colorize("&7No tienes solicitudes pendientes."));
                return;
            }
            player.sendMessage(colorize("&6&lSolicitudes pendientes:"));
            for (UUID id : requests) {
                plugin.db().getName(id).thenAccept(name -> {
                    if (name == null) return;
                    Component line = Component.text()
                        .content("- " + name + " ")
                        .color(NamedTextColor.YELLOW)
                        .append(Component.text("[aceptar]").color(NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/amigos aceptar " + name))
                            .hoverEvent(HoverEvent.showText(Component.text("Aceptar solicitud", NamedTextColor.GREEN))))
                        .append(Component.text(" "))
                        .append(Component.text("[rechazar]").color(NamedTextColor.RED)
                            .clickEvent(ClickEvent.runCommand("/amigos rechazar " + name))
                            .hoverEvent(HoverEvent.showText(Component.text("Rechazar solicitud", NamedTextColor.RED))))
                        .build();
                    player.sendMessage(line);
                });
            }
            player.sendMessage(colorize(""));
        });
    }

    private Component colorize(String message) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacyAmpersand().deserialize(message);
    }
}
