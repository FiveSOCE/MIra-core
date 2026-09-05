package com.mira.core.service;

import com.mira.core.api.AuditService;
import com.mira.core.api.MaintenanceService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public final class CoreMaintenanceService implements MaintenanceService {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private final AuditService audit;
    private final File stateFile;
    private YamlConfiguration state;
    private boolean enabled;
    private long startAt;
    private long endAt;
    private String reason = "";
    private long lastCountdownSecond = -1L;

    public CoreMaintenanceService(JavaPlugin plugin, AuditService audit) {
        this.plugin = plugin;
        this.audit = audit;
        this.stateFile = new File(plugin.getDataFolder(), "maintenance.yml");
        reload();
    }

    public void reload() {
        state = YamlConfiguration.loadConfiguration(stateFile);
        enabled = state.getBoolean("enabled", plugin.getConfig().getBoolean("maintenance.enabled", false));
        startAt = Math.max(0L, state.getLong("scheduled-start", 0L));
        endAt = Math.max(0L, state.getLong("scheduled-end", 0L));
        reason = cleanReason(state.getString("reason", plugin.getConfig().getString(
                "maintenance.default-reason", "Server maintenance")));
        lastCountdownSecond = -1L;
        tick();
    }

    @Override public boolean enabled() { return enabled; }
    @Override public String kickMessage() { return plugin.getConfig().getString(
            "maintenance.kick-message",
            "&5&lMira &8>> &cThe server is currently under maintenance. Please try again later."); }
    @Override public String motd() { return plugin.getConfig().getString("maintenance.motd", "&5Mira &8- &cMaintenance"); }
    @Override public String bypassPermission() { return plugin.getConfig().getString(
            "maintenance.bypass-permission", "miracore.maintenance.bypass"); }
    @Override public Optional<String> reason() { return reason == null || reason.isBlank() ? Optional.empty() : Optional.of(reason); }
    @Override public Optional<Instant> scheduledStart() { return startAt <= 0L ? Optional.empty() : Optional.of(Instant.ofEpochMilli(startAt)); }
    @Override public Optional<Instant> scheduledEnd() { return endAt <= 0L ? Optional.empty() : Optional.of(Instant.ofEpochMilli(endAt)); }

    @Override
    public void enable(String actor) {
        enable(actor, plugin.getConfig().getString("maintenance.default-reason", "Server maintenance"));
    }

    @Override
    public void enable(String actor, String reason) {
        this.reason = cleanReason(reason);
        activate(actor == null ? "system" : actor, false);
    }

    @Override
    public void disable(String actor) {
        boolean wasEnabled = enabled;
        enabled = false;
        startAt = 0L;
        endAt = 0L;
        reason = "";
        lastCountdownSecond = -1L;
        persist();
        audit("MAINTENANCE_DISABLED", actor, Map.of("wasEnabled", Boolean.toString(wasEnabled)));
        if (wasEnabled) {
            broadcast(plugin.getConfig().getString("maintenance.messages.disabled",
                    "&aMaintenance mode has ended. The server is open again."));
        }
    }

    @Override
    public void schedule(Instant start, Instant end, String actor) {
        schedule(start, end, actor,
                plugin.getConfig().getString("maintenance.default-reason", "Server maintenance"));
    }

    @Override
    public void schedule(Instant start, Instant end, String actor, String reason) {
        if (start == null) throw new IllegalArgumentException("start");
        if (end != null && !end.isAfter(start)) throw new IllegalArgumentException("Maintenance end must be after start");
        if (enabled) throw new IllegalStateException("Maintenance is already active");

        startAt = start.toEpochMilli();
        endAt = end == null ? 0L : end.toEpochMilli();
        this.reason = cleanReason(reason);
        lastCountdownSecond = -1L;
        persist();
        audit("MAINTENANCE_SCHEDULED", actor, Map.of(
                "start", Long.toString(startAt),
                "end", Long.toString(endAt),
                "reason", this.reason));
        tick();
    }

    @Override
    public void clearSchedule(String actor) {
        startAt = 0L;
        if (!enabled) endAt = 0L;
        lastCountdownSecond = -1L;
        if (!enabled) reason = "";
        persist();
        audit("MAINTENANCE_SCHEDULE_CLEARED", actor, Map.of());
    }

    public void tick() {
        long now = System.currentTimeMillis();

        if (!enabled && startAt > 0L) {
            if (endAt > 0L && now >= endAt) {
                startAt = 0L;
                endAt = 0L;
                reason = "";
                lastCountdownSecond = -1L;
                persist();
                audit("MAINTENANCE_WINDOW_EXPIRED", "scheduler", Map.of());
                return;
            }

            long millis = startAt - now;
            if (millis <= 0L) {
                activate("scheduler", true);
            } else {
                long seconds = Math.max(1L, (millis + 999L) / 1000L);
                if (countdowns().contains(seconds) && lastCountdownSecond != seconds) {
                    lastCountdownSecond = seconds;
                    broadcast(plugin.getConfig().getString("maintenance.messages.countdown",
                                    "&eMaintenance begins in &f%time%&e. &7%reason%")
                            .replace("%time%", format(seconds))
                            .replace("%reason%", reason));
                }
            }
        }

        if (enabled && endAt > 0L && now >= endAt) {
            enabled = false;
            endAt = 0L;
            startAt = 0L;
            reason = "";
            lastCountdownSecond = -1L;
            persist();
            audit("MAINTENANCE_DISABLED_SCHEDULED", "scheduler", Map.of());
            broadcast(plugin.getConfig().getString("maintenance.messages.disabled",
                    "&aMaintenance mode has ended. The server is open again."));
        }
    }

    private void activate(String actor, boolean scheduled) {
        if (enabled) return;

        enabled = true;
        startAt = 0L;
        lastCountdownSecond = -1L;
        persist();

        audit(scheduled ? "MAINTENANCE_ENABLED_SCHEDULED" : "MAINTENANCE_ENABLED",
                actor, Map.of("reason", reason));

        String activeMessage = plugin.getConfig().getString("maintenance.messages.active",
                "&cMaintenance mode is now active. &7%reason%")
                .replace("%reason%", reason);
        broadcast(activeMessage);

        // Intentionally kick every connected player, including bypass users.
        // Bypass controls re-entry only, matching the server maintenance workflow.
        Component kick = LEGACY.deserialize(kickMessage()
                .replace("%reason%", reason)
                .replace("%permission%", bypassPermission()));

        for (Player player : List.copyOf(Bukkit.getOnlinePlayers())) {
            player.kick(kick);
        }
    }

    private Set<Long> countdowns() {
        LinkedHashSet<Long> values = new LinkedHashSet<>();
        for (Integer value : plugin.getConfig().getIntegerList("maintenance.countdown-seconds")) {
            if (value != null && value > 0) values.add(value.longValue());
        }
        return values;
    }

    private void broadcast(String raw) {
        if (raw == null || raw.isBlank()) return;
        Bukkit.broadcast(LEGACY.deserialize(raw));
    }

    private String cleanReason(String raw) {
        String value = raw == null ? "" : raw.trim();
        return value.isBlank() ? "Server maintenance" : value;
    }

    private String format(long seconds) {
        if (seconds >= 3600L && seconds % 3600L == 0L) return (seconds / 3600L) + "h";
        if (seconds >= 60L && seconds % 60L == 0L) return (seconds / 60L) + "m";
        return seconds + "s";
    }

    private void persist() {
        state.set("enabled", enabled);
        state.set("scheduled-start", startAt);
        state.set("scheduled-end", endAt);
        state.set("reason", reason);
        try {
            plugin.getDataFolder().mkdirs();
            state.save(stateFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save maintenance.yml: " + ex.getMessage());
        }
    }

    private void audit(String action, String actor, Map<String, String> metadata) {
        audit.record("MiraCore", action, null, actor == null ? "system" : actor,
                "maintenance", "Maintenance state changed", metadata);
    }
}
