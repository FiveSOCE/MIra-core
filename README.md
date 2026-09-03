# MiraCore

Shared infrastructure for the Mira Minecraft plugin ecosystem, targeting **Paper 1.21.11 / Java 21**.

## Download

Current release: **v0.2.0**

[**Download MiraCore v0.2.0**](https://github.com/FiveSOCE/MIra-core/releases/download/v0.2.0/MiraCore-0.2.0.jar)

[View all releases](https://github.com/FiveSOCE/MIra-core/releases)

## v0.2.0

MiraCore is the common service layer used by Mira plugins. v0.2.0 expands the existing service registry, cooldown, messaging, module health and diagnostics systems with:

- persistent shared player profiles
- central notification service
- global Mira audit logging
- shared pagination helpers
- permission debugging
- persistent milestone / achievement service
- expanded ecosystem health/status tooling

## Commands

```text
/miracore status
/miracore test
/miracore why <player> <permission>
/miracore audit [query]
/miracore profiles
/miracore reload
/miracore help
```

Alias: `/mcore`

All administration commands require `miracore.admin`, default OP.

## Shared APIs

Other Mira plugins retrieve Core through Bukkit services:

```java
MiraCore core = MiraCoreProvider.require();
```

Core provides shared services for:

- `ServiceRegistry`
- `CooldownService`
- `MessageService`
- `ModuleRegistry`
- player profiles
- notifications
- audit events/history
- pagination
- permission diagnostics
- milestones

Plugins should communicate through explicit APIs and Bukkit services rather than hidden static state.

## Requirements

- Paper 1.21.11
- Java 21
- No third-party dependencies

## Building

```bash
gradle clean test build
```

Output:

```text
build/libs/MiraCore-0.2.0.jar
```
