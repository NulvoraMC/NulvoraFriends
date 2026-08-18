# 8. Errores comunes

Tabla de errores frecuentes, sus causas y soluciones.

## Errores de configuracion

### Could not resolve com.nulvora.friends:nulvora-friends-papermc

| Causa | Solucion |
|-------|----------|
| Credenciales de GitHub Packages no configuradas | Verificar `GITHUB_ACTOR` y `GITHUB_TOKEN` en el entorno |
| Token sin scope `read:packages` | Generar un nuevo PAT en github.com/settings/tokens con el scope correcto |
| Repositorio Maven mal configurado | Verificar la URL y las credenciales en `settings.gradle.kts` |

### 401 Unauthorized al resolver la dependencia

| Causa | Solucion |
|-------|----------|
| Token de GitHub expirado o invalido | Generar un nuevo PAT en github.com/settings/tokens |
| Token sin permisos | Verificar que el token tiene scope `read:packages` |

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

| Causa | Solucion |
|-------|----------|
| Velocity no tiene Discord habilitado | Verificar `discord.enabled: true` en config.json |
| Guild ID invalido | Verificar que el `guild-id` coincide con tu servidor de Discord |
| Bot no tiene permisos | Verificar que el bot tiene permisos de slash commands en el servidor |

### El usuario no ve el comando

| Causa | Solucion |
|-------|----------|
| Discord puede tardar en propagar | Esperar unos segundos o re-registrar el comando |
| Permisos de Discord | Verificar que el usuario tiene permisos para usar slash commands |

## Diagnostico rapido

Si algo no funciona, verifica en este orden:

1. **¿NulvoraFriends-Paper esta activo?** → `plugins list` en la consola
2. **¿Tu plugin carga?** → Buscar errores en el log al iniciar
3. **¿Las credenciales estan bien?** → Ejecutar `./gradlew dependencies`
4. **¿El comando se registro?** → Buscar "Comando registrado" en el log
5. **¿Discord esta habilitado?** → Verificar config de Velocity
6. **¿Hay jugadores online?** → Sin jugadores, no hay plugin messaging
