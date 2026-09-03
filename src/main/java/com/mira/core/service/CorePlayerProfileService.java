package com.mira.core.service;

import com.mira.core.MiraCorePlugin;
import com.mira.core.api.PlayerProfile;
import com.mira.core.api.PlayerProfileService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CorePlayerProfileService implements PlayerProfileService {
    private final MiraCorePlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;
    private final Map<UUID, PlayerProfile> cache = new ConcurrentHashMap<>();

    public CorePlayerProfileService(MiraCorePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "profiles.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
        load();
    }

    private void load() {
        ConfigurationSection root = yaml.getConfigurationSection("profiles");
        if (root == null) return;
        for (String raw : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(raw);
                String base = "profiles." + raw;
                String name = yaml.getString(base + ".name", raw);
                Instant first = Instant.ofEpochMilli(yaml.getLong(base + ".first-seen", System.currentTimeMillis()));
                Instant last = Instant.ofEpochMilli(yaml.getLong(base + ".last-seen", System.currentTimeMillis()));
                Map<String,String> metadata = new HashMap<>();
                ConfigurationSection meta = yaml.getConfigurationSection(base + ".metadata");
                if (meta != null) for (String key : meta.getKeys(false)) metadata.put(key, meta.getString(key, ""));
                cache.put(uuid, new PlayerProfile(uuid, name, first, last, false, Map.copyOf(metadata)));
            } catch (Exception ignored) { }
        }
    }

    @Override public Optional<PlayerProfile> get(UUID uuid) { return Optional.ofNullable(cache.get(uuid)); }

    @Override public Optional<PlayerProfile> get(String name) {
        if (name == null) return Optional.empty();
        return cache.values().stream().filter(p -> p.name().equalsIgnoreCase(name)).findFirst();
    }

    @Override
    public synchronized PlayerProfile touch(UUID uuid, String name, boolean online) {
        Instant now = Instant.now();
        PlayerProfile old = cache.get(uuid);
        Instant first = old == null ? now : old.firstSeen();
        Map<String,String> metadata = old == null ? Map.of() : old.metadata();
        PlayerProfile profile = new PlayerProfile(uuid, name == null ? uuid.toString() : name, first, now, online, metadata);
        cache.put(uuid, profile);
        persist(profile);
        return profile;
    }

    @Override
    public synchronized void metadata(UUID uuid, String key, String value) {
        PlayerProfile old = cache.get(uuid);
        if (old == null || key == null || key.isBlank()) return;
        Map<String,String> metadata = new HashMap<>(old.metadata());
        if (value == null) metadata.remove(key); else metadata.put(key, value);
        PlayerProfile updated = new PlayerProfile(old.uuid(), old.name(), old.firstSeen(), old.lastSeen(), old.online(), Map.copyOf(metadata));
        cache.put(uuid, updated);
        persist(updated);
    }

    @Override public Collection<PlayerProfile> cached() { return List.copyOf(cache.values()); }

    private void persist(PlayerProfile profile) {
        String base = "profiles." + profile.uuid();
        yaml.set(base + ".name", profile.name());
        yaml.set(base + ".first-seen", profile.firstSeen().toEpochMilli());
        yaml.set(base + ".last-seen", profile.lastSeen().toEpochMilli());
        yaml.set(base + ".metadata", null);
        for (Map.Entry<String,String> entry : profile.metadata().entrySet()) yaml.set(base + ".metadata." + entry.getKey(), entry.getValue());
        try { yaml.save(file); }
        catch (Exception ex) { plugin.getLogger().warning("Could not save profiles.yml: " + ex.getMessage()); }
    }
}
