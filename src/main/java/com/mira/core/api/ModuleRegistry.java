package com.mira.core.api;

import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Optional;

public interface ModuleRegistry {
    ModuleSnapshot register(Plugin plugin);

    ModuleSnapshot register(Plugin plugin, String displayName);

    Optional<ModuleSnapshot> get(String pluginName);

    List<ModuleSnapshot> all();

    ModuleSnapshot setHealth(Plugin plugin, ModuleHealth health, String detail);

    boolean unregister(Plugin plugin);
}
