package com.mira.core.api;

import java.time.Duration;
import java.util.UUID;

public interface CooldownService {
    void start(UUID playerId, String key, Duration duration);

    boolean active(UUID playerId, String key);

    Duration remaining(UUID playerId, String key);

    boolean clear(UUID playerId, String key);

    int clear(UUID playerId);

    void clearAll();
}
