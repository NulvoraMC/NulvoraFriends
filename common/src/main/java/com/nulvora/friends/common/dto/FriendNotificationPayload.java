package com.nulvora.friends.common.dto;

import com.google.gson.annotations.SerializedName;

public record FriendNotificationPayload(
    @SerializedName("player") String playerUuid,
    @SerializedName("type") String type,
    @SerializedName("friend_uuid") String friendUuid,
    @SerializedName("friend_name") String friendName,
    @SerializedName("server") String serverName
) {}
