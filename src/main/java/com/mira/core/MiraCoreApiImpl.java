package com.mira.core;

import com.mira.core.api.CooldownService;
import com.mira.core.api.DiagnosticReport;
import com.mira.core.api.MessageService;
import com.mira.core.api.MiraCore;
import com.mira.core.api.ModuleRegistry;
import com.mira.core.api.ServiceRegistry;
import com.mira.core.service.CoreDiagnostics;

public final class MiraCoreApiImpl implements MiraCore {
    private final String version;
    private final MessageService messages;
    private final ServiceRegistry services;
    private final CooldownService cooldowns;
    private final ModuleRegistry modules;
    private CoreDiagnostics diagnostics;

    public MiraCoreApiImpl(String version, MessageService messages, ServiceRegistry services, CooldownService cooldowns, ModuleRegistry modules) {
        this.version = version;
        this.messages = messages;
        this.services = services;
        this.cooldowns = cooldowns;
        this.modules = modules;
    }

    public void diagnostics(CoreDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public MessageService messages() {
        return messages;
    }

    @Override
    public ServiceRegistry services() {
        return services;
    }

    @Override
    public CooldownService cooldowns() {
        return cooldowns;
    }

    @Override
    public ModuleRegistry modules() {
        return modules;
    }

    @Override
    public DiagnosticReport runDiagnostics() {
        if (diagnostics == null) throw new IllegalStateException("MiraCore diagnostics are not initialized");
        return diagnostics.run();
    }
}
