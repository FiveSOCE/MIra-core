package com.mira.core.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoreServiceRegistryTest {
    @Test
    void registerLookupAndUnregisterRoundTrip() {
        CoreServiceRegistry registry = new CoreServiceRegistry();
        Runnable service = () -> { };

        registry.register(Runnable.class, service);
        assertSame(service, registry.get(Runnable.class).orElseThrow());
        assertTrue(registry.registeredTypes().contains(Runnable.class));
        assertTrue(registry.unregister(Runnable.class, service));
        assertTrue(registry.get(Runnable.class).isEmpty());
    }

    @Test
    void rejectsReplacementService() {
        CoreServiceRegistry registry = new CoreServiceRegistry();
        registry.register(CharSequence.class, "first");
        assertThrows(IllegalStateException.class, () -> registry.register(CharSequence.class, "second"));
    }
}
