# AGENTS.md - NulvoraFriends

## Build commands

```bash
# Build all modules (requires JAVA_HOME pointing to Java 25)
JAVA_HOME=/home/daniel/.sdkman/candidates/java/25.0.3-tem ./gradlew build

# Build only velocity plugin
JAVA_HOME=/home/daniel/.sdkman/candidates/java/25.0.3-tem ./gradlew :velocity:build

# Build only papermc plugin
JAVA_HOME=/home/daniel/.sdkman/candidates/java/25.0.3-tem ./gradlew :papermc:build

# Clean build
JAVA_HOME=/home/daniel/.sdkman/candidates/java/25.0.3-tem ./gradlew clean build
```

## Output JARs

- `velocity/build/libs/nulvora-friends-velocity-1.0.0-SNAPSHOT.jar` — Plugin Velocity (shadow JAR with JDA, HikariCP, MariaDB driver)
- `papermc/build/libs/nulvora-friends-papermc-1.0.0-SNAPSHOT.jar` — Plugin PaperMC backend

## Project structure

- `common/` — Shared DTOs (Gson) for plugin messaging protocol proxy↔backend. Compiled at Java 21 target.
- `velocity/` — Core plugin: friend system, commands, Discord bot (JDA 6.5.0 embebido), DB (MySQL/MariaDB via HikariCP). Compiled with Java 25 toolchain.
- `papermc/` — Backend plugin: receives friend data via plugin messaging, PlaceholderAPI placeholders, inventory GUI, notifications. Compiled at Java 21 target.

## Key technical decisions

- **Velocity 4.0.0 API** (`com.velocitypowered:velocity-api:4.0.0`) — requires Java 25 to resolve
- **Gradle 9.0** with **Shadow 9.3.2** — only combination that supports Java 25 class files
- **JDA 6.5.0** (`net.dv8tion:JDA:6.5.0`) — shaded into Velocity JAR with relocated packages
- **Plugin messaging channel**: `nulfriends:main` (JSON payloads via Gson)
- **Commands use raw Mojang Brigadier API** (`LiteralArgumentBuilder<CommandSource>`) — NOT `BrigadierCommand.literal()` static methods (which don't exist in Velocity 4.x)

## Architecture

- Velocity plugin is the brain: all friend logic, DB access, Discord bot, command handling
- PaperMC plugin is a thin client: receives data via plugin messaging, exposes placeholders and GUI
- Communication: proxy→backend via plugin messages (requires player on target server — always true for per-player data push)
- Account linking: `/vincular` in MC generates 8-char code → `/vincular <code>` slash command in Discord bot

## Config files

- `velocity/src/main/resources/config.json` — Default Velocity config (MySQL, Discord token, server names, messages)
- `papermc/src/main/resources/config.yml` — PaperMC config (messages, server names, sounds)
- `papermc/src/main/resources/plugin.yml` + `paper-plugin.yml` — PaperMC plugin descriptors
- `velocity/src/main/resources/velocity-plugin.json` — Velocity plugin descriptor
