package com.nulvora.friends.paper.placeholder;

import com.nulvora.friends.paper.NulvoraFriendsPaper;
import com.nulvora.friends.paper.cache.FriendCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FriendsPlaceholder extends PlaceholderExpansion {

    private final NulvoraFriendsPaper plugin;

    public FriendsPlaceholder(NulvoraFriendsPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "nulfriends";
    }

    @Override
    public @NotNull String getAuthor() {
        return "nulvora";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        List<FriendCache.CachedFriend> friends = plugin.friendCache().getFriends(player.getUniqueId());

        return switch (params.toLowerCase()) {
            case "online" -> String.valueOf(
                friends.stream().filter(FriendCache.CachedFriend::online).count());
            case "total" -> String.valueOf(friends.size());
            case "pending" -> "0";
            case "online_names" -> {
                String names = friends.stream()
                    .filter(FriendCache.CachedFriend::online)
                    .map(FriendCache.CachedFriend::name)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
                yield names.isEmpty() ? "Nadie" : names;
            }
            case "list" -> {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < friends.size(); i++) {
                    if (i > 0) sb.append("\n");
                    FriendCache.CachedFriend f = friends.get(i);
                    sb.append(f.online() ? "\u25cf " : "\u25cb ")
                      .append(f.name());
                    if (f.online() && f.server() != null) {
                        sb.append(" - ").append(f.server());
                    }
                }
                yield sb.toString();
            }
            default -> null;
        };
    }
}
