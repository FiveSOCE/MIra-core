package com.mira.core.listener;

import com.mira.core.service.CoreMotdService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerListPingEvent;

public final class CoreMotdListener implements Listener {
    private final CoreMotdService motd;

    public CoreMotdListener(CoreMotdService motd) {
        this.motd = motd;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPing(ServerListPingEvent event) {
        motd.apply(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (var line : motd.joinMessages()) player.sendMessage(line);
    }
}
