package com.nulvora.friends.common.dto;

import com.google.gson.annotations.SerializedName;

public record JoinRequestPayload(
    @SerializedName("player") String playerUuid,
    @SerializedName("target") String targetUuid
) {}
