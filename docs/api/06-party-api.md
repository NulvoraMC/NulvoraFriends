# 6. API de Party (v1.2.0)

Desde v1.2.0, las extensiones pueden consultar informacion de parties de jugadores a traves de la API de NulvoraFriends.

## Que es una party?

Una party es un grupo temporal de jugadores que juegan juntos. Un jugador es el lider y los demas son miembros. Las parties se crean, disuelven y modifican en tiempo real.

## Acceso a la API

```java
import com.nulvora.friends.paper.api.NulvoraFriendsApi;
import com.nulvora.friends.paper.api.party.*;

PartyApi party = NulvoraFriendsApi.get().party();
```

## Metodos disponibles

### isInParty(UUID playerUuid)

Verifica si un jugador esta en una party.

```java
UUID playerUuid = player.getUniqueId();

boolean inParty = party.isInParty(playerUuid);

if (inParty) {
    System.out.println("El jugador esta en una party");
} else {
    System.out.println("El jugador no esta en ninguna party");
}
```

### getParty(UUID playerUuid)

Obtiene un snapshot completo de la party del jugador.

```java
Optional<PartySnapshot> snapshot = party.getParty(playerUuid);

snapshot.ifPresent(s -> {
    UUID partyId = s.partyId();       // ID unico de la party
    UUID leader = s.leader();         // UUID del lider
    List<PartyMember> members = s.members();  // Lista de miembros

    System.out.println("Party ID: " + partyId);
    System.out.println("Lider: " + leader);
    System.out.println("Miembros: " + members.size());
});

if (snapshot.isEmpty()) {
    System.out.println("El jugador no esta en ninguna party");
}
```

### getMembers(UUID playerUuid)

Obtiene solo la lista de miembros de la party.

```java
List<PartyMember> members = party.getMembers(playerUuid);

for (PartyMember member : members) {
    System.out.println(member.name() + " - " +
        (member.online() ? "Online" : "Offline") +
        (member.server() != null ? " en " + member.server() : ""));
}
```

### isLeader(UUID playerUuid)

Verifica si un jugador es el lider de su party.

```java
boolean isLeader = party.isLeader(playerUuid);

if (isLeader) {
    System.out.println("El jugador es el lider de su party");
} else {
    System.out.println("El jugador no es lider");
}
```

## Records

### PartySnapshot

Snapshot de una party completa.

```java
public record PartySnapshot(
    UUID partyId,              // ID unico de la party
    UUID leader,               // UUID del jugador lider
    List<PartyMember> members  // Lista de todos los miembros
) {}
```

### PartyMember

Informacion de un miembro de la party.

```java
public record PartyMember(
    UUID uuid,      // UUID del jugador
    String name,    // Nombre del jugador
    boolean online, // Si esta conectado
    String server   // Servidor donde esta (nullable si offline)
) {}
```

## Ejemplo completo: comando `/party-info` en Discord

```java
package com.ejemplo.miextension.commands;

import com.nulvora.friends.paper.api.NulvoraFriendsApi;
import com.nulvora.friends.paper.api.discord.*;
import com.nulvora.friends.paper.api.party.*;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.util.List;

public class PartyInfoCommand {

    public static DiscordCommand buildSpec() {
        return DiscordCommand.builder("party-info", "Informacion de tu party")
            .ephemeralDefault(true)
            .option(DiscordCommandOption.user("jugador", "Jugador a consultar", false))
            .build();
    }

    public static DiscordCommandHandler buildHandler() {
        return ctx -> CompletableFuture.supplyAsync(() -> {
            PartyApi partyApi = NulvoraFriendsApi.get().party();

            // Obtener el UUID del jugador
            // En un caso real, necesitarias resolver el UUID desde el ID de Discord
            // o usar el jugador que ejecuta el comando
            String userId = ctx.getString("jugador")
                .map(String::valueOf)
                .orElse(null);

            if (userId == null) {
                return DiscordCommandResponse.of("Especifica un jugador", true);
            }

            // Aqui necesitarias convertir el ID de Discord a UUID de Minecraft
            // Esto depende de tu sistema de vinculacion de cuentas
            UUID playerUuid = resolvePlayerUuid(userId);

            Optional<PartySnapshot> snapshot = partyApi.getParty(playerUuid);

            if (snapshot.isEmpty()) {
                return DiscordCommandResponse.builder("Sin party")
                    .ephemeral(true)
                    .embed(DiscordEmbed.builder()
                        .title("Party Info")
                        .description("El jugador no esta en ninguna party")
                        .color(0xFF0000)
                        .build())
                    .build();
            }

            PartySnapshot s = snapshot.get();
            StringBuilder membersList = new StringBuilder();

            for (PartyMember member : s.members()) {
                String status = member.online() ? "Online" : "Offline";
                String server = member.server() != null ? " (" + member.server() + ")" : "";
                String leader = member.uuid().equals(s.leader()) ? " [LIDER]" : "";
                membersList.append("- ").append(member.name()).append(leader)
                    .append(" - ").append(status).append(server).append("\n");
            }

            return DiscordCommandResponse.builder("Party de " + s.leader())
                .ephemeral(true)
                .embed(DiscordEmbed.builder()
                    .title("Party Info")
                    .description(membersList.toString())
                    .color(0x57F287)
                    .field("Party ID", s.partyId().toString(), false)
                    .field("Total miembros", String.valueOf(s.members().size()), true)
                    .build())
                .build();
        });
    }

    private static UUID resolvePlayerUuid(String input) {
        // Implementar segun tu sistema de vinculacion
        // Ejemplo: consultar base de datos
        return UUID.fromString(input);
    }
}
```

## Limitaciones importantes

1. **Solo jugadores conectados**: Los datos provienen de un cache actualizado por Velocity via plugin messaging. Solo hay datos de jugadores **conectados a este servidor backend**.

2. **Cache actualizado**: Los datos se actualizan cuando la party cambia (join/leave/kick/disband/follow). No son datos en tiempo real.

3. **No hay datos historicos**: No puedes consultar parties pasadas o de jugadores offline en otros servidores.

4. **Una party por jugador**: Un jugador solo puede estar en una party a la vez.

5. **Lider automatico**: Si el lider se desconecta, el miembro mas antiguo hereda el liderazgo.

## Casos de uso comunes

- **Comando `/party` en Discord**: Ver tu party actual desde Discord
- **Sistema de estadisticas**: Registrar partidas por party
- **Notificaciones**: Avisar cuando un amigo entra a una party
- **Moderacion**: Ver que jugadores estan agrupados
