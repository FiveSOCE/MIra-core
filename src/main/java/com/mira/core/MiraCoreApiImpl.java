package com.mira.core;

import com.mira.core.api.*;
import com.mira.core.service.CoreDiagnostics;

public final class MiraCoreApiImpl implements MiraCore {
    private final String version;
    private final MessageService messages;
    private final ServiceRegistry services;
    private final CooldownService cooldowns;
    private final ModuleRegistry modules;
    private final PlayerProfileService profiles;
    private final NotificationService notifications;
    private final AuditService audit;
    private final PaginationService pagination;
    private final PermissionDebugService permissionDebug;
    private final MilestoneService milestones;
    private final BossBarService bossBars;
    private final MaintenanceService maintenance;
    private final UpdateService updates;
    private CoreDiagnostics diagnostics;

    public MiraCoreApiImpl(String version, MessageService messages, ServiceRegistry services, CooldownService cooldowns,
                           ModuleRegistry modules, PlayerProfileService profiles, NotificationService notifications,
                           AuditService audit, PaginationService pagination, PermissionDebugService permissionDebug,
                           MilestoneService milestones, BossBarService bossBars, MaintenanceService maintenance,
                           UpdateService updates) {
        this.version = version;
        this.messages = messages;
        this.services = services;
        this.cooldowns = cooldowns;
        this.modules = modules;
        this.profiles = profiles;
        this.notifications = notifications;
        this.audit = audit;
        this.pagination = pagination;
        this.permissionDebug = permissionDebug;
        this.milestones = milestones;
        this.bossBars = bossBars;
        this.maintenance = maintenance;
        this.updates = updates;
    }

    public void diagnostics(CoreDiagnostics diagnostics) { this.diagnostics = diagnostics; }
    @Override public String version() { return version; }
    @Override public MessageService messages() { return messages; }
    @Override public ServiceRegistry services() { return services; }
    @Override public CooldownService cooldowns() { return cooldowns; }
    @Override public ModuleRegistry modules() { return modules; }
    @Override public PlayerProfileService profiles() { return profiles; }
    @Override public NotificationService notifications() { return notifications; }
    @Override public AuditService audit() { return audit; }
    @Override public PaginationService pagination() { return pagination; }
    @Override public PermissionDebugService permissionDebug() { return permissionDebug; }
    @Override public MilestoneService milestones() { return milestones; }
    @Override public BossBarService bossBars() { return bossBars; }
    @Override public MaintenanceService maintenance() { return maintenance; }
    @Override public UpdateService updates() { return updates; }

    @Override
    public DiagnosticReport runDiagnostics() {
        if (diagnostics == null) throw new IllegalStateException("MiraCore diagnostics are not initialized");
        return diagnostics.run();
    }
}
