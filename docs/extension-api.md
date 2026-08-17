# NulvoraFriends Extension API

Guia completa para desarrolladores y agentes de IA que quieran registrar comandos de Discord desde un plugin PaperMC usando la API de NulvoraFriends.

## Indice

1. [Requisitos](#1-requisitos)
2. [Configuracion del proyecto](#2-configuracion-del-proyecto)
   - [2.0 Obtener acceso al repositorio Maven](#20-obtener-acceso-al-repositorio-maven)
   - [2.1 Configuracion de credenciales](#21-configuracion-de-credenciales-obligatorio)
   - [2.2 build.gradle.kts (Gradle)](#22-buildgradlekts-gradle-kotlin-dsl)
   - [2.3 pom.xml (Maven)](#23-pomxml-maven)
   - [2.4 plugin.yml](#24-pluginyml)
   - [2.5 Verificar la configuracion](#25-verificar-la-configuracion)
3. [Estructura de archivos](#3-estructura-de-archivos)
4. [Implementacion paso a paso](#4-implementacion-paso-a-paso)
5. [Ejemplo completo funcional](#5-ejemplo-completo-funcional)
6. [Referencia de la API](#6-referencia-de-la-api)
7. [Restricciones y limitaciones](#7-restricciones-y-limitaciones)
8. [Errores comunes](#8-errores-comunes)

---

## 1. Requisitos

- **NulvoraFriends-Paper 1.1.0+** instalado en el servidor PaperMC
- **NulvoraFriends-Velocity 1.1.0+** ejecutandose en el proxy Velocity
- **Discord habilitado** en la configuracion de Velocity (`discord.enabled: true` con token y guild-id validos)
- Java 21 o superior
- El servidor PaperMC debe estar registrado en la config de Velocity (`server-names`)

---

## 2. Configuracion del proyecto

El plugin extension es un plugin PaperMC normal. La unica dependencia especial es `nulvora-friends-papermc` como `compileOnly` (porque ya esta instalado en el servidor).

### 2.0 Obtener acceso al repositorio Maven

Los artifacts de NulvoraFriends se publican en **GitHub Packages**. Para resolverlos necesitas un **Personal Access Token (PAT)** de GitHub:

1. Ir a [https://github.com/settings/tokens](https://github.com/settings/tokens)
2. Click en **"Generate new token"** → **"Generate new token (classic)"**
3. Darle un nombre (ej: `maven-packages`)
4. Seleccionar el scope **`read:packages`** (lectura de packages)
5. Click en **"Generate token"** y copiarlo

> **IMPORTANTE**: El token solo se muestra una vez. Guardalo en un lugar seguro.

### 2.1 Configuracion de credenciales (OBLIGATORIO)

GitHub Packages requiere autenticacion. Hay dos formas de configurar las credenciales:

#### Opcion A: Variables de entorno (recomendado)

Definir en el entorno:

```bash
# Linux/Mac (en .bashrc, .zshrc o .env)
export GITHUB_ACTOR="tu-usuario-de-github"
export GITHUB_TOKEN="ghp_tu_token_aqui"
```

```powershell
# Windows (PowerShell)
$env:GITHUB_ACTOR="tu-usuario-de-github"
$env:GITHUB_TOKEN="ghp_tu_token_aqui"
```

#### Opcion B: Archivo gradle.properties en el home

Crear o editar `~/.gradle/gradle.properties`:

```properties
github.user=tu-usuario-de-github
github.token=ghp_tu_token_aqui
```

> **NUNCA** subas el token a un repositorio Git. Si usas CI/CD, usa secrets del repositorio.

### 2.2 build.gradle.kts (Gradle Kotlin DSL)

#### settings.config.kts (si se usan variables de entorno)

Si configuras las credenciales via variables de entorno (`GITHUB_ACTOR` / `GITHUB_TOKEN`), necesitas declarar el repositorio en `settings.gradle.kts`:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencyResolutionManagement {
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
}

rootProject.name = "mi-extension"
```

#### build.gradle.kts completo

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

// Solo si NO usas dependencyResolutionManagement en settings.gradle.kts:
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
    compileOnly("com.nulvora.friends:nulvora-friends-papermc:1.1.0-SNAPSHOT")
}
```

> **Nota para agentes de IA**: La dependencia de NulvoraFriends es `compileOnly` porque el JAR ya existe en el servidor. NUNCA uses `implementation` para esta dependencia.

### 2.3 pom.xml (Maven)

Si usas Maven en vez de Gradle:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.ejemplo</groupId>
    <artifactId>mi-extension</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <repositories>
        <repository>
            <id>papermc</id>
            <url>https://repo.papermc.io/repository/maven-public/</url>
        </repository>
        <repository>
            <id>github-nulvorafriends</id>
            <url>https://maven.pkg.github.com/danielmaldonadodev/nulvorafriends</url>
            <releases><enabled>true</enabled></releases>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>io.papermc.paper</groupId>
            <artifactId>paper-api</artifactId>
            <version>1.21.1-R0.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>com.nulvora.friends</groupId>
            <artifactId>nulvora-friends-papermc</artifactId>
            <version>1.1.0-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

Credenciales Maven — crear `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github-nulvorafriends</id>
      <username>${env.GITHUB_ACTOR}</username>
      <password>${env.GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
```

### 2.4 plugin.yml

```yaml
name: MiExtension
version: '1.0.0'
main: com.ejemplo.miextension.MiExtension
api-version: '1.21'
depend: [NulvoraFriends]
```

> **Importante**: Usa `depend` (no `softdepend`) para garantizar que NulvoraFriends carga primero y la API esta disponible.

### 2.5 Verificar la configuracion

Para verificar que la dependencia se resuelve correctamente:

```bash
# Gradle
./gradlew dependencies --configuration compileClasspath

# Maven
mvn dependency:tree
```

Deberias ver algo como:

```
com.nulvora.friends:nulvora-friends-papermc:1.1.0-SNAPSHOT -> compileOnly (provided)
```

---

## 3. Estructura de archivos

```
src/main/java/com/ejemplo/miextension/
  MiExtension.java          // Clase principal del plugin
  commands/
    BedWarsCommand.java      // Definicion del comando + handler
```

---

## 4. Implementacion paso a paso

### Paso 1: Clase principal del plugin

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
        // Opcional: des-registrar todos los comandos al desactivar
        // (se hace automaticamente, pero es buena practica ser explicito)
        if (registry != null) {
            registry.unregisterAll();
        }
        getLogger().info("MiExtension deshabilitada.");
    }

    private void registerDiscordCommands() {
        // Aqui se registran los comandos (ver paso 2)
    }

    public DiscordCommandRegistry getRegistry() {
        return registry;
    }
}
```

### Paso 2: Definir un comando de Discord

Crear una clase que construya el comando y su handler:

```java
package com.ejemplo.miextension.commands;

import com.nulvora.friends.paper.api.discord.*;
import java.util.concurrent.CompletableFuture;

public class BedWarsCommand {

    /**
     * Construye el spec del comando de Discord.
     * Este metodo solo crea la definicion, NO registra nada.
     */
    public static DiscordCommand buildSpec() {
        return DiscordCommand.builder("bedwars", "Comandos de BedWars")
            .ephemeralDefault(true)
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
     *
     * IMPORTANTE: El handler DEBE retornar un CompletableFuture que se complete
     * con la respuesta. Si no se completa en 8 segundos, se envia un error generico.
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

        // Aqui iria la logica real (consultar base de datos, etc.)
        // Por ahora retornamos un ejemplo

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

        // Aqui iria la logica real de paginacion

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

### Paso 3: Registrar el comando en el onEnable

Actualizar la clase principal para registrar el comando:

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

### Paso 4: Registrar multiples comandos

Si tu extension tiene varios comandos de Discord, registralos todos en `registerDiscordCommands()`:

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

---

## 5. Ejemplo completo funcional

### Archivo: `MiExtension.java`

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

        registry.register(
            BedWarsCommand.buildSpec(),
            BedWarsCommand.buildHandler()
        ).thenRun(() -> getLogger().info("Comando /bedwars registrado."))
         .exceptionally(ex -> { getLogger().severe("Error: " + ex.getMessage()); return null; });

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

### Archivo: `BedWarsCommand.java`

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

---

## 6. Referencia de la API

### Punto de entrada

```java
NulvoraFriendsApi api = NulvoraFriendsApi.get(); // Singleton
DiscordCommandRegistry registry = api.discord();
```

### Construccion de comandos

```java
// Comando raiz
DiscordCommand.builder("nombre", "descripcion")
    .ephemeralDefault(true)       // true = solo visible para quien ejecuta
    .option(...)                  // Opciones a nivel de comando raiz
    .subcommand(...)              // Subcomandos
    .build();

// Subcomando
DiscordSubcommand.builder("nombre", "descripcion")
    .option(...)                  // Opciones del subcomando
    .build();
```

### Tipos de opciones

```java
DiscordCommandOption.string("name", "desc", required)
DiscordCommandOption.integer("name", "desc", required)
DiscordCommandOption.number("name", "desc", required)
DiscordCommandOption.bool("name", "desc", required)
DiscordCommandOption.user("name", "desc", required)
```

### Leyendo opciones en el handler

```java
ctx.getString("nombre")     // → Optional<String>
ctx.getInt("page")          // → Optional<Integer>
ctx.getDouble("ratio")      // → Optional<Double>
ctx.getBoolean("verbose")   // → Optional<Boolean>
ctx.discordUserId()         // → long
ctx.discordUsername()        // → String
ctx.command()               // → String (nombre del comando raiz)
ctx.subcommand()            // → String (nombre del subcomando, o null)
ctx.options()               // → Map<String, String> (todas las opciones)
```

### Respuestas

```java
// Respuesta simple (texto + ephemeral)
DiscordCommandResponse.of("mensaje", ephemeral);

// Respuesta de error (siempre ephemeral)
DiscordCommandResponse.error("mensaje de error");

// Respuesta con builder (texto + ephemeral + embeds)
DiscordCommandResponse.builder("mensaje")
    .ephemeral(false)
    .success(true)
    .embed(DiscordEmbed.builder()
        .title("Titulo")
        .description("Descripcion")
        .color(0xFF0000)
        .field("Campo", "Valor", true)    // inline = true
        .field("Otro", "Dato", false)     // inline = false
        .footer("Pie de pagina")
        .imageUrl("https://ejemplo.com/img.png")
        .thumbnailUrl("https://ejemplo.com/thumb.png")
        .build())
    .build();
```

### Des-registro

```java
registry.unregister("bedwars");   // Un comando
registry.unregisterAll();         // Todos los de esta extension
```

> Los comandos se des-registran automaticamente al desactivar el plugin (PluginDisableEvent).

---

## 7. Restricciones y limitaciones

### Nombres de comandos

- Deben cumplir: `^[a-z0-9_-]{1,32}$`
- Solo minusculas, numeros, guiones y guiones bajos
- Maximo 32 caracteres

### Contenido

- Descripcion: 1-100 caracteres
- Maximo 25 opciones por comando/subcomando
- Maximo 25 subcomandos por comando
- Maximo 10 embeds por respuesta

### Discord

- Los comandos se registran a nivel de **guild** (no global)
- Propagacion instantanea (no hay espera de ~1 hora como con comandos globales)
- El `ephemeral` se fija en el `deferReply()`. Si la respuesta indica un valor diferente, Velocity hace `deleteOriginal()` + followup

### Transporte (plugin messaging)

- **Backend → Velocity**: requiere un jugador online como carrier. Los registros se encolan y se envian al entrar el primer jugador (`PlayerJoinEvent`)
- **Velocity → Backend**: requiere un jugador en el servidor destino. Si el servidor esta vacio, Discord recibe "servidor no disponible"
- **Discord 3s ack**: Velocity hace `deferReply()` inmediato, luego edita con la respuesta real

### Timeouts

- **PaperMC** (handler): 8 segundos por defecto (configurable en `papermc/config.yml`)
- **Velocity** (invocacion total): 10 segundos por defecto (configurable en `velocity/config.json`)

---

## 8. Errores comunes

| Error | Causa | Solucion |
|-------|-------|----------|
| `Could not resolve com.nulvora.friends:nulvora-friends-papermc` | Credenciales de GitHub Packages no configuradas o token sin scope `read:packages` | Verificar `GITHUB_ACTOR` y `GITHUB_TOKEN` en el entorno, o `~/.gradle/gradle.properties` |
| `401 Unauthorized` al resolver la dependencia | Token de GitHub expirado o invalido | Generar un nuevo PAT en github.com/settings/tokens con scope `read:packages` |
| `IllegalStateException: API no disponible` | NulvoraFriends-Paper no esta activo | Verificar plugin instalado y habilitado |
| `Nombre de comando reservado` | Colision con `vincular`/`desvincular`/`amigos` | Cambiar el nombre del comando |
| `Comando ya registrado por servidor` | Otro servidor ya registro ese nombre | Coordinar nombres entre extensiones |
| `Nombre de comando invalido` | No cumple `^[a-z0-9_-]{1,32}$` | Usar solo minusculas, numeros, guiones |
| Servidor no disponible | El backend no tiene jugadores online | Verificar que el servidor destino esta activo |
| Timeout en respuesta | El handler tardo mas de 8 segundos | Optimizar la logica del handler |
| `depend: [NulvoraFriends]` no encontrado | Nombre del plugin en plugin.yml incorrecto | Verificar el nombre exacto en el plugin.yml de NulvoraFriends |
| API retorna null | NulvoraFriends cargo despues de tu plugin | Asegurar `depend: [NulvoraFriends]` (no `softdepend`) |
| Comando no aparece en Discord | Velocity no tiene Discord habilitado o guild-id invalido | Verificar config de Velocity |

---

## Patron de agentes de IA

Cuando un agente de IA implemente una extension para NulvoraFriends, debe seguir estos pasos en orden:

### Paso 0: Configurar el repositorio Maven

1. Verificar que `GITHUB_ACTOR` y `GITHUB_TOKEN` estan definidos en el entorno, O que `~/.gradle/gradle.properties` tiene `github.user` y `github.token`
2. El token debe tener el scope `read:packages`
3. En `settings.gradle.kts` (o `build.gradle.kts` si no se usa settings), agregar el repositorio con credenciales:
   ```kotlin
   maven("https://maven.pkg.github.com/danielmaldonadodev/nulvorafriends") {
       credentials {
           username = System.getenv("GITHUB_ACTOR") ?: project.findProperty("github.user") as? String ?: ""
           password = System.getenv("GITHUB_TOKEN") ?: project.findProperty("github.token") as? String ?: ""
       }
   }
   ```

### Paso 1: Verificar build.gradle.kts

- Tiene `compileOnly("com.nulvora.friends:nulvora-friends-papermc:1.1.0-SNAPSHOT")`
- NO usa `implementation` para esta dependencia

### Paso 2: Verificar plugin.yml

- Tiene `depend: [NulvoraFriends]` (no `softdepend`)
- Tiene `main` apuntando a la clase principal correcta

### Paso 3: Implementar la clase principal

- En `onEnable()`: obtener la API con `NulvoraFriendsApi.get().discord()` dentro de try-catch
- En `onEnable()`: registrar cada comando con `registry.register(spec, handler)`
- En `onDisable()`: llamar `registry.unregisterAll()`

### Paso 4: Implementar cada comando de Discord

- Crear el spec con `DiscordCommand.builder(...)` + `.subcommand(...)` + `.option(...)` + `.build()`
- Crear el handler que retorne `CompletableFuture<DiscordCommandResponse>`
- El handler debe usar `ctx.subcommand()` para distinguir subcomandos
- El handler debe usar `ctx.getString()`/`getInt()`/etc. para leer opciones
- La respuesta debe usar `DiscordCommandResponse.of()` o `.builder()` con `.embed()`

### Reglas estrictas del handler

- El handler **NUNCA** debe bloquear el hilo principal. Usar `CompletableFuture.supplyAsync()` o `CompletableFuture.completedFuture()`
- El handler **DEBE** completar el futuro dentro de 8 segundos
- Los comandos **NO** pueden colisionar con `vincular`, `desvincular` ni `amigos`
- Los nombres de comandos deben ser minusculas, sin espacios, maximo 32 caracteres
