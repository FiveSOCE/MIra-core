package com.mira.core.api;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface UpdateService {
    record UpdateStatus(String pluginName, String installedVersion, String latestVersion,
                        String repository, boolean updateAvailable, boolean reachable, String detail) { }

    CompletableFuture<List<UpdateStatus>> checkNow();
    List<UpdateStatus> cached();
    Optional<UpdateStatus> cached(String pluginName);
}
