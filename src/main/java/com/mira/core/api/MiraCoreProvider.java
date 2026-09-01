package com.mira.core.api;

import org.bukkit.Bukkit;

import java.util.Optional;

public final class MiraCoreProvider {
    private MiraCoreProvider() {
    }

    public static Optional<MiraCore> get() {
        return Optional.ofNullable(Bukkit.getServicesManager().load(MiraCore.class));
    }

    public static MiraCore require() {
        return get().orElseThrow(() -> new IllegalStateException("MiraCore API is not available. Is MiraCore installed and enabled?"));
    }
}
