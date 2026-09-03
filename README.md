# MiraCore

MiraCore is the shared infrastructure and API layer for the Mira Paper server suite. It provides common messaging, cooldowns, service discovery, player profiles, notifications, audit logging, diagnostics and module health so individual Mira plugins do not duplicate the same foundation.

## Download

[**Download MiraCore v0.2.0**](https://github.com/FiveSOCE/MIra-core/releases/download/v0.2.0/MiraCore-0.2.0.jar)

## Requirements / Dependencies

- Paper 1.21.11
- Java 21
- No third-party dependencies

## How MiraCore Works

MiraCore registers shared services through Bukkit so other Mira plugins can retrieve them with `MiraCoreProvider.require()` instead of relying on hidden static state. Core currently provides a service registry, cooldown service, the suite-wide message/prefix service, module registration and health state, persistent player profiles, notifications, audit history, pagination helpers, permission diagnostics and persistent milestone/achievement support.

The module registry lets plugins report whether they are healthy or degraded, while the audit and diagnostics commands give administrators one place to inspect suite behaviour. MiraCore is also the source of the shared `&5&lMira &8>> &r` chat prefix for plugins that use Core messaging.

## Commands

| Command | Permission | What it does |
| --- | --- | --- |
| `/miracore status` | `miracore.admin` | Shows registered Mira modules and their current health/status. |
| `/miracore test` | `miracore.admin` | Runs MiraCore diagnostics/self-tests. |
| `/miracore why <player> <permission>` | `miracore.admin` | Debugs whether a player has a permission and why. |
| `/miracore audit [query]` | `miracore.admin` | Views/searches shared Mira audit history. |
| `/miracore profiles` | `miracore.admin` | Shows shared player-profile information/statistics. |
| `/miracore reload` | `miracore.admin` | Reloads MiraCore configuration. |
| `/miracore help` | `miracore.admin` | Shows MiraCore command help. |

Alias: `/mcore`

## Permissions

| Permission | Default | What it does |
| --- | --- | --- |
| `miracore.admin` | OP | Allows MiraCore administration, audits, diagnostics and permission debugging. |
