# 5. Referencia de la API

Referencia completa de todas las clases, metodos y constructores disponibles.

## Punto de entrada

```java
NulvoraFriendsApi api = NulvoraFriendsApi.get(); // Singleton
DiscordCommandRegistry registry = api.discord();
```

`NulvoraFriendsApi.get()` retorna la instancia singleton de la API. Si NulvoraFriends-Paper no esta activo, lanza `IllegalStateException`.

## DiscordCommandRegistry

Interfaz para gestionar comandos de Discord.

### Metodos

| Metodo | Parametros | Retorna | Descripcion |
|--------|-----------|---------|-------------|
| `register()` | `DiscordCommand spec, DiscordCommandHandler handler` | `CompletableFuture<Void>` | Registra un comando en Discord |
| `unregister()` | `String commandName` | `CompletableFuture<Void>` | Elimina un comando especifico |
| `unregisterAll()` | (ninguno) | `CompletableFuture<Void>` | Elimina todos los comandos de esta extension |

### Ejemplo de registro

```java
registry.register(spec, handler)
    .thenRun(() -> System.out.println("Registrado!"))
    .exceptionally(ex -> {
        System.out.println("Error: " + ex.getMessage());
        return null;
    });
```

### Ejemplo de des-registro

```java
// Eliminar un comando especifico
registry.unregister("bedwars");

// Eliminar todos los comandos de esta extension
registry.unregisterAll();
```

> Los comandos se des-registran automaticamente al desactivar el plugin (PluginDisableEvent).

---

## DiscordCommand

Define la estructura de un comando de Discord.

### Constructor

```java
DiscordCommand.builder(String name, String description)
    .ephemeralDefault(boolean ephemeral)
    .option(DiscordCommandOption option)
    .subcommand(DiscordSubcommand subcommand)
    .build();
```

### Parametros

| Parametro | Tipo | Requerido | Descripcion |
|-----------|------|-----------|-------------|
| `name` | `String` | Si | Nombre del comando (minusculas, max 32 chars) |
| `description` | `String` | Si | Descripcion del comando (max 100 chars) |
| `ephemeralDefault` | `boolean` | No | Si `true`, solo quien ejecuta ve la respuesta (default: `false`) |

### Ejemplos

#### Comando simple sin subcomandos

```java
DiscordCommand.builder("hello", "Saluda desde Discord")
    .ephemeralDefault(true)
    .build();
```

#### Comando con subcomandos

```java
DiscordCommand.builder("bedwars", "Comandos de BedWars")
    .ephemeralDefault(true)
    .subcommand(DiscordSubcommand.builder("status", "Ver estado")
        .build())
    .subcommand(DiscordSubcommand.builder("top", "Ver leaderboard")
        .build())
    .build();
```

#### Comando con opciones directas

```java
DiscordCommand.builder("stats", "Ver estadisticas")
    .ephemeralDefault(false)
    .option(DiscordCommandOption.user("jugador", "Jugador a consultar", true))
    .option(DiscordCommandOption.string("periodo", "Periodo de tiempo", false))
    .build();
```

---

## DiscordSubcommand

Define un subcomando dentro de un comando.

### Constructor

```java
DiscordSubcommand.builder(String name, String description)
    .option(DiscordCommandOption option)
    .build();
```

### Ejemplo

```java
DiscordSubcommand.builder("status", "Ver estado de las arenas")
    .option(DiscordCommandOption.string("arena", "Nombre de la arena", false))
    .build();
```

---

## DiscordCommandOption

Define una opcion que el usuario puede proporcionar.

### Metodos estaticos

| Metodo | Tipo de retorno | Descripcion |
|--------|----------------|-------------|
| `string(name, description, required)` | `DiscordCommandOption` | Opcion de texto |
| `integer(name, description, required)` | `DiscordCommandOption` | Opcion de numero entero |
| `number(name, description, required)` | `DiscordCommandOption` | Opcion de numero decimal |
| `bool(name, description, required)` | `DiscordCommandOption` | Opcion booleana (si/no) |
| `user(name, description, required)` | `DiscordCommandOption` | Mencion de usuario de Discord |

### Parametros comunes

| Parametro | Tipo | Descripcion |
|-----------|------|-------------|
| `name` | `String` | Nombre de la opcion (minusculas, max 32 chars) |
| `description` | `String` | Descripcion de la opcion (max 100 chars) |
| `required` | `boolean` | Si `true`, el usuario debe proporcionarla |

### Ejemplos

```java
// Texto obligatorio
DiscordCommandOption.string("nombre", "Nombre del jugador", true)

// Numero entero opcional
DiscordCommandOption.integer("page", "Numero de pagina", false)

// Decimal opcional
DiscordCommandOption.number("ratio", "Ratio de ganancia", false)

// Booleano
DiscordCommandOption.bool("verbose", "Mostrar detalles", false)

// Usuario de Discord
DiscordCommandOption.user("jugador", "Jugador a invitar", true)
```

---

## DiscordCommandContext

Proporciona informacion sobre la ejecucion del comando.

### Metodos para leer opciones

| Metodo | Retorna | Descripcion |
|--------|---------|-------------|
| `getString(name)` | `Optional<String>` | Valor de una opcion string |
| `getInt(name)` | `Optional<Integer>` | Valor de una opcion integer |
| `getDouble(name)` | `Optional<Double>` | Valor de una opcion number |
| `getBoolean(name)` | `Optional<Boolean>` | Valor de una opcion bool |

### Metodos de informacion

| Metodo | Retorna | Descripcion |
|--------|---------|-------------|
| `discordUserId()` | `long` | ID del usuario de Discord que ejecuto el comando |
| `discordUsername()` | `String` | Nombre del usuario de Discord |
| `command()` | `String` | Nombre del comando raiz |
| `subcommand()` | `String` | Nombre del subcomando (o `null` si no hay) |
| `options()` | `Map<String, String>` | Todas las opciones como mapa |

### Ejemplos

```java
// Leer una opcion string
String arena = ctx.getString("arena").orElse("todas");

// Leer un entero
int page = ctx.getInt("page").orElse(1);

// Leer un decimal
double ratio = ctx.getDouble("ratio").orElse(0.5);

// Leer un booleano
boolean verbose = ctx.getBoolean("verbose").orElse(false);

// Informacion del usuario
long userId = ctx.discordUserId();
String username = ctx.discordUsername();

// Informacion del comando
String commandName = ctx.command();      // "bedwars"
String subName = ctx.subcommand();       // "status" o null

// Todas las opciones
Map<String, String> allOptions = ctx.options();
```

---

## DiscordCommandResponse

Define la respuesta que se enviara a Discord.

### Metodos estaticos

| Metodo | Parametros | Descripcion |
|--------|-----------|-------------|
| `of()` | `String content, boolean ephemeral` | Respuesta simple con texto |
| `error()` | `String message` | Respuesta de error (siempre ephemeral) |
| `builder()` | `String content` | Builder para respuestas complejas |

### Ejemplos simples

```java
// Respuesta simple, solo para quien ejecuta
DiscordCommandResponse.of("Operacion completada!", true);

// Respuesta visible para todos
DiscordCommandResponse.of("Anuncio importante", false);

// Respuesta de error (siempre privada)
DiscordCommandResponse.of("No tienes permisos para esto", true);
```

### Builder de respuesta

```java
DiscordCommandResponse.builder("mensaje")
    .ephemeral(true)           // Solo para quien ejecuta
    .success(true)             // Marca como exito (opcional)
    .embed(DiscordEmbed.builder()  // Agregar embed
        .titulo("Titulo")
        .build())
    .build();
```

### Campos del builder

| Campo | Tipo | Descripcion |
|-------|------|-------------|
| `ephemeral` | `boolean` | Si la respuesta es privada |
| `success` | `boolean` | Si se marca como operacion exitosa |
| `embed` | `DiscordEmbed` | Embed de Discord para la respuesta |

---

## DiscordEmbed

Un embed rico de Discord con campos, colores e imagenes.

### Constructor

```java
DiscordEmbed.builder()
    .title(String title)
    .description(String description)
    .color(int color)
    .field(String name, String value, boolean inline)
    .footer(String text)
    .imageUrl(String url)
    .thumbnailUrl(String url)
    .build();
```

### Campos

| Campo | Tipo | Requerido | Descripcion |
|-------|------|-----------|-------------|
| `title` | `String` | Si | Titulo del embed |
| `description` | `String` | No | Descripcion (soporta markdown) |
| `color` | `int` | No | Color en hexadecimal (ej: `0x57F287`) |
| `field` | `String, String, boolean` | No | Campo con nombre, valor e inline |
| `footer` | `String` | No | Pie de pagina |
| `imageUrl` | `String` | No | Imagen grande |
| `thumbnailUrl` | `String` | No | Miniatura |

### Colores comunes

```java
0x57F287  // Verde (exito)
0xFFD700  // Dorado (advertencia/informacion)
0xFF0000  // Rojo (error)
0x3498DB  // Azul (informativo)
0x9B59B6  // Morado (especial)
```

### Ejemplo completo de embed

```java
DiscordEmbed.builder()
    .title("BedWars Status")
    .description("Todas las arenas estan operativas")
    .color(0x57F287)
    .field("Arena consultada", "todas", true)
    .field("Jugadores online", "24", true)
    .field("Servidores activos", "3", true)
    .footer("NulvoraFriends Extension API")
    .imageUrl("https://ejemplo.com/banner.png")
    .thumbnailUrl("https://ejemplo.com/logo.png")
    .build();
```

### Multiples embeds

Puedes agregar hasta 10 embeds en una respuesta:

```java
DiscordCommandResponse.builder("Reporte diario")
    .ephemeral(false)
    .embed(DiscordEmbed.builder()
        .title("Jugadores online")
        .description("24 jugadores conectados")
        .color(0x57F287)
        .build())
    .embed(DiscordEmbed.builder()
        .title("Servidores activos")
        .description("3 servidores funcionando")
        .color(0x3498DB)
        .build())
    .build();
```

---

## DiscordCommandHandler

Interfaz funcional que procesa las ejecuciones del comando.

### Firma

```java
@FunctionalInterface
public interface DiscordCommandHandler {
    CompletableFuture<DiscordCommandResponse> handle(DiscordCommandContext ctx);
}
```

### Reglas importantes

1. **NUNCA** bloquear el hilo principal
2. **DEBE** retornar un `CompletableFuture`
3. **DEBE** completar el futuro dentro de 8 segundos

### Ejemplo basico

```java
DiscordCommandHandler handler = ctx -> CompletableFuture.supplyAsync(() -> {
    return DiscordCommandResponse.of("Hola!", true);
});
```

### Ejemplo con logica real

```java
DiscordCommandHandler handler = ctx -> CompletableFuture.supplyAsync(() -> {
    String arena = ctx.getString("arena").orElse("todas");

    // Consultar base de datos (operacion asincrona)
    ArenaData data = database.getArena(arena);

    if (data == null) {
        return DiscordCommandResponse.of("Arena no encontrada", true);
    }

    return DiscordCommandResponse.builder("Estado de " + arena)
        .ephemeral(true)
        .embed(DiscordEmbed.builder()
            .title("Arena Status")
            .description(data.getStatus())
            .color(0x57F287)
            .field("Jugadores", String.valueOf(data.getPlayers()), true)
            .build())
        .build();
});
```

### Si no necesitas asincronia

```java
// Usar completedFuture para operaciones simples
DiscordCommandHandler handler = ctx -> CompletableFuture.completedFuture(
    DiscordCommandResponse.of("Respuesta inmediata", true)
);
```
