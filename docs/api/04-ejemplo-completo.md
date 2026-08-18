# 4. Ejemplo completo funcional

Un plugin completo con un comando `/bedwars` que tiene dos subcomandos: `status` y `top`.

## Archivo: MiExtension.java

```java
package com.ejemplo.miextension;

import com.ejemplo.miextension.commands.BedWarsCommand;
import com.nulvora.friends.paper.api.NulvoraFriendsApi;
import com.nulvora.friends.paper.api.discord.DiscordCommandRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public class MiExtension extends JavaPlugin {

    private DiscordCommandRegistry registry;

    @Override
    public void onEnable() {
        try {
            registry = NulvoraFriendsApi.get().discord();
        } catch (IllegalStateException e) {
            getLogger().severe("NulvoraFriends-Paper no encontrado! Desactivando...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Registrar comando /bedwars
        registry.register(
            BedWarsCommand.buildSpec(),
            BedWarsCommand.buildHandler()
        ).thenRun(() -> getLogger().info("Comando /bedwars registrado."))
         .exceptionally(ex -> {
             getLogger().severe("Error al registrar /bedwars: " + ex.getMessage());
             return null;
         });

        getLogger().info("MiExtension habilitada.");
    }

    @Override
    public void onDisable() {
        if (registry != null) {
            registry.unregisterAll();
        }
    }
}
```

## Archivo: BedWarsCommand.java

```java
package com.ejemplo.miextension.commands;

import com.nulvora.friends.paper.api.discord.*;
import java.util.concurrent.CompletableFuture;

public class BedWarsCommand {

    public static DiscordCommand buildSpec() {
        return DiscordCommand.builder("bedwars", "Comandos de BedWars")
            .ephemeralDefault(true)
            .subcommand(DiscordSubcommand.builder("status", "Ver estado de las arenas")
                .option(DiscordCommandOption.string("arena", "Nombre de la arena", false))
                .build())
            .subcommand(DiscordSubcommand.builder("top", "Ver top de jugadores")
                .option(DiscordCommandOption.integer("page", "Pagina", false))
                .build())
            .build();
    }

    public static DiscordCommandHandler buildHandler() {
        return ctx -> CompletableFuture.supplyAsync(() -> {
            String sub = ctx.subcommand();
            if (sub == null) {
                return DiscordCommandResponse.of("Uso: /bedwars <status|top>", true);
            }
            return switch (sub) {
                case "status" -> {
                    String arena = ctx.getString("arena").orElse("todas");
                    yield DiscordCommandResponse.builder("Estado")
                        .ephemeral(true)
                        .embed(DiscordEmbed.builder()
                            .title("BedWars Status")
                            .description("Arenas operativas")
                            .color(0x57F287)
                            .field("Arena", arena, true)
                            .build())
                        .build();
                }
                case "top" -> {
                    int page = ctx.getInt("page").orElse(1);
                    yield DiscordCommandResponse.builder("Top - Pagina " + page)
                        .ephemeral(true)
                        .embed(DiscordEmbed.builder()
                            .title("Leaderboard")
                            .description("1. Jugador1 - 1500\n2. Jugador2 - 1200")
                            .color(0xFFD700)
                            .build())
                        .build();
                }
                default -> DiscordCommandResponse.of("Subcomando desconocido.", true);
            };
        });
    }
}
```

## plugin.yml

```yaml
name: MiExtension
version: '1.0.0'
main: com.ejemplo.miextension.MiExtension
api-version: '1.21'
depend: [NulvoraFriends]
```

## build.gradle.kts

```kotlin
plugins {
    java
}

group = "com.ejemplo"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.pkg.github.com/danielmaldonadodev/nulvorafriends") {
        credentials {
            username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("github.user") as? String ?: ""
            password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("github.token") as? String ?: ""
        }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("com.nulvora.friends:nulvora-friends-papermc:1.2.0-SNAPSHOT")
}
```

## Como funciona

```
1. Jugador escribe en Discord: /bedwars status
2. Velocity recibe la interaccion
3. Velocity envia via plugin messaging a tu servidor PaperMC
4. Tu handler ejecuta handleStatus()
5. Se genera un embed con el estado de las arenas
6. La respuesta viaja de vuelta a Velocity
7. Velocity la muestra en Discord
```

## Ejemplo con mas opciones

### Comando con opcion de tipo user

```java
public static DiscordCommand buildSpec() {
    return DiscordCommand.builder("party", "Gestion de partys")
        .ephemeralDefault(true)
        .subcommand(DiscordSubcommand.builder("invite", "Invitar a un jugador")
            .option(DiscordCommandOption.user("jugador", "Jugador a invitar", true))
            .build())
        .build();
}
```

### Handler que lee el usuario mencionado

```java
case "invite" -> {
    // ctx.getString("jugador") retorna el ID del usuario como string
    String userId = ctx.getString("jugador").orElse("");
    String username = ctx.discordUsername();

    yield DiscordCommandResponse.builder("Invitacion enviada")
        .ephemeral(true)
        .embed(DiscordEmbed.builder()
            .title("Party Invite")
            .description(username + " invito a <@" + userId + "> a la party")
            .color(0x57F287)
            .build())
        .build();
}
```

### Comando sin subcomandos (solo opciones)

```java
public static DiscordCommand buildSpec() {
    return DiscordCommand.builder("stats", "Ver estadisticas de un jugador")
        .ephemeralDefault(true)
        .option(DiscordCommandOption.user("jugador", "Jugador a consultar", false))
        .option(DiscordCommandOption.string("periodo", "Periodo de tiempo", false))
        .build();
}

public static DiscordCommandHandler buildHandler() {
    return ctx -> CompletableFuture.supplyAsync(() -> {
        String userId = ctx.getString("jugador")
            .map(String::valueOf)
            .orElse("todos");
        String periodo = ctx.getString("periodo").orElse("hoy");

        return DiscordCommandResponse.builder("Estadisticas")
            .ephemeral(true)
            .embed(DiscordEmbed.builder()
                .title("Stats de " + userId)
                .description("Periodo: " + periodo)
                .color(0x3498DB)
                .build())
            .build();
    });
}
```
