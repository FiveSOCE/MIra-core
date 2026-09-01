package com.mira.core;

import com.mira.core.api.DiagnosticReport;
import com.mira.core.api.MessageService;
import com.mira.core.api.MiraCore;
import com.mira.core.service.CoreCooldownService;
import com.mira.core.service.CoreDiagnostics;
import com.mira.core.service.CoreMessageService;
import com.mira.core.service.CoreModuleRegistry;
import com.mira.core.service.CoreServiceRegistry;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class MiraCorePlugin extends JavaPlugin {
    private CoreMessageService messageService;
    private CoreServiceRegistry serviceRegistry;
    private CoreCooldownService cooldownService;
    private CoreModuleRegistry moduleRegistry;
    private MiraCoreApiImpl api;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messageService = new CoreMessageService(this);
        serviceRegistry = new CoreServiceRegistry();
        cooldownService = new CoreCooldownService();
        moduleRegistry = new CoreModuleRegistry();

        api = new MiraCoreApiImpl(getPluginMeta().getVersion(), messageService, serviceRegistry, cooldownService, moduleRegistry);
        CoreDiagnostics diagnostics = new CoreDiagnostics(this, api, serviceRegistry, cooldownService, moduleRegistry);
        api.diagnostics(diagnostics);

        getServer().getServicesManager().register(MiraCore.class, api, this, ServicePriority.Normal);
        serviceRegistry.register(MiraCore.class, api);
        moduleRegistry.register(this, "MiraCore");

        MiraCoreCommand executor = new MiraCoreCommand(this);
        PluginCommand command = getCommand("miracore");
        if (command == null) throw new IllegalStateException("miracore command is missing from plugin.yml");
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        getLogger().info("MiraCore v" + api.version() + " enabled. API service registered.");

        if (getConfig().getBoolean("diagnostics.startup-check", true)) {
            getServer().getScheduler().runTask(this, () -> {
                DiagnosticReport report = api.runDiagnostics();
                if (report.passed()) {
                    getLogger().info("Startup diagnostics passed " + report.passedCount() + "/" + report.checks().size() + " checks.");
                } else {
                    getLogger().warning("Startup diagnostics failed: " + report.passedCount() + "/" + report.checks().size() + " checks passed. Run /miracore test for details.");
                }
            });
        }
    }

    @Override
    public void onDisable() {
        if (api != null) getServer().getServicesManager().unregister(MiraCore.class, api);
        if (serviceRegistry != null && api != null) serviceRegistry.unregister(MiraCore.class, api);
        if (cooldownService != null) cooldownService.clearAll();
        if (moduleRegistry != null) moduleRegistry.unregister(this);
    }

    public MiraCoreApiImpl api() {
        return api;
    }

    public MessageService messages() {
        return messageService;
    }

    public void reloadCoreConfiguration() {
        reloadConfig();
        messageService.reload();
    }
}
