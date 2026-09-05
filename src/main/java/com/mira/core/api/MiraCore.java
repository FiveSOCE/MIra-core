package com.mira.core.api;

public interface MiraCore {
    String version();

    MessageService messages();

    ServiceRegistry services();

    CooldownService cooldowns();

    ModuleRegistry modules();

    PlayerProfileService profiles();

    NotificationService notifications();

    AuditService audit();

    PaginationService pagination();

    PermissionDebugService permissionDebug();

    MilestoneService milestones();

    BossBarService bossBars();

    MaintenanceService maintenance();

    UpdateService updates();

    RewardService rewards();

    DiagnosticReport runDiagnostics();
}
