package com.mira.core.listener;

import com.mira.core.api.BossBarService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class CoreBossBarListener implements Listener {
    private final BossBarService bossBars;

    public CoreBossBarListener(BossBarService bossBars) {
        this.bossBars = bossBars;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        bossBars.hideAll(event.getPlayer());
    }
}
