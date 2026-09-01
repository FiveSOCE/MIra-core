package com.mira.core.api;

import java.util.Optional;
import java.util.Set;

public interface ServiceRegistry {
    <T> void register(Class<T> type, T service);

    <T> Optional<T> get(Class<T> type);

    <T> boolean unregister(Class<T> type, T service);

    Set<Class<?>> registeredTypes();
}
