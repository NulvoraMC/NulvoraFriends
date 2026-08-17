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

    @SerializedName("extensions")
    private ExtensionsConfig extensions = new ExtensionsConfig();

    @SerializedName("party")
    private PartyConfig party = new PartyConfig();

    public MysqlConfig mysql() { return mysql; }
    public DiscordConfig discord() { return discord; }
    public Map<String, String> serverNames() { return serverNames; }
    public int maxFriends() { return maxFriends; }
    public MessagesConfig messages() { return messages; }
    public ExtensionsConfig extensions() { return extensions; }
    public PartyConfig party() { return party; }

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
        @SerializedName("party-invitation-received") private String partyInvitationReceived = "&e%player% &ate ha invitado a una party. Usa &e/party aceptar %player%";
        @SerializedName("party-invitation-sent") private String partyInvitationSent = "&aInvitación enviada a &e%player%&a.";
        @SerializedName("party-joined") private String partyJoined = "&aTe has unido a la party de &e%leader%&a.";
        @SerializedName("party-player-joined") private String partyPlayerJoined = "&e%player% &ase ha unido a la party.";
        @SerializedName("party-left") private String partyLeft = "&aHas abandonado la party.";
        @SerializedName("party-player-left") private String partyPlayerLeft = "&e%player% &aha abandonado la party.";
        @SerializedName("party-disbanded") private String partyDisbanded = "&cLa party se ha disuelto.";
        @SerializedName("party-leader-changed") private String partyLeaderChanged = "&e%player% &aes el nuevo líder de la party.";
        @SerializedName("party-follow") private String partyFollow = "&aTu party se ha movido a &e%server%&a.";
        @SerializedName("party-not-in-party") private String partyNotInParty = "&cNo estás en ninguna party.";
        @SerializedName("party-already-in-party") private String partyAlreadyInParty = "&cYa estás en una party.";
        @SerializedName("party-full") private String partyFull = "&cLa party está llena (máximo %max%).";
        @SerializedName("party-invite-expired") private String partyInviteExpired = "&cLa invitación ha expirado.";
        @SerializedName("party-invite-not-found") private String partyInviteNotFound = "&cNo tienes invitación pendiente de &e%player%&c.";
        @SerializedName("party-kicked") private String partyKicked = "&cHas sido expulsado de la party.";
        @SerializedName("party-player-kicked") private String partyPlayerKicked = "&e%player% &aha sido expulsado de la party.";
        @SerializedName("party-not-leader") private String partyNotLeader = "&cNo eres el líder de la party.";
        @SerializedName("party-toofew") private String partyToofew = "&cLa party necesita al menos 2 jugadores para moverse.";

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
        public String partyInvitationReceived() { return partyInvitationReceived; }
        public String partyInvitationSent() { return partyInvitationSent; }
        public String partyJoined() { return partyJoined; }
        public String partyPlayerJoined() { return partyPlayerJoined; }
        public String partyLeft() { return partyLeft; }
        public String partyPlayerLeft() { return partyPlayerLeft; }
        public String partyDisbanded() { return partyDisbanded; }
        public String partyLeaderChanged() { return partyLeaderChanged; }
        public String partyFollow() { return partyFollow; }
        public String partyNotInParty() { return partyNotInParty; }
        public String partyAlreadyInParty() { return partyAlreadyInParty; }
        public String partyFull() { return partyFull; }
        public String partyInviteExpired() { return partyInviteExpired; }
        public String partyInviteNotFound() { return partyInviteNotFound; }
        public String partyKicked() { return partyKicked; }
        public String partyPlayerKicked() { return partyPlayerKicked; }
        public String partyNotLeader() { return partyNotLeader; }
        public String partyToofew() { return partyToofew; }
    }

    public static class ExtensionsConfig {
        @SerializedName("enabled") private boolean enabled = true;
        @SerializedName("command-timeout-ms") private long commandTimeoutMs = 10000;

        public boolean enabled() { return enabled; }
        public long commandTimeoutMs() { return commandTimeoutMs; }
    }

    public static class PartyConfig {
        @SerializedName("enabled") private boolean enabled = true;
        @SerializedName("max-size") private int maxSize = 10;
        @SerializedName("invite-expire-seconds") private int inviteExpireSeconds = 60;
        @SerializedName("follow") private FollowConfig follow = new FollowConfig();

        public boolean enabled() { return enabled; }
        public int maxSize() { return maxSize; }
        public int inviteExpireSeconds() { return inviteExpireSeconds; }
        public FollowConfig follow() { return follow; }
    }

    public static class FollowConfig {
        @SerializedName("enabled") private boolean enabled = true;
        @SerializedName("cooldown-ms") private long cooldownMs = 5000;

        public boolean enabled() { return enabled; }
        public long cooldownMs() { return cooldownMs; }
    }
}
