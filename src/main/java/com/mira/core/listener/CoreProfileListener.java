package com.mira.core.listener;

import com.mira.core.api.PlayerProfileService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class CoreProfileListener implements Listener {
    private final PlayerProfileService profiles;

    public CoreProfileListener(PlayerProfileService profiles) { this.profiles = profiles; }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        profiles.touch(event.getPlayer().getUniqueId(), event.getPlayer().getName(), true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        profiles.touch(event.getPlayer().getUniqueId(), event.getPlayer().getName(), false);
    }
}
