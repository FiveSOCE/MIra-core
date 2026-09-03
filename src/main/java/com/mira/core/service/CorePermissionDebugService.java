package com.mira.core.service;

import com.mira.core.api.PermissionDebugService;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CorePermissionDebugService implements PermissionDebugService {
    @Override
    public Result inspect(CommandSender sender, String permission) {
        if (sender == null || permission == null || permission.isBlank()) {
            return new Result(permission == null ? "" : permission, false, false, List.of(), "Invalid sender or permission.");
        }
        boolean granted = sender.hasPermission(permission);
        List<String> matched = new ArrayList<>();
        String lower = permission.toLowerCase(Locale.ROOT);
        for (PermissionAttachmentInfo info : sender.getEffectivePermissions()) {
            String node = info.getPermission().toLowerCase(Locale.ROOT);
            if (node.equals(lower) || wildcardMatches(node, lower)) {
                matched.add(info.getPermission() + "=" + info.getValue() + (info.getAttachment() == null ? "" : " via attachment"));
            }
        }
        String explanation;
        if (sender.isOp() && matched.isEmpty()) explanation = "Result is " + granted + "; sender is OP and no explicit matching attachment was found.";
        else if (matched.isEmpty()) explanation = "Result is " + granted + "; no explicit effective permission attachment matched the node.";
        else explanation = "Result is " + granted + "; matching effective permission entries are shown below.";
        return new Result(permission, granted, sender.isOp(), List.copyOf(matched), explanation);
    }

    private boolean wildcardMatches(String granted, String requested) {
        if (!granted.endsWith(".*")) return false;
        String prefix = granted.substring(0, granted.length() - 1);
        return requested.startsWith(prefix);
    }
}
