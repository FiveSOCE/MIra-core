package com.mira.core.service;

import com.mira.core.api.AuditService;
import com.mira.core.api.MaintenanceService;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public final class CoreMaintenanceService implements MaintenanceService {
    private final JavaPlugin plugin;
    private final AuditService audit;
    private final File stateFile;
    private YamlConfiguration state;
    private boolean enabled;
    private long startAt;
    private long endAt;

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
        tick();
    }

    @Override public boolean enabled() { return enabled; }
    @Override public String kickMessage() { return plugin.getConfig().getString("maintenance.kick-message", "&cThe server is currently under maintenance."); }
    @Override public String motd() { return plugin.getConfig().getString("maintenance.motd", "&5Mira &8- &cMaintenance"); }
    @Override public String bypassPermission() { return plugin.getConfig().getString("maintenance.bypass-permission", "miracore.maintenance.bypass"); }
    @Override public Optional<Instant> scheduledStart() { return startAt <= 0L ? Optional.empty() : Optional.of(Instant.ofEpochMilli(startAt)); }
    @Override public Optional<Instant> scheduledEnd() { return endAt <= 0L ? Optional.empty() : Optional.of(Instant.ofEpochMilli(endAt)); }

    @Override
    public void enable(String actor) {
        enabled = true;
        startAt = 0L;
        persist();
        audit("MAINTENANCE_ENABLED", actor, Map.of());
    }

    @Override
    public void disable(String actor) {
        enabled = false;
        startAt = 0L;
        endAt = 0L;
        persist();
        audit("MAINTENANCE_DISABLED", actor, Map.of());
    }

    @Override
    public void schedule(Instant start, Instant end, String actor) {
        if (start == null) throw new IllegalArgumentException("start");
        if (end != null && !end.isAfter(start)) throw new IllegalArgumentException("Maintenance end must be after start");
        startAt = start.toEpochMilli();
        endAt = end == null ? 0L : end.toEpochMilli();
        persist();
        audit("MAINTENANCE_SCHEDULED", actor, Map.of(
                "start", Long.toString(startAt),
                "end", Long.toString(endAt)));
        tick();
    }

    @Override
    public void clearSchedule(String actor) {
        startAt = 0L;
        endAt = 0L;
        persist();
        audit("MAINTENANCE_SCHEDULE_CLEARED", actor, Map.of());
    }

    public void tick() {
        long now = System.currentTimeMillis();
        if (!enabled && startAt > 0L && now >= startAt) {
            enabled = true;
            startAt = 0L;
            persist();
            audit("MAINTENANCE_ENABLED_SCHEDULED", "scheduler", Map.of());
        }
        if (enabled && endAt > 0L && now >= endAt) {
            enabled = false;
            endAt = 0L;
            startAt = 0L;
            persist();
            audit("MAINTENANCE_DISABLED_SCHEDULED", "scheduler", Map.of());
        }
    }

    private void persist() {
        state.set("enabled", enabled);
        state.set("scheduled-start", startAt);
        state.set("scheduled-end", endAt);
        try {
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
