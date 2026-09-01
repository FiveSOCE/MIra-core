package com.mira.core.service;

import com.mira.core.api.CooldownService;
import com.mira.core.api.DiagnosticCheck;
import com.mira.core.api.DiagnosticReport;
import com.mira.core.api.MiraCore;
import com.mira.core.api.ModuleRegistry;
import com.mira.core.api.ServiceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CoreDiagnostics {
    private final JavaPlugin plugin;
    private final MiraCore api;
    private final ServiceRegistry services;
    private final CooldownService cooldowns;
    private final ModuleRegistry modules;

    public CoreDiagnostics(JavaPlugin plugin, MiraCore api, ServiceRegistry services, CooldownService cooldowns, ModuleRegistry modules) {
        this.plugin = plugin;
        this.api = api;
        this.services = services;
        this.cooldowns = cooldowns;
        this.modules = modules;
    }

    public DiagnosticReport run() {
        List<DiagnosticCheck> checks = new ArrayList<>();
        checks.add(check("Plugin enabled", plugin.isEnabled(), plugin.isEnabled() ? "MiraCore is enabled" : "Plugin reports disabled"));
        checks.add(check("Primary thread", Bukkit.isPrimaryThread(), Bukkit.isPrimaryThread() ? "Running on server thread" : "Diagnostics were invoked asynchronously"));
        checks.add(check("Configuration", plugin.getDataFolder().exists() && new java.io.File(plugin.getDataFolder(), "config.yml").isFile(), "config.yml presence"));
        checks.add(check("Bukkit API service", Bukkit.getServicesManager().load(MiraCore.class) == api, "MiraCore API is registered with Bukkit ServicesManager"));
        checks.add(check("Module registry", modules.get(plugin.getName()).isPresent(), "MiraCore module registration"));

        DiagnosticMarker marker = new DiagnosticMarker();
        boolean serviceRoundTrip = false;
        try {
            services.register(DiagnosticMarker.class, marker);
            serviceRoundTrip = services.get(DiagnosticMarker.class).orElse(null) == marker;
        } finally {
            services.unregister(DiagnosticMarker.class, marker);
        }
        checks.add(check("Service registry", serviceRoundTrip, "register/get/unregister round-trip"));

        UUID testId = UUID.randomUUID();
        cooldowns.start(testId, "miracore:diagnostic", Duration.ofSeconds(1));
        boolean cooldownRoundTrip = cooldowns.active(testId, "miracore:diagnostic")
                && !cooldowns.remaining(testId, "miracore:diagnostic").isZero();
        cooldowns.clear(testId, "miracore:diagnostic");
        cooldownRoundTrip = cooldownRoundTrip && !cooldowns.active(testId, "miracore:diagnostic");
        checks.add(check("Cooldown service", cooldownRoundTrip, "start/remaining/clear round-trip"));

        return new DiagnosticReport(Instant.now(), checks);
    }

    private DiagnosticCheck check(String name, boolean passed, String detail) {
        return new DiagnosticCheck(name, passed, detail);
    }

    private static final class DiagnosticMarker {
    }
}
