package com.mira.core.api;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEntry(Instant time, String source, String action, UUID actor, String actorName, String target, String message, Map<String, String> metadata) {}
