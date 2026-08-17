package com.nulvora.friends.common.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public record FriendDataPayload(
    @SerializedName("player") String playerUuid,
    @SerializedName("friends") List<FriendEntry> friends
) {
    public record FriendEntry(
        @SerializedName("uuid") String uuid,
        @SerializedName("name") String name,
        @SerializedName("online") boolean online,
        @SerializedName("server") String server
    ) {}
}
