package com.mira.core.api;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public interface MessageService {
    Component prefix();

    Component parse(String legacyText);

    void send(CommandSender recipient, Component message);

    default void send(CommandSender recipient, String legacyText) {
        send(recipient, parse(legacyText));
    }
}
