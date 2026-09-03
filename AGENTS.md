# AGENTS.md - NulvoraFriends

## Versionado

Semver `X.Y.Z`, sin sufijo `-SNAPSHOT` en releases. Bugfix: tercer número; característica pequeña: segundo; ruptura: primero. Al cambiar versión deben coincidir:

- `build.gradle.kts`
- `papermc/src/main/resources/plugin.yml`
- `velocity/src/main/resources/velocity-plugin.json`
- `velocity/src/main/java/com/nulvora/friends/velocity/NulvoraFriendsPlugin.java`

## Build

```bash
JAVA_HOME=/home/daniel/.sdkman/candidates/java/25.0.3-tem ./gradlew build
JAVA_HOME=/home/daniel/.sdkman/candidates/java/25.0.3-tem ./gradlew :velocity:build
JAVA_HOME=/home/daniel/.sdkman/candidates/java/25.0.3-tem ./gradlew :papermc:build
JAVA_HOME=/home/daniel/.sdkman/candidates/java/25.0.3-tem ./gradlew clean build
```

Los JAR son `velocity/build/libs/nulvora-friends-velocity-<version>.jar` y `papermc/build/libs/nulvora-friends-papermc-<version>.jar`.

## Arquitectura

- `common/`: DTOs Gson y protocolo Redis compartido; target Java 21.
- `velocity/`: lógica de amigos y parties, comandos, MySQL/MariaDB y publicación Redis; toolchain Java 25.
- `papermc/`: caché local alimentada por Redis, GUI, placeholders y API pública de parties; target Java 21.
- Redis sustituye totalmente a Bungee/Velocity plugin messaging. No se deben registrar canales ni usar jugadores como carriers.
- MySQL/MariaDB sigue siendo la fuente de verdad de amistades. Redis contiene snapshots con TTL y distribuye eventos/solicitudes por pub/sub.
- Canales: `<namespace>:events` (Velocity→Paper) y `<namespace>:requests` (Paper→Velocity).
- Claves: `<namespace>:cache:<tipo>:<uuid>`.
- Redis es obligatorio y su host, puerto, credenciales, database y namespace deben coincidir en ambos plugins.
- El sistema de Discord, la vinculación de cuentas y la API de comandos Discord fueron eliminados en 2.0.0.
- `PartyService` sigue manteniendo parties en memoria en Velocity; publica snapshots a Redis para Paper.
- Los comandos Velocity usan Brigadier Mojang directamente.

Consulta `docs/redis.md` para el protocolo y la configuración.
