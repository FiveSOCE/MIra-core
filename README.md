# MiraCore

MiraCore is the shared infrastructure and API layer for the Mira Paper server suite. It provides common messaging, cooldowns, service discovery, player profiles, notifications, audit logging, diagnostics and module health so individual Mira plugins do not duplicate the same foundation.

## Download

[**Download MiraCore v0.4.0**](https://github.com/FiveSOCE/Mira-core/releases/download/v0.4.0/MiraCore-0.4.0.jar)

[View All Releases](https://github.com/FiveSOCE/Mira-core/releases)

## Requirements / Dependencies

- Paper 1.21.11
- Java 21
- No third-party dependencies

## How MiraCore Works

MiraCore registers shared services through Bukkit so other Mira plugins can retrieve them with `MiraCoreProvider.require()` instead of relying on hidden static state. Core currently provides a service registry, cooldown service, the suite-wide message/prefix service, module registration and health state, persistent player profiles, notifications, audit history, pagination helpers, permission diagnostics and persistent milestone/achievement support, shared BossBar presentation, maintenance authority, safe report-only release checking, and the persistent suite-wide reward queue/claim-code service.

The module registry lets plugins report whether they are healthy or degraded, while the audit and diagnostics commands give administrators one place to inspect suite behaviour. MiraCore is also the source of the shared `&5&lMira &8>> &r` chat prefix for plugins that use Core messaging.

## Commands

| Command | Permission | What it does |
| --- | --- | --- |
| `/miracore status` | `miracore.admin` | Shows registered Mira modules and their current health/status. |
| `/miracore test` | `miracore.admin` | Runs MiraCore diagnostics/self-tests. |
| `/miracore why <player> <permission>` | `miracore.admin` | Debugs whether a player has a permission and why. |
| `/miracore audit [query]` | `miracore.admin` | Views/searches shared Mira audit history. |
| `/miracore profiles` | `miracore.admin` | Shows shared player-profile information/statistics. |
| `/miracore maintenance status` | `miracore.admin` | Shows maintenance state and schedules. |
| `/miracore maintenance on\|off` | `miracore.admin` | Enables or disables maintenance mode. |
| `/miracore maintenance schedule <delay> [duration]` | `miracore.admin` | Schedules a future maintenance window using values such as `10m`, `2h` or `1d`. |
| `/miracore maintenance cancel` | `miracore.admin` | Clears the scheduled maintenance window. |
| `/miracore updates [refresh]` | `miracore.admin` | Compares installed Mira module versions with verified GitHub Releases. Never auto-downloads. |
| `/miracore reload` | `miracore.admin` | Reloads MiraCore configuration. |
| `/miracore help` | `miracore.admin` | Shows MiraCore command help. |
| `/rewards` | `miracore.rewards` | Opens the player's paginated queued-reward browser and claim preview. |
| `/claim <code>` | `miracore.rewards` | Redeems an active global claim code into the persistent reward queue. |

Alias: `/mcore`

## Permissions

| Permission | Default | What it does |
| --- | --- | --- |
| `miracore.admin` | OP | Allows MiraCore administration, audits, diagnostics and permission debugging. |
| `miracore.rewards` | Everyone | Allows viewing queued rewards and redeeming configured claim codes. |

## Shared Presentation and Operations (0.3.0)

MiraCore now exposes `BossBarService` as the suite-wide boss-bar presentation authority. Other Mira modules can create/update a player-scoped named bar without each plugin maintaining its own unrelated boss-bar implementation.

`MaintenanceService` owns persistent maintenance state and scheduled activation. Non-bypass players are denied during maintenance, while the server list MOTD and kick message are configurable.

`UpdateService` compares registered Mira module versions against configured GitHub repositories asynchronously. It is intentionally report-only: MiraCore does not download, replace or hot-swap plugin JARs.

## Shared Reward Platform (0.4.0)

MiraCore now owns the reusable `RewardService` so other Mira plugins can queue rewards without each module building its own delivery store.

### `/rewards`

- persistent per-player reward queue
- paginated reward list
- full item/command preview before claiming
- partial item delivery when only some stacks fit
- overflow remains queued under the same reward ID
- command rewards remain queued if dispatch fails
- claim actions are recorded in the Core audit log

### `/claim <code>`

Global claim codes are configured in `claim-codes.yml`. Codes are case-insensitive, can require a permission and/or expiry timestamp, and may queue item plus console-command rewards.

A code is recorded as used once its reward has been safely queued. This prevents repeated redemption while still allowing inventory overflow to be claimed later through `/rewards`.
