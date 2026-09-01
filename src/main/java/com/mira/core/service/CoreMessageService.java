package com.mira.core.service;

import com.mira.core.api.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class CoreMessageService implements MessageService {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private volatile Component prefix = Component.empty();

    public CoreMessageService(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        reload();
    }

    public void reload() {
        String raw = plugin.getConfig().getString("prefix", "&5[Mira]&r ");
        prefix = LEGACY.deserialize(raw == null ? "&5[Mira]&r " : raw);
    }

    @Override
    public Component prefix() {
        return prefix;
    }

    @Override
    public Component parse(String legacyText) {
        return LEGACY.deserialize(legacyText == null ? "" : legacyText);
    }

    @Override
    public void send(CommandSender recipient, Component message) {
        Objects.requireNonNull(recipient, "recipient");
        Objects.requireNonNull(message, "message");
        recipient.sendMessage(prefix.append(message));
    }
}
