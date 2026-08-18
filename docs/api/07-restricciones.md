# 7. Restricciones y limitaciones

Conoce los limites de la API para evitar errores.

## Nombres de comandos

### Reglas

- Solo minusculas: `a-z`
- Numeros: `0-9`
- Guiones: `-`
- Guiones bajos: `_`
- Maximo **32 caracteres**
- Regex: `^[a-z0-9_-]{1,32}$`

### Ejemplos validos

```
bedwars
bed-wars
bed_wars
bw-stats
party
party-info
```

### Ejemplos invalidos

```
BedWars      ← mayusculas
bed wars     ← espacios
bedwars!     ← caracteres especiales
bed-wars-status-muy-largo-para-ser-valido-que-tiene-mas-de-32  ← muy largo
```

## Contenido

### Limites

| Elemento | Limite |
|----------|--------|
| Descripcion de comando/subcomando | 1-100 caracteres |
| Opciones por comando/subcomando | Maximo 25 |
| Subcomandos por comando | Maximo 25 |
| Embeds por respuesta | Maximo 10 |

### Colision con comandos reservados

Estos nombres estan reservados por NulvoraFriends y **no puedes usarlos**:

- `vincular`
- `desvincular`
- `amigos`

Si intentas registrar un comando con estos nombres, recibirás un error.

## Discord

### Registro de comandos

- Los comandos se registran a nivel de **guild** (no global)
- Propagacion **instantanea** (no hay espera de ~1 hora como con comandos globales)
- Maximo 100 comandos por guild (limite de Discord)

### Comportamiento de ephemeral

El `ephemeral` se fija en el `deferReply()`. Si la respuesta indica un valor diferente, Velocity hace `deleteOriginal()` + followup con el valor correcto.

En la practica: siempre usa el mismo valor de `ephemeral` en el spec y en la respuesta.

## Transporte (plugin messaging)

### Backend → Velocity

- Requiere un **jugador online** como carrier
- Los registros se encolan y se envian al entrar el primer jugador (`PlayerJoinEvent`)
- Si no hay jugadores conectados, el registro se queda en cola

### Velocity → Backend

- Requiere un **jugador en el servidor destino**
- Si el servidor esta vacio, Discord recibe "servidor no disponible"
- La peticion se envia al servidor donde esta el jugador

### Discord 3s ack

Discord exige que el bot responda en **3 segundos**. NulvoraFriends resuelve esto asi:

1. Velocity hace `deferReply()` inmediato (responde "pensando...")
2. Tu handler procesa la peticion
3. Velocity edita el mensaje con la respuesta real

Esto significa que el usuario ve un "pensando..." brevemente antes de la respuesta.

## Timeouts

| Componente | Timeout por defecto | Configurable en |
|------------|-------------------|-----------------|
| PaperMC (handler) | 8 segundos | `papermc/config.yml` |
| Velocity (invocacion total) | 10 segundos | `velocity/config.json` |

Si tu handler tarda mas de 8 segundos, se envia un error generico al usuario de Discord.

### Configuracion de timeout

En `papermc/config.yml`:

```yaml
extensions:
  handler-timeout-ms: 8000  # 8 segundos
```

En `velocity/config.json`:

```json
{
  "extensions": {
    "command-timeout-ms": 10000
  }
}
```

## Limites de Discord

Estos son limites de la plataforma Discord, no de NulvoraFriends:

| Limite | Valor |
|--------|-------|
| Comandos por guild | 100 |
| Opciones por comando | 25 |
| Longitud de descripcion | 100 caracteres |
| Longitud de nombre de opcion | 32 caracteres |
| Embeds por mensaje | 10 |
| Caracteres en un embed | 6000 |
| Caracteres en un campo | 1024 |
| Caracteres en descripcion de embed | 4096 |

## Mejores practicas

1. **Usa nombres descriptivos pero cortos**: `bedwars-status` mejor que `bws`
2. **Documenta tus opciones**: Las descripciones aparecen en Discord
3. **Maneja errores gracefully**: Siempre retorna una respuesta, nunca lances excepciones desde el handler
4. **Manten los handlers rapidos**: Objetivo: menos de 2 segundos
5. **Usa embeds para informacion compleja**: Mejor legibilidad que texto plano
6. **Prueba con usuarios reales**: Los permisos de Discord afectan la visibilidad
