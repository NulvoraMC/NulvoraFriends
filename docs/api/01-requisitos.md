# 1. Requisitos

Antes de empezar a desarrollar tu extension, verifica que tienes todo lo necesario.

## Requisitos obligatorios

### Software del servidor

| Componente | Version minima | Donde se ejecuta |
|------------|---------------|------------------|
| **NulvoraFriends-Paper** | 1.3.3+ | Plugin PaperMC |
| **NulvoraFriends-Velocity** | 1.3.3+ | Plugin Velocity (proxy) |
| **Java** | 21 o superior | Servidor PaperMC |

### Configuracion del servidor

1. **Discord habilitado** en la configuracion de Velocity:

```json
{
  "discord": {
    "enabled": true,
    "token": "tu-token-de-discord",
    "guild-id": "id-de-tu-servidor-de-discord"
  }
}
```

2. **Tu servidor PaperMC registrado** en la config de Velocity:

```json
{
  "server-names": [
    "lobby",
    "bedwars",
    "survival"
  ]
}
```

> Si tu servidor no esta en `server-names`, los comandos no funcionaran porque Velocity no sabe a que servidor enviar la peticion.

### Herramientas de desarrollo

- **IDE**: IntelliJ IDEA, Eclipse o VS Code
- **Sistema de build**: Gradle (recomendado) o Maven
- **Java**: JDK 21+

## Requisitos opcionales pero recomendados

- Un servidor de pruebas con Velocity + PaperMC funcionando
- Un servidor de Discord donde puedas crear slash commands
- Un bot de Discord ya configurado (NulvoraFriends-Velocity lo gestiona)

## Verificacion rapida

Si ya tienes el servidor funcionando, puedes verificar que todo esta listo ejecutando estos comandos en la consola de Velocity:

```
/nulvorafriends status
```

Deberia mostrar que el bot de Discord esta conectado y la base de datos esta operativa.

## Siguiente paso

Una vez verificados los requisitos, continua con la [Configuracion del proyecto](02-configuracion-proyecto.md).
