package com.nulvora.friends.common.redis;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Shared JSON protocol used over Redis keys and pub/sub. */
public final class RedisProtocol {
    public static final String FRIEND_DATA = "friends_data";
    public static final String FRIEND_NOTIFICATION = "friend_notification";
    public static final String OPEN_MENU = "open_menu";
    public static final String JOIN_REQUEST = "join_request";
    public static final String PARTY_DATA = "party_data";
    public static final String PARTY_WARP = "party_warp";

    private static final Gson GSON = new GsonBuilder().create();

    private RedisProtocol() {}

    public static String envelope(String type, Object payload) {
        JsonObject value = new JsonObject();
        value.addProperty("type", type);
        value.add("payload", GSON.toJsonTree(payload));
        return GSON.toJson(value);
    }

    public static Message decode(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return new Message(root.get("type").getAsString(), root.getAsJsonObject("payload"));
    }

    public static Gson gson() { return GSON; }

    public record Message(String type, JsonObject payload) {}
}
