package com.mira.core.api;

import java.time.Instant;
import java.util.Optional;

public interface MaintenanceService {
    boolean enabled();
    String kickMessage();
    String motd();
    String bypassPermission();
    Optional<Instant> scheduledStart();
    Optional<Instant> scheduledEnd();
    void enable(String actor);
    void disable(String actor);
    void schedule(Instant start, Instant end, String actor);
    void clearSchedule(String actor);
}
