package com.mira.core.service;

import com.mira.core.api.CooldownService;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CoreCooldownService implements CooldownService {
    private final ConcurrentMap<CooldownKey, Long> expiries = new ConcurrentHashMap<>();

    @Override
    public void start(UUID playerId, String key, Duration duration) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) throw new IllegalArgumentException("duration cannot be negative");
        String normalized = normalize(key);
        if (duration.isZero()) {
            expiries.remove(new CooldownKey(playerId, normalized));
            return;
        }
        long millis;
        try {
            millis = duration.toMillis();
        } catch (ArithmeticException exception) {
            millis = Long.MAX_VALUE;
        }
        long now = System.currentTimeMillis();
        long expiry = millis >= Long.MAX_VALUE - now ? Long.MAX_VALUE : now + millis;
        expiries.put(new CooldownKey(playerId, normalized), expiry);
    }

    @Override
    public boolean active(UUID playerId, String key) {
        return !remaining(playerId, key).isZero();
    }

    @Override
    public Duration remaining(UUID playerId, String key) {
        Objects.requireNonNull(playerId, "playerId");
        CooldownKey cooldownKey = new CooldownKey(playerId, normalize(key));
        Long expiry = expiries.get(cooldownKey);
        if (expiry == null) return Duration.ZERO;
        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0L) {
            expiries.remove(cooldownKey, expiry);
            return Duration.ZERO;
        }
        return Duration.ofMillis(remaining);
    }

    @Override
    public boolean clear(UUID playerId, String key) {
        Objects.requireNonNull(playerId, "playerId");
        return expiries.remove(new CooldownKey(playerId, normalize(key))) != null;
    }

    @Override
    public int clear(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        int before = expiries.size();
        expiries.keySet().removeIf(key -> key.playerId().equals(playerId));
        return before - expiries.size();
    }

    @Override
    public void clearAll() {
        expiries.clear();
    }

    private String normalize(String key) {
        Objects.requireNonNull(key, "key");
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) throw new IllegalArgumentException("cooldown key cannot be blank");
        return normalized;
    }

    private record CooldownKey(UUID playerId, String key) {
    }
}
