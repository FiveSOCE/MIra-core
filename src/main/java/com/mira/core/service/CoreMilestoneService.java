package com.mira.core.service;

import com.mira.core.MiraCorePlugin;
import com.mira.core.api.MilestoneService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.Instant;
import java.util.*;

public final class CoreMilestoneService implements MilestoneService {
    private final MiraCorePlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public CoreMilestoneService(MiraCorePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "milestones.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public synchronized boolean award(UUID player, String key, String source, Map<String, String> metadata) {
        if (player == null || key == null || key.isBlank()) return false;
        String base = "players." + player + "." + sanitize(key);
        if (yaml.contains(base + ".time")) return false;
        yaml.set(base + ".key", key);
        yaml.set(base + ".source", source == null ? "unknown" : source);
        yaml.set(base + ".time", System.currentTimeMillis());
        if (metadata != null) for (Map.Entry<String,String> entry : metadata.entrySet()) yaml.set(base + ".metadata." + entry.getKey(), entry.getValue());
        save();
        return true;
    }

    @Override public synchronized boolean has(UUID player, String key) {
        return player != null && key != null && yaml.contains("players." + player + "." + sanitize(key) + ".time");
    }

    @Override
    public synchronized List<Milestone> all(UUID player) {
        if (player == null) return List.of();
        ConfigurationSection root = yaml.getConfigurationSection("players." + player);
        if (root == null) return List.of();
        List<Milestone> out = new ArrayList<>();
        for (String id : root.getKeys(false)) {
            String base = "players." + player + "." + id;
            String key = yaml.getString(base + ".key", id);
            String source = yaml.getString(base + ".source", "unknown");
            Instant awarded = Instant.ofEpochMilli(yaml.getLong(base + ".time", 0L));
            Map<String,String> metadata = new HashMap<>();
            ConfigurationSection meta = yaml.getConfigurationSection(base + ".metadata");
            if (meta != null) for (String mk : meta.getKeys(false)) metadata.put(mk, meta.getString(mk, ""));
            out.add(new Milestone(player, key, source, awarded, Map.copyOf(metadata)));
        }
        out.sort(Comparator.comparing(Milestone::awardedAt));
        return List.copyOf(out);
    }

    private String sanitize(String key) { return key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_"); }
    private void save() {
        try { yaml.save(file); }
        catch (Exception ex) { plugin.getLogger().warning("Could not save milestones.yml: " + ex.getMessage()); }
    }
}
