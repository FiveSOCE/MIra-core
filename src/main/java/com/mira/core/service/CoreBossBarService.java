package com.mira.core.service;

import com.mira.core.api.BossBarService;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class CoreBossBarService implements BossBarService {
    private final Map<UUID, Map<String, BossBar>> bars = new HashMap<>();

    @Override
    public void show(Player player, String id, Component title, float progress, BossBar.Color color, BossBar.Overlay overlay) {
        if (player == null || id == null || id.isBlank()) return;
        assertPrimaryThread();
        String key = normalize(id);
        Component safeTitle = title == null ? Component.empty() : title;
        float safeProgress = Math.max(0F, Math.min(1F, progress));
        BossBar.Color safeColor = color == null ? BossBar.Color.PURPLE : color;
        BossBar.Overlay safeOverlay = overlay == null ? BossBar.Overlay.PROGRESS : overlay;

        Map<String, BossBar> playerBars = bars.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>());
        BossBar existing = playerBars.get(key);
        if (existing == null) {
            BossBar created = BossBar.bossBar(safeTitle, safeProgress, safeColor, safeOverlay);
            playerBars.put(key, created);
            player.showBossBar(created);
            return;
        }

        existing.name(safeTitle);
        existing.progress(safeProgress);
        existing.color(safeColor);
        existing.overlay(safeOverlay);
    }

    @Override
    public boolean hide(Player player, String id) {
        if (player == null || id == null || id.isBlank()) return false;
        assertPrimaryThread();
        Map<String, BossBar> playerBars = bars.get(player.getUniqueId());
        if (playerBars == null) return false;
        BossBar removed = playerBars.remove(normalize(id));
        if (removed == null) return false;
        player.hideBossBar(removed);
        if (playerBars.isEmpty()) bars.remove(player.getUniqueId());
        return true;
    }

    @Override
    public void hideAll(Player player) {
        if (player == null) return;
        assertPrimaryThread();
        Map<String, BossBar> removed = bars.remove(player.getUniqueId());
        if (removed != null) removed.values().forEach(player::hideBossBar);
    }

    @Override
    public int active(Player player) {
        if (player == null) return 0;
        Map<String, BossBar> playerBars = bars.get(player.getUniqueId());
        return playerBars == null ? 0 : playerBars.size();
    }

    public void shutdown() {
        assertPrimaryThread();
        for (UUID uuid : bars.keySet().toArray(UUID[]::new)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) hideAll(player);
            else bars.remove(uuid);
        }
    }

    private String normalize(String id) { return id.trim().toLowerCase(Locale.ROOT); }

    private void assertPrimaryThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("MiraCore boss bar mutations must run on the server thread");
    }
}
