package com.mira.core.api;

public interface MiraCore {
    String version();

    MessageService messages();

    ServiceRegistry services();

    CooldownService cooldowns();

    ModuleRegistry modules();

    DiagnosticReport runDiagnostics();
}
