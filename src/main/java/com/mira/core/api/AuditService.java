package com.mira.core.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AuditService {
    void record(String source, String action, UUID actor, String actorName, String target, String message, Map<String, String> metadata);
    default void record(String source, String action, UUID actor, String actorName, String target, String message) {
        record(source, action, actor, actorName, target, message, Map.of());
    }
    List<AuditEntry> recent(int limit);
    List<AuditEntry> search(String query, int limit);
}
