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

- `velocity/build/libs/nulvora-friends-velocity-1.1.0-SNAPSHOT.jar` — Plugin Velocity (shadow JAR with JDA, HikariCP, MariaDB driver)
- `papermc/build/libs/nulvora-friends-papermc-1.1.0-SNAPSHOT.jar` — Plugin PaperMC backend (shadow JAR, includes common classes)

## Project structure

- `common/` — Shared DTOs (Gson) for plugin messaging protocol proxy↔backend. Compiled at Java 21 target. Contains DTOs for the extension system (command specs, invocations, responses, embeds).
- `velocity/` — Core plugin: friend system, commands, Discord bot (JDA 6.5.0 embebido), DB (MySQL/MariaDB via HikariCP), **ExtensionCommandRegistry** (dynamic slash command routing). Compiled with Java 25 toolchain.
- `papermc/` — Backend plugin: receives friend data via plugin messaging, PlaceholderAPI placeholders, inventory GUI, notifications, **public API for extensions** (`com.nulvora.friends.paper.api`), **DiscordCommandManager** (command registration, handler execution with timeout). Compiled at Java 21 target.

## Key technical decisions

- **Velocity 4.0.0 API** (`com.velocitypowered:velocity-api:4.0.0`) — requires Java 25 to resolve
- **Gradle 9.0** with **Shadow 9.3.2** — only combination that supports Java 25 class files
- **JDA 6.5.0** (`net.dv8tion:JDA:6.5.0`) — shaded into Velocity JAR with relocated packages
- **Plugin messaging channel**: `nulfriends:main` (JSON payloads via Gson)
- **Commands use raw Mojang Brigadier API** (`LiteralArgumentBuilder<CommandSource>`) — NOT `BrigadierCommand.literal()` static methods (which don't exist in Velocity 4.x)
- **Shadow applied to papermc** (no relocations) to bundle `common` classes into the JAR for runtime use
- **Extension commands registered as guild-scoped** for instant Discord propagation (not global, which takes up to 1h)

## Architecture

- Velocity plugin is the brain: all friend logic, DB access, Discord bot, command handling
- PaperMC plugin is a thin client: receives data via plugin messaging, exposes placeholders and GUI
- Communication: proxy→backend via plugin messages (requires player on target server — always true for per-player data push)
- Account linking: `/vincular` in MC generates 8-char code → `/vincular <code>` slash command in Discord bot

## Extension system (v1.1.0)

Extension plugins register Discord slash commands via the PaperMC API. Velocity routes these commands and manages Discord interaction.

### Flow

```
Extension → PaperMC API (register) → plugin messaging → Velocity (upsert to JDA)
Discord interaction → Velocity → plugin messaging → PaperMC → handler → response → Velocity → Discord
```

### Transport constraints

- **Backend→Velocity**: requires an online player as carrier. Registrations are queued and flushed on `PlayerJoinEvent`.
- **Velocity→Backend**: requires a player on the target server. If server is empty, Discord gets "server unavailable" error.
- **Discord 3s ack limit**: Velocity does `deferReply()` immediately, then `editOriginal()` when the response arrives.
- **Timeout**: Paper enforces 8s timeout on handler execution; Velocity has 10s global timeout per invocation.

### Message types (common/dto/extension/)

| Message | Direction | Purpose |
|---------|-----------|---------|
| `ext_cmd_register` | Backend→Proxy | Register a new slash command |
| `ext_cmd_register_ack` | Proxy→Backend | Ack registration (success/error) |
| `ext_cmd_unregister` | Backend→Proxy | Unregister commands |
| `ext_cmd_invoke` | Proxy→Backend | User executed a command on Discord |
| `ext_cmd_response` | Backend→Proxy | Handler response to send to Discord |

### Key classes

**PaperMC API** (`com.nulvora.friends.paper.api`):
- `NulvoraFriendsApi` — singleton entry point: `NulvoraFriendsApi.get().discord()`
- `DiscordCommandRegistry` — interface: `register()`, `unregister()`, `unregisterAll()`
- `DiscordCommand`, `DiscordSubcommand`, `DiscordCommandOption` — command spec builders
- `DiscordCommandHandler` — `@FunctionalInterface CompletionStage<DiscordCommandResponse> handle(DiscordCommandContext)`
- `DiscordCommandContext` — options, user info, command path
- `DiscordCommandResponse`, `DiscordEmbed` — response builders (text + ephemeral + embeds)

**PaperMC internal** (`com.nulvora.friends.paper.extension`):
- `DiscordCommandManager` — validates specs, stores command→handler, queues registrations, executes handlers with timeout, auto-unregisters on plugin disable

**Velocity** (`com.nulvora.friends.velocity.extension`):
- `ExtensionCommandRegistry` — maps command→server, upserts/deletes JDA commands (guild-scope), manages pending invocations with timeout, selects carrier player

### Config (velocity config.json)

```json
"extensions": {
  "enabled": true,
  "command-timeout-ms": 10000
}
```

### Registration collisions

First-come-first-served. Duplicate names (including built-in `vincular`/`desvincular`/`amigos`) are rejected with an error ack. The extension's `register()` future completes exceptionally with the error.

### Lifecycle

- Extension enables → calls `register()` → queued → flushed on first player join → ack received
- Extension disables → `PluginDisableEvent` → auto-unregister all its commands
- Proxy restarts → Velocity clears all dynamic commands from Discord → backends re-register on next player join

## Config files

- `velocity/src/main/resources/config.json` — Default Velocity config (MySQL, Discord token, server names, messages, extensions)
- `papermc/src/main/resources/config.yml` — PaperMC config (messages, server names, sounds)
- `papermc/src/main/resources/plugin.yml` + `paper-plugin.yml` — PaperMC plugin descriptors
- `velocity/src/main/resources/velocity-plugin.json` — Velocity plugin descriptor
