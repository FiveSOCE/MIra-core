package com.mira.core.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface MilestoneService {
    boolean award(UUID player, String key, String source, Map<String, String> metadata);
    boolean has(UUID player, String key);
    List<Milestone> all(UUID player);

    record Milestone(UUID player, String key, String source, Instant awardedAt, Map<String, String> metadata) {}
}
