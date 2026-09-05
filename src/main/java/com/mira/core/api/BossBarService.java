package com.mira.core.api;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface BossBarService {
    void show(Player player, String id, Component title, float progress, BossBar.Color color, BossBar.Overlay overlay);
    boolean hide(Player player, String id);
    void hideAll(Player player);
    int active(Player player);
}
