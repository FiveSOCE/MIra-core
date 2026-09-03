package com.mira.core.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PlayerProfile(UUID uuid, String name, Instant firstSeen, Instant lastSeen, boolean online, Map<String, String> metadata) {}
