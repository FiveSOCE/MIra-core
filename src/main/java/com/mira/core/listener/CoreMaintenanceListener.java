package com.mira.core.listener;

import com.mira.core.api.MaintenanceService;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public final class CoreMaintenanceListener implements Listener {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private final MaintenanceService maintenance;

    public CoreMaintenanceListener(MaintenanceService maintenance) {
        this.maintenance = maintenance;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        if (!maintenance.enabled()) return;
        if (event.getPlayer().hasPermission(maintenance.bypassPermission())) return;

        String reason = maintenance.reason().orElse("Server maintenance");
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                LEGACY.deserialize(maintenance.kickMessage().replace("%reason%", reason)));
    }
}
