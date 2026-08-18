# NulvoraFriends Extension API

Guia completa para desarrolladores y agentes de IA que quieran registrar comandos de Discord desde un plugin PaperMC usando la API de NulvoraFriends.

## Que es la Extension API?

La Extension API te permite **crear comandos de Discord** que se ejecutan desde tu plugin PaperMC. Los jugadores usan `/tucomando` en Discord y la respuesta viene de tu codigo en el servidor Minecraft.

### Arquitectura

```
Discord (slash command) → Velocity → tu plugin PaperMC → respuesta → Discord
```

El flujo completo es:

1. Un jugador escribe `/bedwars status` en Discord
2. Velocity recibe la interaccion y la reenvia a tu plugin via plugin messaging
3. Tu plugin procesa la peticion y genera una respuesta
4. La respuesta viaja de vuelta a Velocity y se muestra en Discord

## Documentacion

| Capitulo | Descripcion |
|----------|-------------|
| [1. Requisitos](01-requisitos.md) | Que necesitas antes de empezar |
| [2. Configuracion del proyecto](02-configuracion-proyecto.md) | Dependencias, credenciales, build files |
| [3. Implementacion paso a paso](03-implementacion-paso-a-paso.md) | Guia practica con codigo |
| [4. Ejemplo completo funcional](04-ejemplo-completo.md) | Plugin completo listo para usar |
| [5. Referencia de la API](05-referencia-api.md) | Todas las clases, metodos y constructores |
| [6. API de Party](06-party-api.md) | Consultar informacion de partys |
| [7. Restricciones y limitaciones](07-restricciones.md) | Limites de Discord, timeouts, nombres |
| [8. Errores comunes](08-errores-comunes.md) | Soluciones a problemas frecuentes |
| [9. Guia para agentes de IA](09-guia-agentes-ia.md) | Pasos automatizados para generar extensiones |

## Resumen rapido

```java
// 1. Obtener la API
DiscordCommandRegistry registry = NulvoraFriendsApi.get().discord();

// 2. Definir el comando
DiscordCommand spec = DiscordCommand.builder("mi-comando", "Descripcion")
    .ephemeralDefault(true)
    .subcommand(DiscordSubcommand.builder("info", "Muestra info")
        .build())
    .build();

// 3. Definir que hacer
DiscordCommandHandler handler = ctx -> CompletableFuture.supplyAsync(() -> {
    return DiscordCommandResponse.of("Hola desde PaperMC!", true);
});

// 4. Registrar
registry.register(spec, handler);
```

## Ejemplo real: comando `/bedwars`

Imagina que tienes un plugin de BedWars. Quieres que los jugadores puedan ver el estado de las arenas desde Discord:

```
Usuario en Discord: /bedwars status
Bot en Discord: 
  ┌─────────────────────┐
  │  BedWars Status      │
  │                      │
  │  Arenas operativas   │
  │  Arena: todas        │
  │  Jugadores: 24       │
  │  Servidores: 3       │
  └─────────────────────┘
```

Eso es exactamente lo que la Extension API te permite crear.
