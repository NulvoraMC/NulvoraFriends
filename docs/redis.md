# Transporte y caché Redis

Desde la versión 2.0.0, NulvoraFriends no registra canales de plugin messaging. Velocity y todos los servidores Paper deben apuntar a la misma instancia, base de datos y `namespace` de Redis.

## Modelo

- Velocity mantiene MySQL/MariaDB como fuente de verdad para amistades.
- Los snapshots `friends_data` y `party_data` se guardan con TTL en claves `<namespace>:cache:<tipo>:<uuid>`.
- Velocity publica snapshots, notificaciones y aperturas del menú en `<namespace>:events`.
- Paper carga las claves al entrar un jugador y mantiene sus cachés locales mediante la suscripción de eventos.
- Paper publica solicitudes de acciones del proxy, como warp de party, en `<namespace>:requests`.

Redis es obligatorio. Si no se puede verificar la conexión durante el arranque, el plugin no habilita sus servicios.

## Configuración

La sección debe coincidir en `velocity/config.json` y `papermc/config.yml`:

```yaml
redis:
  host: localhost
  port: 6379
  username: ''
  password: ''
  database: 0
  namespace: nulfriends
```

Velocity también define `cache-ttl-seconds` (300 por defecto). Un `namespace` distinto permite aislar varias redes dentro de una misma instancia Redis.
