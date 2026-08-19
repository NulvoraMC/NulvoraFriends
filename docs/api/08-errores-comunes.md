# 8. Errores comunes

Tabla de errores frecuentes, sus causas y soluciones.

## Errores de configuracion

### Could not resolve com.nulvora.friends:nulvora-friends-papermc

| Causa | Solucion |
|-------|----------|
| Falta el repositorio `https://maven.elordenador.org/repository/maven-releases/` en `settings.gradle.kts` / `build.gradle.kts` | Añadirlo (lectura anonima, sin credenciales) |
| La version pedida no existe en ese repositorio | Verificar el numero de version, p.ej. `1.3.3` |

## Errores de runtime

### IllegalStateException: API no disponible

| Causa | Solucion |
|-------|----------|
| NulvoraFriends-Paper no esta activo | Verificar que el plugin esta instalado y habilitado |
| Tu plugin carga antes que NulvoraFriends | Asegurar `depend: [NulvoraFriends]` en plugin.yml |

```java
// Solucion: manejar la excepcion
try {
    registry = NulvoraFriendsApi.get().discord();
} catch (IllegalStateException e) {
    getLogger().severe("NulvoraFriends no disponible!");
    getServer().getPluginManager().disablePlugin(this);
    return;
}
```

### Nombre de comando reservado

| Causa | Solucion |
|-------|----------|
| Intentas usar `vincular`, `desvincular` o `amigos` | Cambiar el nombre del comando |

### Comando ya registrado por servidor

| Causa | Solucion |
|-------|----------|
| Otro servidor ya registro ese nombre | Coordinar nombres entre extensiones |

### Nombre de comando invalido

| Causa | Solucion |
|-------|----------|
| No cumple `^[a-z0-9_-]{1,32}$` | Usar solo minusculas, numeros, guiones |

```
Invalido: BedWars    → Valid: bedwars
Invalido: bed wars   → Valid: bed-wars
Invalido: bw!        → Valid: bw-stats
```

## Errores de transporte

### Servidor no disponible

| Causa | Solucion |
|-------|----------|
| El backend no tiene jugadores online | Verificar que el servidor destino esta activo y tiene jugadores |

### Timeout en respuesta

| Causa | Solucion |
|-------|----------|
| El handler tardo mas de 8 segundos | Optimizar la logica del handler |
| Operacion de base de datos lenta | Usar indices, cachear datos, hacer consultas asincronas |

```java
// Mal: bloquear el hilo
return ctx -> {
    // Esto bloquea todo!
    Thread.sleep(10000);
    return DiscordCommandResponse.of("Done", true);
};

// Bien: usar supplyAsync
return ctx -> CompletableFuture.supplyAsync(() -> {
    // Esto corre en un hilo separado
    return DiscordCommandResponse.of("Done", true);
});
```

## Errores de dependencia

### depend: [NulvoraFriends] no encontrado

| Causa | Solucion |
|-------|----------|
| Nombre del plugin en plugin.yml incorrecto | Verificar el nombre exacto: `NulvoraFriends` |

```yaml
# Correcto
depend: [NulvoraFriends]

# Incorrecto
depend: [nulvorafriends]
depend: [Nulvora-Friends]
depend: [NulvoraFriends-Paper]
```

### API retorna null

| Causa | Solucion |
|-------|----------|
| NulvoraFriends cargo despues de tu plugin | Asegurar `depend: [NulvoraFriends]` (no `softdepend`) |

## Errores de Discord

### Comando no aparece en Discord

Un log de "encolado para registro" en el backend **no** significa que el comando ya esta en
Discord: la confirmacion real llega despues, vía el ack de Velocity. Revisar el log del **proxy**
Velocity, no solo el del backend:

| Causa | Solucion |
|-------|----------|
| Velocity no tiene Discord habilitado | Verificar `discord.enabled: true` en config.json |
| `guild-id` en `0` o invalido | Configurar el `guild-id` real de tu servidor de Discord |
| Bot aun conectando cuando se registro | El registro queda diferido y se procesa solo al recibir el `ReadyEvent`; revisar el log del proxy poco despues del arranque |
| Bot sin el scope `applications.commands` | Reinvitar al bot con ese scope (el proxy loguea `MISSING_ACCESS` en ese caso) |
| Ningun jugador online en el backend | El registro (y su ack) no se puede transmitir hasta que entre un jugador |

### El usuario no ve el comando

| Causa | Solucion |
|-------|----------|
| Discord puede tardar en propagar | Esperar unos segundos o re-registrar el comando |
| Permisos de Discord | Verificar que el usuario tiene permisos para usar slash commands |

## Diagnostico rapido

Si algo no funciona, verifica en este orden:

1. **¿NulvoraFriends-Paper esta activo?** → `plugins list` en la consola
2. **¿Tu plugin carga?** → Buscar errores en el log al iniciar
3. **¿El repositorio Maven se resuelve?** → Ejecutar `./gradlew dependencies`
4. **¿El comando se encolo?** → Buscar "encolado para registro" en el log del **backend**
5. **¿Velocity confirmo el registro?** → Buscar "registrado en Discord correctamente" (exito) o
   "rechazado"/"Error registrando" (fallo) en el log del **proxy** — esta es la unica confirmacion real
6. **¿Discord esta habilitado?** → Verificar config de Velocity
7. **¿Hay jugadores online?** → Sin jugadores, no hay plugin messaging
