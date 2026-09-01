package com.mira.core.event;

import com.mira.core.api.ModuleSnapshot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class MiraModuleHealthChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ModuleSnapshot previous;
    private final ModuleSnapshot current;

    public MiraModuleHealthChangeEvent(ModuleSnapshot previous, ModuleSnapshot current) {
        this.previous = previous;
        this.current = current;
    }

    public ModuleSnapshot previous() {
        return previous;
    }

    public ModuleSnapshot current() {
        return current;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
