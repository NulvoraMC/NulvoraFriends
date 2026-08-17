package com.nulvora.friends.velocity.party;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.nulvora.friends.velocity.NulvoraFriendsPlugin;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import java.util.UUID;

public class PartyCommand {

    private final NulvoraFriendsPlugin plugin;

    public PartyCommand(NulvoraFriendsPlugin plugin) {
        this.plugin = plugin;
    }

    public BrigadierCommand create() {
        LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder.<CommandSource>literal("party")
            .executes(ctx -> {
                if (ctx.getSource() instanceof Player player) {
                    executeList(player);
                } else {
                    ctx.getSource().sendMessage(colorize("&cSolo jugadores."));
                }
                return 1;
            });

        builder.then(LiteralArgumentBuilder.<CommandSource>literal("invitar")
            .then(RequiredArgumentBuilder.<CommandSource, String>argument("jugador", StringArgumentType.word())
                .executes(ctx -> {
                    if (ctx.getSource() instanceof Player player) {
                        executeInvite(player, ctx.getArgument("jugador", String.class));
                    }
                    return 1;
                })));

        builder.then(LiteralArgumentBuilder.<CommandSource>literal("aceptar")
            .executes(ctx -> {
                if (ctx.getSource() instanceof Player player) {
                    executeAcceptLatest(player);
                }
                return 1;
            })
            .then(RequiredArgumentBuilder.<CommandSource, String>argument("jugador", StringArgumentType.word())
                .executes(ctx -> {
                    if (ctx.getSource() instanceof Player player) {
                        executeAccept(player, ctx.getArgument("jugador", String.class));
                    }
                    return 1;
                })));

        builder.then(LiteralArgumentBuilder.<CommandSource>literal("rechazar")
            .executes(ctx -> {
                if (ctx.getSource() instanceof Player player) {
                    executeDenyLatest(player);
                }
                return 1;
            })
            .then(RequiredArgumentBuilder.<CommandSource, String>argument("jugador", StringArgumentType.word())
                .executes(ctx -> {
                    if (ctx.getSource() instanceof Player player) {
                        executeDeny(player, ctx.getArgument("jugador", String.class));
                    }
                    return 1;
                })));

        builder.then(LiteralArgumentBuilder.<CommandSource>literal("abandonar")
            .executes(ctx -> {
                if (ctx.getSource() instanceof Player player) {
                    executeLeave(player);
                }
                return 1;
            }));

        builder.then(LiteralArgumentBuilder.<CommandSource>literal("expulsar")
            .then(RequiredArgumentBuilder.<CommandSource, String>argument("jugador", StringArgumentType.word())
                .executes(ctx -> {
                    if (ctx.getSource() instanceof Player player) {
                        executeKick(player, ctx.getArgument("jugador", String.class));
                    }
                    return 1;
                })));

        builder.then(LiteralArgumentBuilder.<CommandSource>literal("disolver")
            .executes(ctx -> {
                if (ctx.getSource() instanceof Player player) {
                    executeDisband(player);
                }
                return 1;
            }));

        builder.then(LiteralArgumentBuilder.<CommandSource>literal("warp")
            .executes(ctx -> {
                if (ctx.getSource() instanceof Player player) {
                    executeWarp(player);
                }
                return 1;
            }));

        return new BrigadierCommand(builder.build());
    }

    // ─── Subcommands ────────────────────────────────────────────────────

    private void executeList(Player player) {
        plugin.partyService().getParty(player.getUniqueId()).ifPresentOrElse(party -> {
            plugin.db().getName(party.leader()).thenAccept(leaderName -> {
                player.sendMessage(colorize(""));
                player.sendMessage(colorize("&6&lParty &7(ID: " + party.id().toString().substring(0, 8) + ")"));
                player.sendMessage(colorize("&7Líder: &e" + (leaderName != null ? leaderName : "?") + (party.isLeader(player.getUniqueId()) ? " &7(tú)" : "")));
                player.sendMessage(colorize("&7Miembros &e" + party.size() + "/" + plugin.config().party().maxSize()));
                for (UUID id : party.getMembers()) {
                    plugin.db().getName(id).thenAccept(name -> {
                        if (name == null) return;
                        String serverName = plugin.proxy().getPlayer(id)
                            .flatMap(Player::getCurrentServer)
                            .map(sc -> plugin.config().getServerName(sc.getServerInfo().getName()))
                            .orElse(null);
                        String suffix = party.isLeader(id) ? " &7[&6líder&7]" : "";
                        if (serverName != null) {
                            player.sendMessage(colorize("&a\u25cf &e" + name + suffix + " &7- &a" + serverName));
                        } else {
                            player.sendMessage(colorize("&7\u25cb &8" + name + suffix + " &7- offline"));
                        }
                    });
                }
                player.sendMessage(colorize(""));
            });
        }, () -> player.sendMessage(colorize(plugin.config().messages().partyNotInParty())));
    }

    private void executeInvite(Player player, String targetName) {
        plugin.proxy().getPlayer(targetName).ifPresentOrElse(target -> {
            plugin.partyService().inviteOrCreate(player.getUniqueId(), target.getUniqueId())
                .exceptionally(ex -> {
                    handleInviteError(player, ex);
                    return null;
                });
        }, () -> player.sendMessage(colorize(
            plugin.config().messages().playerOffline().replace("%player%", targetName))));
    }

    private void handleInviteError(Player player, Throwable ex) {
        String msg = ex.getMessage();
        if (msg == null) return;
        switch (msg) {
            case "not_leader" -> player.sendMessage(colorize(plugin.config().messages().partyNotLeader()));
            case "party_full" -> player.sendMessage(colorize(
                plugin.config().messages().partyFull()
                    .replace("%max%", String.valueOf(plugin.config().party().maxSize()))));
            case "already_in_party" -> player.sendMessage(colorize(plugin.config().messages().partyAlreadyInParty()));
            case "target_in_party" -> player.sendMessage(colorize("&cEse jugador ya está en una party."));
        }
    }

    private void executeAcceptLatest(Player player) {
        plugin.partyService().getParty(player.getUniqueId()).ifPresentOrElse(
            party -> player.sendMessage(colorize(plugin.config().messages().partyAlreadyInParty())),
            () -> {
                var pending = plugin.partyService().getPendingInvites(player.getUniqueId());
                if (pending.isEmpty()) {
                    player.sendMessage(colorize("&cNo tienes invitaciones pendientes."));
                    return;
                }
                plugin.partyService().accept(player.getUniqueId(), pending.get(0))
                    .thenAccept(accepted -> {
                        if (!accepted) {
                            player.sendMessage(colorize(plugin.config().messages().partyInviteExpired()));
                        }
                    });
            });
    }

    private void executeAccept(Player player, String inviterName) {
        plugin.proxy().getPlayer(inviterName).ifPresentOrElse(inviter -> {
            plugin.partyService().getParty(inviter.getUniqueId()).ifPresentOrElse(party -> {
                plugin.partyService().accept(player.getUniqueId(), party.id())
                    .thenAccept(accepted -> {
                        if (!accepted) {
                            player.sendMessage(colorize(
                                plugin.config().messages().partyInviteNotFound()
                                    .replace("%player%", inviterName)));
                        }
                    });
            }, () -> player.sendMessage(colorize(
                plugin.config().messages().partyInviteNotFound()
                    .replace("%player%", inviterName))));
        }, () -> player.sendMessage(colorize(
            plugin.config().messages().playerOffline().replace("%player%", inviterName))));
    }

    private void executeDenyLatest(Player player) {
        var pending = plugin.partyService().getPendingInvites(player.getUniqueId());
        if (pending.isEmpty()) {
            player.sendMessage(colorize("&cNo tienes invitaciones pendientes."));
            return;
        }
        plugin.partyService().deny(player.getUniqueId(), pending.get(0));
        player.sendMessage(colorize("&7Invitación rechazada."));
    }

    private void executeDeny(Player player, String inviterName) {
        plugin.proxy().getPlayer(inviterName).ifPresentOrElse(inviter -> {
            plugin.partyService().getParty(inviter.getUniqueId()).ifPresentOrElse(party -> {
                plugin.partyService().deny(player.getUniqueId(), party.id());
                player.sendMessage(colorize("&7Invitación de &e" + inviterName + " &7rechazada."));
            }, () -> player.sendMessage(colorize(
                plugin.config().messages().playerNotFound().replace("%player%", inviterName))));
        }, () -> player.sendMessage(colorize(
            plugin.config().messages().playerOffline().replace("%player%", inviterName))));
    }

    private void executeLeave(Player player) {
        plugin.partyService().leave(player.getUniqueId())
            .thenAccept(left -> {
                if (left) {
                    player.sendMessage(colorize(plugin.config().messages().partyLeft()));
                }
            })
            .exceptionally(ex -> {
                if ("not_in_party".equals(ex.getMessage())) {
                    player.sendMessage(colorize(plugin.config().messages().partyNotInParty()));
                }
                return null;
            });
    }

    private void executeKick(Player player, String targetName) {
        plugin.proxy().getPlayer(targetName).ifPresentOrElse(target -> {
            plugin.partyService().kick(player.getUniqueId(), target.getUniqueId())
                .exceptionally(ex -> {
                    handleKickError(player, ex);
                    return null;
                });
        }, () -> player.sendMessage(colorize(
            plugin.config().messages().playerOffline().replace("%player%", targetName))));
    }

    private void handleKickError(Player player, Throwable ex) {
        String msg = ex.getMessage();
        if (msg == null) return;
        switch (msg) {
            case "not_in_party" -> player.sendMessage(colorize(plugin.config().messages().partyNotInParty()));
            case "not_leader" -> player.sendMessage(colorize(plugin.config().messages().partyNotLeader()));
        }
    }

    private void executeDisband(Player player) {
        plugin.partyService().disband(player.getUniqueId())
            .exceptionally(ex -> {
                String msg = ex.getMessage();
                if ("not_in_party".equals(msg)) {
                    player.sendMessage(colorize(plugin.config().messages().partyNotInParty()));
                } else if ("not_leader".equals(msg)) {
                    player.sendMessage(colorize(plugin.config().messages().partyNotLeader()));
                }
                return null;
            });
    }

    private void executeWarp(Player player) {
        plugin.partyService().getParty(player.getUniqueId()).ifPresentOrElse(party -> {
            if (!party.isLeader(player.getUniqueId())) {
                player.sendMessage(colorize(plugin.config().messages().partyNotLeader()));
                return;
            }
            if (party.size() < 2) {
                player.sendMessage(colorize(plugin.config().messages().partyToofew()));
                return;
            }
            String targetServer = player.getCurrentServer()
                .map(sc -> sc.getServerInfo().getName())
                .orElse(null);
            if (targetServer == null) return;

            if (!plugin.config().serverNames().containsKey(targetServer)) {
                player.sendMessage(colorize("&cEste servidor no está en la lista de servidores permitidos."));
                return;
            }

            String displayName = plugin.config().getServerName(targetServer);
            plugin.partyService().warpMembers(player.getUniqueId(), targetServer, displayName);
            player.sendMessage(colorize("&aHas movido tu party a &e" + displayName + "&a."));
        }, () -> player.sendMessage(colorize(plugin.config().messages().partyNotInParty())));
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    private net.kyori.adventure.text.Component colorize(String message) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacyAmpersand().deserialize(message);
    }
}
