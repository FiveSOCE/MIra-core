package com.mira.core.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface NotificationService {
    enum Channel { CHAT, ACTION_BAR, TITLE }
    void send(Player player, Channel channel, Component primary);
    void send(Player player, Channel channel, Component primary, Component secondary);
}
