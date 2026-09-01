package com.mira.core.service;

import com.mira.core.api.ServiceRegistry;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class CoreServiceRegistry implements ServiceRegistry {
    private final ConcurrentMap<Class<?>, Object> services = new ConcurrentHashMap<>();

    @Override
    public <T> void register(Class<T> type, T service) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(service, "service");
        if (!type.isInstance(service)) {
            throw new IllegalArgumentException("Service does not implement " + type.getName());
        }
        Object previous = services.putIfAbsent(type, service);
        if (previous != null && previous != service) {
            throw new IllegalStateException("A service is already registered for " + type.getName());
        }
    }

    @Override
    public <T> Optional<T> get(Class<T> type) {
        Objects.requireNonNull(type, "type");
        Object service = services.get(type);
        return service == null ? Optional.empty() : Optional.of(type.cast(service));
    }

    @Override
    public <T> boolean unregister(Class<T> type, T service) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(service, "service");
        return services.remove(type, service);
    }

    @Override
    public Set<Class<?>> registeredTypes() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(services.keySet()));
    }
}
