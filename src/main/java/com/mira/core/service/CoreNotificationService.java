package com.mira.core.service;

import com.mira.core.api.NotificationService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

public final class CoreNotificationService implements NotificationService {
    @Override
    public void send(Player player, Channel channel, Component primary) {
        send(player, channel, primary, Component.empty());
    }

    @Override
    public void send(Player player, Channel channel, Component primary, Component secondary) {
        if (player == null || channel == null) return;
        primary = primary == null ? Component.empty() : primary;
        secondary = secondary == null ? Component.empty() : secondary;
        switch (channel) {
            case CHAT -> player.sendMessage(primary);
            case ACTION_BAR -> player.sendActionBar(primary);
            case TITLE -> player.showTitle(Title.title(primary, secondary));
        }
    }
}
