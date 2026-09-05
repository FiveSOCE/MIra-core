package com.mira.core.api;

import java.time.Instant;
import java.util.Optional;

public interface MaintenanceService {
    boolean enabled();
    String kickMessage();
    String motd();
    String bypassPermission();
    Optional<String> reason();
    Optional<Instant> scheduledStart();
    Optional<Instant> scheduledEnd();
    void enable(String actor);
    default void enable(String actor, String reason) { enable(actor); }
    void disable(String actor);
    void schedule(Instant start, Instant end, String actor);
    default void schedule(Instant start, Instant end, String actor, String reason) { schedule(start, end, actor); }
    void clearSchedule(String actor);
}
