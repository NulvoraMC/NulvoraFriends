# 3. Implementacion paso a paso

Esta guia te muestra como crear tu extension desde cero, con codigo explicado linea por linea.

## Paso 1: Estructura de archivos

Crea la siguiente estructura en tu proyecto:

```
src/main/java/com/ejemplo/miextension/
  MiExtension.java
  commands/
    BedWarsCommand.java
```

## Paso 2: Clase principal del plugin

Crea `MiExtension.java`:

```java
package com.ejemplo.miextension;

import com.nulvora.friends.paper.api.NulvoraFriendsApi;
import com.nulvora.friends.paper.api.discord.DiscordCommandRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public class MiExtension extends JavaPlugin {

    private DiscordCommandRegistry registry;

    @Override
    public void onEnable() {
        // Obtener la API de NulvoraFriends
        // Esto lanza IllegalStateException si NulvoraFriends-Paper no esta activo
        try {
            registry = NulvoraFriendsApi.get().discord();
        } catch (IllegalStateException e) {
            getLogger().severe("NulvoraFriends-Paper no encontrado! Desactivando...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Registrar comandos de Discord
        registerDiscordCommands();

        getLogger().info("MiExtension habilitada correctamente.");
    }

    @Override
    public void onDisable() {
        // Des-registrar todos los comandos al desactivar
        // (se hace automaticamente, pero es buena practica ser explicito)
        if (registry != null) {
            registry.unregisterAll();
        }
        getLogger().info("MiExtension deshabilitada.");
    }

    private void registerDiscordCommands() {
        // Aqui se registran los comandos (ver paso 4)
    }

    public DiscordCommandRegistry getRegistry() {
        return registry;
    }
}
```

### Explicacion de cada parte

| Linea | Que hace |
|-------|----------|
| `NulvoraFriendsApi.get()` | Obtiene el singleton de la API. Lanza excepcion si no esta disponible |
| `.discord()` | Obtiene el registro de comandos de Discord |
| `registry.unregisterAll()` | Elimina todos los comandos que esta extension registro |
| `disablePlugin(this)` | Desactiva tu plugin si NulvoraFriends no esta disponible |

## Paso 3: Definir un comando de Discord

Crea `commands/BedWarsCommand.java`:

```java
package com.ejemplo.miextension.commands;

import com.nulvora.friends.paper.api.discord.*;
import java.util.concurrent.CompletableFuture;

public class BedWarsCommand {

    // Esta clase tiene dos partes: el spec (definicion) y el handler (logica)

    /**
     * Construye el spec del comando de Discord.
     * Este metodo solo crea la definicion, NO registra nada.
     */
    public static DiscordCommand buildSpec() {
        return DiscordCommand.builder("bedwars", "Comandos de BedWars")
            .ephemeralDefault(true)  // Solo visible para quien ejecuta
            .subcommand(DiscordSubcommand.builder("status", "Ver estado de las arenas")
                .option(DiscordCommandOption.string("arena", "Nombre de la arena (dejar vacio para todas)", false))
                .build())
            .subcommand(DiscordSubcommand.builder("top", "Ver top de jugadores")
                .option(DiscordCommandOption.integer("page", "Numero de pagina", false))
                .build())
            .build();
    }

    /**
     * Construye el handler que procesara las ejecuciones del comando.
     * El handler se ejecuta de forma ASINCRONA. No debe bloquear el hilo principal.
     */
    public static DiscordCommandHandler buildHandler() {
        return ctx -> CompletableFuture.supplyAsync(() -> {
            // Determinar que subcomando se ejecuto
            String subcommand = ctx.subcommand();

            if (subcommand == null) {
                return DiscordCommandResponse.of(
                    "Uso: /bedwars <status|top>", true);
            }

            return switch (subcommand) {
                case "status" -> handleStatus(ctx);
                case "top" -> handleTop(ctx);
                default -> DiscordCommandResponse.of(
                    "Subcomando desconocido: " + subcommand, true);
            };
        });
    }

    private static DiscordCommandResponse handleStatus(DiscordCommandContext ctx) {
        String arena = ctx.getString("arena").orElse("todas");

        return DiscordCommandResponse.builder("Estado de arenas")
            .ephemeral(true)
            .embed(DiscordEmbed.builder()
                .title("BedWars Status")
                .description("Todas las arenas estan operativas")
                .color(0x57F287) // Verde
                .field("Arena consultada", arena, true)
                .field("Jugadores online", "24", true)
                .field("Servidores activos", "3", true)
                .footer("NulvoraFriends Extension API")
                .build())
            .build();
    }

    private static DiscordCommandResponse handleTop(DiscordCommandContext ctx) {
        int page = ctx.getInt("page").orElse(1);

        return DiscordCommandResponse.builder("Top de jugadores - Pagina " + page)
            .ephemeral(true)
            .embed(DiscordEmbed.builder()
                .title("Leaderboard BedWars")
                .description("1. Jugador1 - 1500 puntos\n2. Jugador2 - 1200 puntos\n3. Jugador3 - 900 puntos")
                .color(0xFFD700) // Dorado
                .footer("Pagina " + page)
                .build())
            .build();
    }
}
```

### Explicacion del spec

```java
DiscordCommand.builder("bedwars", "Comandos de BedWars")
```

- `"bedwars"`: nombre del comando (minusculas, sin espacios, max 32 caracteres)
- `"Comandos de BedWars"`: descripcion que aparece en Discord

```java
.ephemeralDefault(true)
```

- `true`: solo quien ejecuta el comando ve la respuesta (privado)
- `false`: todos en el canal ven la respuesta

```java
.subcommand(DiscordSubcommand.builder("status", "Ver estado de las arenas")
    .option(DiscordCommandOption.string("arena", "Nombre de la arena", false))
    .build())
```

- Crea un subcomando `/bedwars status`
- Con una opcion opcional `arena` de tipo string

### Explicacion del handler

```java
return ctx -> CompletableFuture.supplyAsync(() -> {
```

- El handler recibe un `ctx` (contexto) con toda la informacion de la ejecucion
- Debe retornar un `CompletableFuture<DiscordCommandResponse>`
- `supplyAsync` ejecuta el codigo en un hilo separado (IMPORTANTE: no bloquear el hilo principal)

```java
String subcommand = ctx.subcommand();
```

- Obtiene el nombre del subcomando ejecutado (`"status"` o `"top"`)
- Retorna `null` si no hay subcomando

```java
String arena = ctx.getString("arena").orElse("todas");
```

- Obtiene el valor de la opcion `arena`
- Si no se proporciono, usa `"todas"` como valor por defecto

## Paso 4: Registrar el comando

Actualiza `MiExtension.java` para registrar el comando en `registerDiscordCommands()`:

```java
private void registerDiscordCommands() {
    registry.register(
        BedWarsCommand.buildSpec(),
        BedWarsCommand.buildHandler()
    ).thenRun(() -> {
        getLogger().info("Comando /bedwars registrado en Discord correctamente.");
    }).exceptionally(ex -> {
        getLogger().severe("Error al registrar /bedwars: " + ex.getMessage());
        return null;
    });
}
```

### Explicacion

- `registry.register(spec, handler)` envia el comando a Velocity para que lo registre en Discord
- Retorna un `CompletableFuture<Void>` que se completa cuando el registro termina
- `thenRun` se ejecuta si el registro fue exitoso
- `exceptionally` se ejecuta si hubo un error

## Paso 5: Registrar multiples comandos

Si tu extension tiene varios comandos, registralos todos:

```java
private void registerDiscordCommands() {
    // Comando /bedwars
    registry.register(BedWarsCommand.buildSpec(), BedWarsCommand.buildHandler())
        .thenRun(() -> getLogger().info("Comando /bedwars registrado."))
        .exceptionally(ex -> { getLogger().severe("Error /bedwars: " + ex.getMessage()); return null; });

    // Comando /build
    registry.register(BuildCommand.buildSpec(), BuildCommand.buildHandler())
        .thenRun(() -> getLogger().info("Comando /build registrado."))
        .exceptionally(ex -> { getLogger().severe("Error /build: " + ex.getMessage()); return null; });

    // Comando /economy
    registry.register(EconomyCommand.buildSpec(), EconomyCommand.buildHandler())
        .thenRun(() -> getLogger().info("Comando /economy registrado."))
        .exceptionally(ex -> { getLogger().severe("Error /economy: " + ex.getMessage()); return null; });
}
```

> Los registros se encolan y se envian cuando el primer jugador se conecta al servidor. No hay problema en registrar muchos comandos en `onEnable`.

## Siguiente paso

Ver un [ejemplo completo funcional](04-ejemplo-completo.md) o consultar la [Referencia de la API](05-referencia-api.md).
