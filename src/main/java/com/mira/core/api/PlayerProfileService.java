package com.mira.core.api;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PlayerProfileService {
    Optional<PlayerProfile> get(UUID uuid);
    Optional<PlayerProfile> get(String name);
    PlayerProfile touch(UUID uuid, String name, boolean online);
    void metadata(UUID uuid, String key, String value);
    Collection<PlayerProfile> cached();
}
