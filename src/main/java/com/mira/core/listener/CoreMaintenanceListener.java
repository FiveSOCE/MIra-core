package com.mira.core.listener;

import com.mira.core.api.MaintenanceService;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.ServerListPingEvent;

public final class CoreMaintenanceListener implements Listener {
    private final MaintenanceService maintenance;

    public CoreMaintenanceListener(MaintenanceService maintenance) {
        this.maintenance = maintenance;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        if (!maintenance.enabled()) return;
        if (event.getPlayer().hasPermission(maintenance.bypassPermission())) return;
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, color(maintenance.kickMessage()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPing(ServerListPingEvent event) {
        if (maintenance.enabled()) event.setMotd(color(maintenance.motd()));
    }

    private static String color(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
    }
}
