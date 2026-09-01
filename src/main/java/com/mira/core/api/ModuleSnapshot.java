package com.mira.core.api;

import java.util.Objects;

public record ModuleSnapshot(
        String pluginName,
        String displayName,
        String version,
        ModuleHealth health,
        String detail,
        boolean enabled
) {
    public ModuleSnapshot {
        Objects.requireNonNull(pluginName, "pluginName");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(health, "health");
        detail = detail == null ? "" : detail;
    }
}
