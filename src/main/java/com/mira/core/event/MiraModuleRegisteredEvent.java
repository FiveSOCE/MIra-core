package com.mira.core.event;

import com.mira.core.api.ModuleSnapshot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class MiraModuleRegisteredEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ModuleSnapshot module;

    public MiraModuleRegisteredEvent(ModuleSnapshot module) {
        this.module = module;
    }

    public ModuleSnapshot module() {
        return module;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
