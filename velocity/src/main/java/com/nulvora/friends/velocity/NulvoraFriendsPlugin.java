package com.nulvora.friends.velocity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nulvora.friends.common.messaging.Channel;
import com.nulvora.friends.velocity.command.AmigosCommand;
import com.nulvora.friends.velocity.command.LinkCommand;
import com.nulvora.friends.velocity.command.UnlinkCommand;
import com.nulvora.friends.velocity.config.NulvoraConfig;
import com.nulvora.friends.velocity.discord.DiscordBot;
import com.nulvora.friends.velocity.friends.FriendService;
import com.nulvora.friends.velocity.messaging.PluginMessageListener;
import com.nulvora.friends.velocity.presence.PresenceListener;
import com.nulvora.friends.velocity.storage.Database;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;

@Plugin(id = "nulfriends", name = "NulvoraFriends", version = "1.0.0-SNAPSHOT",
        url = "https://github.com/nulvora/nulvorafriends",
        description = "Sistema de amigos con integracion Discord para la network Nulvora")
public class NulvoraFriendsPlugin {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private NulvoraConfig config;
    private Database db;
    private FriendService friendService;
    private DiscordBot discordBot;
    private PresenceListener presenceListener;
    private ExecutorService executor;

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

        if (config.discord().enabled()) {
            discordBot = new DiscordBot(this);
            discordBot.start();
        }

        proxy.getChannelRegistrar().register(MinecraftChannelIdentifier.from(Channel.CHANNEL_NAME));

        presenceListener = new PresenceListener(this);
        proxy.getEventManager().register(this, presenceListener);
        proxy.getEventManager().register(this, new PluginMessageListener(this));

        proxy.getCommandManager().register(
            proxy.getCommandManager().metaBuilder("nulfriends:amigos").build(),
            new AmigosCommand(this).create()
        );
        proxy.getCommandManager().register(
            proxy.getCommandManager().metaBuilder("nulfriends:vincular").build(),
            new LinkCommand(this).create()
        );
        proxy.getCommandManager().register(
            proxy.getCommandManager().metaBuilder("nulfriends:desvincular").build(),
            new UnlinkCommand(this).create()
        );

        logger.info("NulvoraFriends enabled.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (discordBot != null) discordBot.stop();
        if (db != null) db.close();
        if (executor != null) executor.shutdownNow();
        logger.info("NulvoraFriends disabled.");
    }

    private void loadConfig() {
        try {
            byte[] bytes = Files.readAllBytes(dataDirectory.resolve("config.json"));
            config = new Gson().fromJson(new String(bytes), NulvoraConfig.class);
        } catch (IOException e) {
            logger.warn("Could not load config, using defaults.");
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
    public Optional<DiscordBot> discordBot() { return Optional.ofNullable(discordBot); }
    public PresenceListener presence() { return presenceListener; }
    public ExecutorService executor() { return executor; }
}
