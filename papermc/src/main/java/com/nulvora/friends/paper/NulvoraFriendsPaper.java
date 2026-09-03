package com.nulvora.friends.paper;

import com.nulvora.friends.paper.api.NulvoraFriendsApi;
import com.nulvora.friends.paper.cache.FriendCache;
import com.nulvora.friends.paper.cache.PartyCache;
import com.nulvora.friends.paper.gui.FriendsGUI;
import com.nulvora.friends.paper.listener.NotificationListener;
import com.nulvora.friends.paper.redis.RedisService;
import com.nulvora.friends.paper.party.PartyApiImpl;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class NulvoraFriendsPaper extends JavaPlugin {

    private FriendCache friendCache;
    private PartyCache partyCache;
    private FriendsGUI friendsGUI;
    private RedisService redis;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        friendCache = new FriendCache();
        partyCache = new PartyCache();
        friendsGUI = new FriendsGUI(this);

        // Exponer API singleton
        NulvoraFriendsApi.setInstance(new NulvoraFriendsApi(new PartyApiImpl(partyCache, this)));

        try {
            redis = new RedisService(this);
        } catch (Exception e) {
            getLogger().severe("No se pudo conectar a Redis: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new NotificationListener(this), this);

        // Registrar listener para PlaceholderAPI (carga después de este plugin con loadbefore)
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPluginEnable(PluginEnableEvent event) {
                if (event.getPlugin().getName().equals("PlaceholderAPI")) {
                    com.nulvora.friends.paper.placeholder.PlaceholderAPIHook.register(
                        NulvoraFriendsPaper.this);
                }
            }
        }, this);

        // También intentar registrar ahora si PlaceholderAPI ya está cargado
        com.nulvora.friends.paper.placeholder.PlaceholderAPIHook.register(this);

        getCommand("amigos").setExecutor((sender, command, label, args) -> {
            if (sender instanceof org.bukkit.entity.Player player) {
                friendsGUI.open(player);
            } else {
                sender.sendMessage("Este comando solo puede ser ejecutado por jugadores.");
            }
            return true;
        });

        getLogger().info("NulvoraFriends-Paper enabled.");
    }

    @Override
    public void onDisable() {
        NulvoraFriendsApi.clearInstance();

        if (redis != null) redis.close();
        if (friendCache != null) {
            friendCache.clear();
        }
        if (partyCache != null) {
            partyCache.clear();
        }
        getLogger().info("NulvoraFriends-Paper disabled.");
    }

    public FriendCache friendCache() { return friendCache; }
    public PartyCache partyCache() { return partyCache; }
    public FriendsGUI friendsGUI() { return friendsGUI; }
    public RedisService redis() { return redis; }

    /**
     * Retorna la API singleton de NulvoraFriends.
     * Equivalente a {@code NulvoraFriendsApi.get()} pero accesible desde la instancia del plugin.
     *
     * @return la API de NulvoraFriends, o {@code null} si el plugin no está activo
     */
    public NulvoraFriendsApi getApi() {
        return NulvoraFriendsApi.get();
    }
}
