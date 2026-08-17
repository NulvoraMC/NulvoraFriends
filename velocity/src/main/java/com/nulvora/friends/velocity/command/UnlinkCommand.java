package com.nulvora.friends.velocity.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.nulvora.friends.velocity.NulvoraFriendsPlugin;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

public class UnlinkCommand {

    private final NulvoraFriendsPlugin plugin;

    public UnlinkCommand(NulvoraFriendsPlugin plugin) {
        this.plugin = plugin;
    }

    public BrigadierCommand create() {
        LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder.<CommandSource>literal("desvincular")
            .executes(ctx -> {
                if (ctx.getSource() instanceof Player player) {
                    executeUnlink(player);
                }
                return 1;
            });
        return new BrigadierCommand(builder.build());
    }

    private void executeUnlink(Player player) {
        plugin.db().isLinked(player.getUniqueId()).thenAccept(linked -> {
            if (!linked) {
                player.sendMessage(colorize(plugin.config().messages().notLinked()));
                return;
            }
            plugin.db().getDiscordId(player.getUniqueId()).thenAccept(discordId ->
                plugin.db().unlinkAccounts(player.getUniqueId()).thenRun(() -> {
                    plugin.discordBot().ifPresent(bot -> discordId.ifPresent(bot::removeLinkedRole));
                    player.sendMessage(colorize(plugin.config().messages().unlinkSuccess()));
                })
            );
        });
    }

    private net.kyori.adventure.text.Component colorize(String message) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacyAmpersand().deserialize(message);
    }
}
