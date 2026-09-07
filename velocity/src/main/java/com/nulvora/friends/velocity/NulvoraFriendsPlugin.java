package com.nulvora.friends.velocity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nulvora.friends.velocity.command.AmigosCommand;
import com.nulvora.friends.velocity.config.NulvoraConfig;
import com.nulvora.friends.velocity.friends.FriendService;
import com.nulvora.friends.velocity.redis.RedisService;
import com.nulvora.friends.velocity.party.PartyCommand;
import com.nulvora.friends.velocity.party.PartyFollowListener;
import com.nulvora.friends.velocity.party.PartyListener;
import com.nulvora.friends.velocity.party.PartyService;
import com.nulvora.friends.velocity.presence.PresenceListener;
import com.nulvora.friends.velocity.storage.Database;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.inject.Inject;
import org.slf4j.Logger;

@Plugin(id = "nulfriends", name = "NulvoraFriends", version = "2.0.1",
        url = "https://github.com/nulvora/nulvorafriends",
        description = "Sistema de amigos distribuido con Redis para la network Nulvora")
public class NulvoraFriendsPlugin {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private NulvoraConfig config;
    private Database db;
    private FriendService friendService;
    private RedisService redis;
    private PartyService partyService;
    private PresenceListener presenceListener;
    private ExecutorService executor;

    @Inject
    public NulvoraFriendsPlugin(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = Path.of("plugins", "nulfriends");
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        executor = Executors.newFixedThreadPool(4);

        saveDefaultConfig();
        loadConfig();

        db = new Database(config);
        db.initialize();

        friendService = new FriendService(db);

        try {
            redis = new RedisService(this);
        } catch (Exception e) {
            db.close();
            executor.shutdownNow();
            throw new IllegalStateException("Could not connect to Redis; NulvoraFriends cannot start.", e);
        }

        if (config.party().enabled()) {
            partyService = new PartyService(this);
        }

        presenceListener = new PresenceListener(this);
        proxy.getEventManager().register(this, presenceListener);

        if (partyService != null) {
            proxy.getEventManager().register(this, new PartyListener(this));
            proxy.getEventManager().register(this, new PartyFollowListener(this));
        }

        proxy.getCommandManager().register(
            proxy.getCommandManager().metaBuilder("nulfriends:amigos").build(),
            new AmigosCommand(this).create()
        );
        if (partyService != null) {
            proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("nulfriends:party").build(),
                new PartyCommand(this).create()
            );
        }

        logger.info("NulvoraFriends enabled.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (redis != null) redis.close();
        if (db != null) db.close();
        if (executor != null) executor.shutdownNow();
        logger.info("NulvoraFriends disabled.");
    }

    private void loadConfig() {
        try {
            byte[] bytes = Files.readAllBytes(dataDirectory.resolve("config.json"));
            config = new Gson().fromJson(new String(bytes), NulvoraConfig.class);
        } catch (Exception e) {
            logger.warn("Could not load config (" + e.getMessage() + "), using defaults.");
            config = new NulvoraConfig();
        }
    }

    private void saveDefaultConfig() {
        try {
            Files.createDirectories(dataDirectory);
            Path configFile = dataDirectory.resolve("config.json");
            if (!Files.exists(configFile)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.json")) {
                    if (in != null) {
                        Files.write(configFile, in.readAllBytes());
                    } else {
                        String defaultConfig = new GsonBuilder().setPrettyPrinting().create().toJson(new NulvoraConfig());
                        Files.writeString(configFile, defaultConfig);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Failed to save default config", e);
        }
    }

    public ProxyServer proxy() { return proxy; }
    public Logger logger() { return logger; }
    public NulvoraConfig config() { return config; }
    public Database db() { return db; }
    public FriendService friendService() { return friendService; }
    public RedisService redis() { return redis; }
    public Optional<PartyService> partyServiceOpt() { return Optional.ofNullable(partyService); }
    public PartyService partyService() { return partyService; }
    public PresenceListener presence() { return presenceListener; }
    public ExecutorService executor() { return executor; }
}
