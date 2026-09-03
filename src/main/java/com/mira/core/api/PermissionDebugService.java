package com.mira.core.api;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface PermissionDebugService {
    Result inspect(CommandSender sender, String permission);

    record Result(String permission, boolean granted, boolean op, List<String> matchedAttachments, String explanation) {}
}
