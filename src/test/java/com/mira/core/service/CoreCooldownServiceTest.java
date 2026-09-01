package com.mira.core.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CoreCooldownServiceTest {
    @Test
    void cooldownRoundTrip() {
        CoreCooldownService service = new CoreCooldownService();
        UUID player = UUID.randomUUID();

        service.start(player, "Combat:Test", Duration.ofSeconds(5));
        assertTrue(service.active(player, "combat:test"));
        assertTrue(service.remaining(player, "COMBAT:TEST").toMillis() > 0L);
        assertTrue(service.clear(player, "combat:test"));
        assertFalse(service.active(player, "combat:test"));
    }

    @Test
    void zeroDurationClearsCooldown() {
        CoreCooldownService service = new CoreCooldownService();
        UUID player = UUID.randomUUID();
        service.start(player, "test", Duration.ofSeconds(5));
        service.start(player, "test", Duration.ZERO);
        assertFalse(service.active(player, "test"));
    }

    @Test
    void blankKeysAreRejected() {
        CoreCooldownService service = new CoreCooldownService();
        assertThrows(IllegalArgumentException.class, () -> service.start(UUID.randomUUID(), "   ", Duration.ofSeconds(1)));
    }
}
