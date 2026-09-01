package com.mira.core.api;

import java.util.Objects;

public record DiagnosticCheck(String name, boolean passed, String detail) {
    public DiagnosticCheck {
        Objects.requireNonNull(name, "name");
        detail = detail == null ? "" : detail;
    }
}
