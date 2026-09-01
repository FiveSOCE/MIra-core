package com.mira.core.api;

import java.time.Instant;
import java.util.List;

public record DiagnosticReport(Instant ranAt, List<DiagnosticCheck> checks) {
    public DiagnosticReport {
        checks = List.copyOf(checks);
    }

    public boolean passed() {
        return checks.stream().allMatch(DiagnosticCheck::passed);
    }

    public long passedCount() {
        return checks.stream().filter(DiagnosticCheck::passed).count();
    }
}
