# 9. Guia para agentes de IA

Guia paso a paso para que un agente de IA implemente una extension de NulvoraFriends de forma automatizada.

## Flujo general

```
1. Verificar configuracion
2. Crear estructura del proyecto
3. Configurar dependencias
4. Implementar clase principal
5. Implementar comandos
6. Compilar y verificar
```

## Paso 0: Verificar configuracion Maven

Antes de cualquier cosa, verificar que el repositorio de NulvoraFriends esta declarado:

```kotlin
maven("https://maven.elordenador.org/repository/maven-releases/")
```

Es un repositorio de lectura anonima (no requiere credenciales ni token) para las versiones
release (sin sufijo `-SNAPSHOT`).

## Paso 1: Verificar build.gradle.kts

Verificar que el archivo de build tiene:

```kotlin
dependencies {
    compileOnly("com.nulvora.friends:nulvora-friends-papermc:1.3.3")
}
```

**REGLAS ESTRICTAS**:
- Usar `compileOnly`, **NUNCA** `implementation`
- Si el usuario usa `implementation`, corregirlo inmediatamente

## Paso 2: Verificar plugin.yml

Verificar que el plugin.yml tiene:

```yaml
depend: [NulvoraFriends]
```

**REGLAS ESTRICTAS**:
- Usar `depend`, **NUNCA** `softdepend`
- Si el usuario usa `softdepend`, corregirlo inmediatamente

## Paso 3: Implementar la clase principal

Crear la clase principal con este patron exacto:

```java
package com.ejemplo.miextension;

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

        registerDiscordCommands();
        getLogger().info("Extension habilitada.");
    }

    @Override
    public void onDisable() {
        if (registry != null) {
            registry.unregisterAll();
        }
    }

    private void registerDiscordCommands() {
        // Registrar cada comando aqui
    }
}
```

## Paso 4: Implementar cada comando

Para cada comando de Discord, crear una clase con dos metodos estaticos:

### buildSpec()

Define la estructura del comando:

```java
public static DiscordCommand buildSpec() {
    return DiscordCommand.builder("nombre-comando", "Descripcion del comando")
        .ephemeralDefault(true)
        .subcommand(DiscordSubcommand.builder("sub", "Descripcion del subcomando")
            .option(DiscordCommandOption.string("opcion", "Descripcion", false))
            .build())
        .build();
}
```

### buildHandler()

Procesa la ejecucion del comando:

```java
public static DiscordCommandHandler buildHandler() {
    return ctx -> CompletableFuture.supplyAsync(() -> {
        String sub = ctx.subcommand();

        return switch (sub) {
            case "sub" -> {
                String valor = ctx.getString("opcion").orElse("default");
                yield DiscordCommandResponse.builder("Respuesta")
                    .ephemeral(true)
                    .embed(DiscordEmbed.builder()
                        .title("Titulo")
                        .description("Descripcion")
                        .color(0x57F287)
                        .field("Campo", valor, true)
                        .build())
                    .build();
            }
            default -> DiscordCommandResponse.of("Subcomando desconocido", true);
        };
    });
}
```

## Paso 5: Registrar en onEnable

En la clase principal, registrar cada comando:

```java
private void registerDiscordCommands() {
    registry.register(NombreCommand.buildSpec(), NombreCommand.buildHandler())
        .thenRun(() -> getLogger().info("Comando /nombre registrado."))
        .exceptionally(ex -> {
            getLogger().severe("Error /nombre: " + ex.getMessage());
            return null;
        });
}
```

## Reglas estrictas del handler

### 1. NUNCA bloquear el hilo principal

```java
// MAL: bloquea el hilo principal
return ctx -> {
    Thread.sleep(5000);
    return DiscordCommandResponse.of("Done", true);
};

// BIEN: usa supplyAsync
return ctx -> CompletableFuture.supplyAsync(() -> {
    return DiscordCommandResponse.of("Done", true);
});
```

### 2. DEBE completar el futuro dentro de 8 segundos

```java
// MAL: tarda mucho
return ctx -> CompletableFuture.supplyAsync(() -> {
    // Operacion lenta...
    Thread.sleep(10000);
    return DiscordCommandResponse.of("Done", true);
});

// BIEN: rapido o timeout
return ctx -> CompletableFuture.supplyAsync(() -> {
    // Operacion rapida
    return DiscordCommandResponse.of("Done", true);
});
```

### 3. Nombres de comandos validos

```
✅ bedwars
✅ bed-wars
✅ bed_wars
✅ bw-stats
✅ party
✅ party-info

❌ BedWars (mayusculas)
❌ bed wars (espacios)
❌ bedwars! (caracteres especiales)
❌ vincular (reservado)
❌ desvincular (reservado)
❌ amigos (reservado)
```

### 4. Respuestas siempre con contexto

```java
// MAL: respuesta sin contexto
DiscordCommandResponse.of("OK", true);

// BIEN: respuesta informativa
DiscordCommandResponse.builder("Operacion exitosa")
    .ephemeral(true)
    .embed(DiscordEmbed.builder()
        .title("Estado actualizado")
        .description("La arena ha sido reiniciada correctamente")
        .color(0x57F287)
        .build())
    .build();
```

## Verificacion final

Despues de implementar, verificar:

1. **Compilacion**: `./gradlew build` sin errores
2. **Dependencias**: `./gradlew dependencies` muestra la dependencia de NulvoraFriends
3. **plugin.yml**: Tiene `depend: [NulvoraFriends]`
4. **Nombres**: Todos los comandos son minusculas, sin espacios, max 32 caracteres
5. **Handler**: Todos usan `CompletableFuture.supplyAsync()` o `completedFuture()`

## Plantilla basica para una extension

### build.gradle.kts

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
    maven("https://maven.elordenador.org/repository/maven-releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("com.nulvora.friends:nulvora-friends-papermc:1.3.3")
}
```

### plugin.yml

```yaml
name: MiExtension
version: '1.0.0'
main: com.ejemplo.miextension.MiExtension
api-version: '1.21'
depend: [NulvoraFriends]
```

### Clase principal

```java
package com.ejemplo.miextension;

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
            getLogger().severe("NulvoraFriends no encontrado!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Extension habilitada.");
    }

    @Override
    public void onDisable() {
        if (registry != null) {
            registry.unregisterAll();
        }
    }

    public DiscordCommandRegistry getRegistry() {
        return registry;
    }
}
```
