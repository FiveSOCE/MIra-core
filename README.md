# MiraCore

MiraCore is the shared infrastructure plugin for the Mira Minecraft plugin ecosystem. It targets **Paper 1.21.11** and **Java 21**.

MiraCore deliberately contains infrastructure rather than gameplay. It gives other Mira plugins one stable place for shared services, cooldowns, messaging, module discovery, health reporting, and diagnostics.

## What MiraCore provides

- Bukkit `ServicesManager` API discovery through `MiraCore`
- Type-safe shared `ServiceRegistry`
- Shared player `CooldownService`
- Mira plugin `ModuleRegistry`
- Module health states: `HEALTHY`, `DEGRADED`, `UNHEALTHY`, `DISABLED`
- Module registration and health-change Bukkit events
- Central configurable chat prefix and Adventure components
- `/miracore status` ecosystem health view
- `/miracore test` runtime self-test suite
- `/miracore reload` configuration reload
- Automated JUnit tests and GitHub Actions builds

## Requirements

- Paper 1.21.11
- Java 21
- No third-party plugin dependencies

## Installation

1. Download the latest MiraCore JAR.
2. Place it in the server's `plugins/` directory.
3. Restart the server.
4. Run `/miracore test` as an operator.
5. A healthy installation should report every self-test as passed.

## Commands

All commands require `miracore.admin`, which defaults to OP.

```text
/miracore status
/miracore test
/miracore reload
/miracore help
```

Alias:

```text
/mcore
```

### `/miracore status`

Shows the MiraCore version, Minecraft version, and all Mira modules currently registered with Core together with their health state.

### `/miracore test`

Runs live checks covering:

- plugin enabled state
- server-thread execution
- configuration presence
- Bukkit API service registration
- MiraCore module registration
- shared service-registry round-trip
- cooldown-service round-trip

This is the recommended in-game verification command after installing or updating MiraCore.

## Configuration

MiraCore creates `plugins/MiraCore/config.yml`.

```yaml
prefix: "&5[Mira]&r "

diagnostics:
  startup-check: true
```

The prefix is used for MiraCore chat output. `startup-check` runs a compact diagnostic pass shortly after the server enables the plugin.

## Using MiraCore from another plugin

A dependent plugin can retrieve the API from Bukkit's service manager:

```java
MiraCore core = MiraCoreProvider.require();
```

For a hard dependency, add MiraCore to the consuming plugin's `plugin.yml`:

```yaml
depend: [MiraCore]
```

### Register a Mira module

```java
core.modules().register(this, "MiraCombat");
core.modules().setHealth(this, ModuleHealth.HEALTHY, "Combat engine ready");
```

`/miracore status` will then surface the module automatically.

### Shared cooldowns

```java
core.cooldowns().start(player.getUniqueId(), "miracombat:savior", Duration.ofSeconds(30));

if (core.cooldowns().active(player.getUniqueId(), "miracombat:savior")) {
    Duration remaining = core.cooldowns().remaining(player.getUniqueId(), "miracombat:savior");
}
```

Cooldowns are runtime-only by design. They are intended for combat abilities, kits, crates, perks, and similar temporary timers rather than persistent scheduling.

### Shared services

A Mira plugin can expose an API to other Mira plugins without static singletons:

```java
core.services().register(MyPluginApi.class, myApi);
```

Another plugin can consume it with:

```java
MyPluginApi api = core.services().get(MyPluginApi.class).orElseThrow();
```

Services should be unregistered by their owner during plugin shutdown.

### Messaging

```java
core.messages().send(player, "&aYour action completed successfully.");
```

MiraCore prepends the configured prefix automatically.

## Module events

MiraCore exposes standard Bukkit events for cross-plugin coordination:

- `MiraModuleRegisteredEvent`
- `MiraModuleHealthChangeEvent`

These allow other Mira plugins or admin tooling to react when the ecosystem changes without polling.

## Design rules

- MiraCore contains no gameplay mechanics.
- Public API classes live under `com.mira.core.api`.
- Cross-plugin communication uses explicit APIs and Bukkit services rather than hidden static state.
- Core services are thread-safe where practical.
- Module-registry mutations are intentionally restricted to the primary server thread because they emit Bukkit events.
- Cooldowns are namespaced string keys so plugins can avoid collisions, for example `miracombat:savior`.
- MiraEnchantments remains standalone and does not require MiraCore.

## Building from source

```bash
gradle clean test build
```

The plugin JAR is produced at:

```text
build/libs/MiraCore-0.1.0.jar
```

GitHub Actions runs the unit tests, compiles against Paper 1.21.11, and uploads the built JAR as the `MiraCore` workflow artifact.
