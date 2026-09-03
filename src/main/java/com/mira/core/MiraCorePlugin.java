package com.mira.core;

import com.mira.core.api.*;
import com.mira.core.listener.CoreProfileListener;
import com.mira.core.service.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class MiraCorePlugin extends JavaPlugin {
    private CoreMessageService messageService;
    private CoreServiceRegistry serviceRegistry;
    private CoreCooldownService cooldownService;
    private CoreModuleRegistry moduleRegistry;
    private CorePlayerProfileService profileService;
    private CoreNotificationService notificationService;
    private CoreAuditService auditService;
    private CorePaginationService paginationService;
    private CorePermissionDebugService permissionDebugService;
    private CoreMilestoneService milestoneService;
    private MiraCoreApiImpl api;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messageService = new CoreMessageService(this);
        serviceRegistry = new CoreServiceRegistry();
        cooldownService = new CoreCooldownService();
        moduleRegistry = new CoreModuleRegistry();
        profileService = new CorePlayerProfileService(this);
        notificationService = new CoreNotificationService();
        auditService = new CoreAuditService(this);
        paginationService = new CorePaginationService();
        permissionDebugService = new CorePermissionDebugService();
        milestoneService = new CoreMilestoneService(this);

        api = new MiraCoreApiImpl(getPluginMeta().getVersion(), messageService, serviceRegistry, cooldownService, moduleRegistry,
                profileService, notificationService, auditService, paginationService, permissionDebugService, milestoneService);
        CoreDiagnostics diagnostics = new CoreDiagnostics(this, api, serviceRegistry, cooldownService, moduleRegistry);
        api.diagnostics(diagnostics);

        getServer().getServicesManager().register(MiraCore.class, api, this, ServicePriority.Normal);
        serviceRegistry.register(MiraCore.class, api);
        serviceRegistry.register(PlayerProfileService.class, profileService);
        serviceRegistry.register(NotificationService.class, notificationService);
        serviceRegistry.register(AuditService.class, auditService);
        serviceRegistry.register(PaginationService.class, paginationService);
        serviceRegistry.register(PermissionDebugService.class, permissionDebugService);
        serviceRegistry.register(MilestoneService.class, milestoneService);
        moduleRegistry.register(this, "MiraCore");

        getServer().getPluginManager().registerEvents(new CoreProfileListener(profileService), this);
        for (Player player : getServer().getOnlinePlayers()) profileService.touch(player.getUniqueId(), player.getName(), true);

        MiraCoreCommand executor = new MiraCoreCommand(this);
        PluginCommand command = getCommand("miracore");
        if (command == null) throw new IllegalStateException("miracore command is missing from plugin.yml");
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        getLogger().info("MiraCore v" + api.version() + " enabled. Shared suite services registered.");

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
        if (profileService != null) {
            for (Player player : getServer().getOnlinePlayers()) profileService.touch(player.getUniqueId(), player.getName(), false);
        }
    }

    public MiraCoreApiImpl api() { return api; }
    public MessageService messages() { return messageService; }
    public void reloadCoreConfiguration() { reloadConfig(); messageService.reload(); }
}
