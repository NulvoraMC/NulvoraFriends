package com.nulvora.friends.velocity.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.nulvora.friends.velocity.NulvoraFriendsPlugin;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

public class LinkCommand {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final long CODE_EXPIRY_MS = 5 * 60 * 1000;
    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();

    private final NulvoraFriendsPlugin plugin;

    public LinkCommand(NulvoraFriendsPlugin plugin) {
        this.plugin = plugin;
    }

    public BrigadierCommand create() {
        LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder.<CommandSource>literal("vincular")
            .executes(ctx -> {
                if (ctx.getSource() instanceof Player player) {
                    executeLink(player);
                }
                return 1;
            });
        return new BrigadierCommand(builder.build());
    }

    private void executeLink(Player player) {
        plugin.db().isLinked(player.getUniqueId()).thenAccept(linked -> {
            if (linked) {
                player.sendMessage(colorize(plugin.config().messages().alreadyLinked()));
                return;
            }
            String code = generateCode();
            plugin.db().storeLinkCode(code, player.getUniqueId(), CODE_EXPIRY_MS).thenRun(() ->
                player.sendMessage(colorize(plugin.config().messages().linkCodeGenerated().replace("%code%", code)))
            );
        });
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }

    private net.kyori.adventure.text.Component colorize(String message) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
            .legacyAmpersand().deserialize(message);
    }
}
