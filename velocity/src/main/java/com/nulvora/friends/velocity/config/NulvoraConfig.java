package com.nulvora.friends.velocity.config;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;

public class NulvoraConfig {

    @SerializedName("mysql")
    private MysqlConfig mysql = new MysqlConfig();

    @SerializedName("discord")
    private DiscordConfig discord = new DiscordConfig();

    @SerializedName("server-names")
    private Map<String, String> serverNames = new HashMap<>();

    @SerializedName("max-friends")
    private int maxFriends = 100;

    @SerializedName("messages")
    private MessagesConfig messages = new MessagesConfig();

    public MysqlConfig mysql() { return mysql; }
    public DiscordConfig discord() { return discord; }
    public Map<String, String> serverNames() { return serverNames; }
    public int maxFriends() { return maxFriends; }
    public MessagesConfig messages() { return messages; }

    public String getServerName(String serverId) {
        return serverNames.getOrDefault(serverId, serverId);
    }

    public static class MysqlConfig {
        @SerializedName("host") private String host = "localhost";
        @SerializedName("port") private int port = 3306;
        @SerializedName("database") private String database = "nulvorafriends";
        @SerializedName("user") private String user = "root";
        @SerializedName("password") private String password = "";
        @SerializedName("pool-size") private int poolSize = 5;

        public String host() { return host; }
        public int port() { return port; }
        public String database() { return database; }
        public String user() { return user; }
        public String password() { return password; }
        public int poolSize() { return poolSize; }
    }

    public static class DiscordConfig {
        @SerializedName("enabled") private boolean enabled = false;
        @SerializedName("token") private String token = "";
        @SerializedName("guild-id") private long guildId = 0L;
        @SerializedName("linked-role-id") private long linkedRoleId = 0L;

        public boolean enabled() { return enabled; }
        public String token() { return token; }
        public long guildId() { return guildId; }
        public long linkedRoleId() { return linkedRoleId; }
    }

    public static class MessagesConfig {
        @SerializedName("friend-request-sent") private String friendRequestSent = "&aSolicitud enviada a &e%player%&a.";
        @SerializedName("friend-request-received") private String friendRequestReceived = "&e%player% &aquiere ser tu amigo. Usa &e/amigos aceptar %player%";
        @SerializedName("friend-accepted") private String friendAccepted = "&aAhora eres amigo de &e%player%&a.";
        @SerializedName("friend-removed") private String friendRemoved = "&cHas eliminado a &e%player% &cde tu lista de amigos.";
        @SerializedName("friend-removed-by") private String friendRemovedBy = "&e%player% &celiminó de su lista de amigos.";
        @SerializedName("already-friends") private String alreadyFriends = "&cYa eres amigo de &e%player%&c.";
        @SerializedName("not-friends") private String notFriends = "&cNo eres amigo de &e%player%&c.";
        @SerializedName("self-request") private String selfRequest = "&cNo puedes agregarte a ti mismo.";
        @SerializedName("friend-full") private String friendFull = "&cTu lista de amigos está llena (máximo %max%).";
        @SerializedName("target-friend-full") private String targetFriendFull = "&cLa lista de amigos de &e%player% &cestá llena.";
        @SerializedName("player-offline") private String playerOffline = "&cEl jugador &e%player% &cestá desconectado.";
        @SerializedName("player-not-found") private String playerNotFound = "&cJugador no encontrado.";
        @SerializedName("link-code-generated") private String linkCodeGenerated = "&aTu código de vinculación es: &e%code% &a(válido 5 min). &6Envíalo al bot de Discord.";
        @SerializedName("link-success") private String linkSuccess = "&aCuenta vinculada con &e%discord%&a.";
        @SerializedName("unlink-success") private String unlinkSuccess = "&aCuenta de Discord desvinculada.";
        @SerializedName("not-linked") private String notLinked = "&cTu cuenta no está vinculada con Discord.";
        @SerializedName("already-linked") private String alreadyLinked = "&cTu cuenta ya está vinculada. Usa &e/desvincular &cprimero.";
        @SerializedName("friend-join-network") private String friendJoinNetwork = "&a&l+ &e%player% &ase ha conectado a la red.";
        @SerializedName("friend-leave-network") private String friendLeaveNetwork = "&c&l- &e%player% &ase ha desconectado.";
        @SerializedName("friend-server-change") private String friendServerChange = "&e%friend% &ahas entrado a &e%server%&a.";

        public String friendRequestSent() { return friendRequestSent; }
        public String friendRequestReceived() { return friendRequestReceived; }
        public String friendAccepted() { return friendAccepted; }
        public String friendRemoved() { return friendRemoved; }
        public String friendRemovedBy() { return friendRemovedBy; }
        public String alreadyFriends() { return alreadyFriends; }
        public String notFriends() { return notFriends; }
        public String selfRequest() { return selfRequest; }
        public String friendFull() { return friendFull; }
        public String targetFriendFull() { return targetFriendFull; }
        public String playerOffline() { return playerOffline; }
        public String playerNotFound() { return playerNotFound; }
        public String linkCodeGenerated() { return linkCodeGenerated; }
        public String linkSuccess() { return linkSuccess; }
        public String unlinkSuccess() { return unlinkSuccess; }
        public String notLinked() { return notLinked; }
        public String alreadyLinked() { return alreadyLinked; }
        public String friendJoinNetwork() { return friendJoinNetwork; }
        public String friendLeaveNetwork() { return friendLeaveNetwork; }
        public String friendServerChange() { return friendServerChange; }
    }
}
