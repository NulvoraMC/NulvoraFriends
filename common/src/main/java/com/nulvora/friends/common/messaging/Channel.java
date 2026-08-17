package com.nulvora.friends.common.messaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nulvora.friends.common.dto.FriendDataPayload;
import com.nulvora.friends.common.dto.FriendNotificationPayload;
import com.nulvora.friends.common.dto.JoinRequestPayload;
import java.nio.charset.StandardCharsets;

public final class Channel {

    public static final String CHANNEL_NAME = "nulfriends:main";

    public static final String MSG_FRIEND_DATA = "friends_data";
    public static final String MSG_FRIEND_NOTIFICATION = "friend_notification";
    public static final String MSG_JOIN_REQUEST = "join_request";
    public static final String MSG_PING = "ping";
    public static final String MSG_PONG = "pong";

    private static final Gson GSON = new GsonBuilder().create();

    private Channel() {}

    public static byte[] encode(String type, String jsonPayload) {
        String envelope = "{\"type\":\"" + type + "\",\"payload\":" + jsonPayload + "}";
        return envelope.getBytes(StandardCharsets.UTF_8);
    }

    public static String decode(byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }

    public static String toJson(FriendDataPayload payload) {
        return GSON.toJson(payload);
    }

    public static String toJson(FriendNotificationPayload payload) {
        return GSON.toJson(payload);
    }

    public static String toJson(JoinRequestPayload payload) {
        return GSON.toJson(payload);
    }

    public static Gson gson() {
        return GSON;
    }
}
