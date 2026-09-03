package com.mira.core;

import com.mira.core.api.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class MiraCoreCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("status", "test", "reload", "why", "audit", "profiles", "help");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private final MiraCorePlugin plugin;

    public MiraCoreCommand(MiraCorePlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("miracore.admin")) {
            plugin.messages().send(sender, Component.text("You do not have permission to use MiraCore admin commands.", NamedTextColor.RED));
            return true;
        }
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "status" -> showStatus(sender);
            case "test" -> runTests(sender);
            case "reload" -> reload(sender);
            case "why" -> why(sender, args);
            case "audit" -> audit(sender, args);
            case "profiles" -> profiles(sender);
            case "help" -> help(sender);
            default -> help(sender);
        };
    }

    private boolean showStatus(CommandSender sender) {
        Runtime runtime = Runtime.getRuntime();
        long used = (runtime.totalMemory() - runtime.freeMemory()) / 1024L / 1024L;
        long max = runtime.maxMemory() / 1024L / 1024L;
        plugin.messages().send(sender, Component.text("Mira Suite Dashboard", NamedTextColor.LIGHT_PURPLE));
        plugin.messages().send(sender, Component.text("Core v" + plugin.api().version() + " | Paper " + plugin.getServer().getMinecraftVersion(), NamedTextColor.GRAY));
        plugin.messages().send(sender, Component.text("Players " + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers()
                + " | Cached profiles " + plugin.api().profiles().cached().size() + " | Memory " + used + "/" + max + " MB", NamedTextColor.AQUA));
        List<ModuleSnapshot> modules = plugin.api().modules().all();
        for (ModuleSnapshot module : modules) {
            Component line = Component.text(" • " + module.displayName() + " v" + module.version() + " ", NamedTextColor.GRAY)
                    .append(Component.text("[" + module.health().name() + "]", healthColor(module.health())));
            if (!module.detail().isBlank()) line = line.append(Component.text(" " + module.detail(), NamedTextColor.DARK_GRAY));
            plugin.messages().send(sender, line);
        }
        return true;
    }

    private boolean runTests(CommandSender sender) {
        DiagnosticReport report = plugin.api().runDiagnostics();
        plugin.messages().send(sender, Component.text("MiraCore self-test: " + report.passedCount() + "/" + report.checks().size() + " passed", report.passed() ? NamedTextColor.GREEN : NamedTextColor.RED));
        for (DiagnosticCheck check : report.checks()) {
            plugin.messages().send(sender, Component.text(check.passed() ? " ✔ " : " ✘ ", check.passed() ? NamedTextColor.GREEN : NamedTextColor.RED)
                    .append(Component.text(check.name(), NamedTextColor.WHITE))
                    .append(Component.text(" - " + check.detail(), NamedTextColor.GRAY)));
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        plugin.reloadCoreConfiguration();
        plugin.api().audit().record("MiraCore", "RELOAD", sender instanceof org.bukkit.entity.Player p ? p.getUniqueId() : null, sender.getName(), "MiraCore", "Reloaded MiraCore configuration.");
        plugin.messages().send(sender, Component.text("MiraCore configuration reloaded.", NamedTextColor.GREEN));
        return true;
    }

    private boolean why(CommandSender sender, String[] args) {
        if (args.length < 3) {
            plugin.messages().send(sender, Component.text("Usage: /miracore why <player> <permission>", NamedTextColor.RED));
            return true;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
        CommandSender target = offline.getPlayer();
        if (target == null) {
            plugin.messages().send(sender, Component.text("That player must be online for live permission debugging.", NamedTextColor.RED));
            return true;
        }
        PermissionDebugService.Result result = plugin.api().permissionDebug().inspect(target, args[2]);
        plugin.messages().send(sender, Component.text(target.getName() + " -> " + result.permission() + " = " + result.granted(), result.granted() ? NamedTextColor.GREEN : NamedTextColor.RED));
        plugin.messages().send(sender, Component.text(result.explanation(), NamedTextColor.GRAY));
        for (String match : result.matchedAttachments()) plugin.messages().send(sender, Component.text(" • " + match, NamedTextColor.DARK_GRAY));
        return true;
    }

    private boolean audit(CommandSender sender, String[] args) {
        String query = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
        List<AuditEntry> entries = query == null ? plugin.api().audit().recent(15) : plugin.api().audit().search(query, 15);
        plugin.messages().send(sender, Component.text("Mira Audit" + (query == null ? "" : " search: " + query), NamedTextColor.LIGHT_PURPLE));
        if (entries.isEmpty()) plugin.messages().send(sender, Component.text("No matching entries.", NamedTextColor.GRAY));
        for (AuditEntry entry : entries) {
            plugin.messages().send(sender, Component.text(TIME.format(entry.time()) + " [" + entry.source() + "/" + entry.action() + "] ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(entry.actorName() + " -> " + entry.target() + ": " + entry.message(), NamedTextColor.GRAY)));
        }
        return true;
    }

    private boolean profiles(CommandSender sender) {
        List<PlayerProfile> profiles = plugin.api().profiles().cached().stream().sorted(Comparator.comparing(PlayerProfile::lastSeen).reversed()).limit(15).toList();
        plugin.messages().send(sender, Component.text("Cached Mira player profiles: " + plugin.api().profiles().cached().size(), NamedTextColor.AQUA));
        for (PlayerProfile profile : profiles) {
            plugin.messages().send(sender, Component.text(" • " + profile.name() + " " + (profile.online() ? "ONLINE" : "last " + TIME.format(profile.lastSeen())), profile.online() ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        }
        return true;
    }

    private boolean help(CommandSender sender) {
        plugin.messages().send(sender, Component.text("/miracore status", NamedTextColor.AQUA).append(Component.text(" - Suite health dashboard", NamedTextColor.GRAY)));
        plugin.messages().send(sender, Component.text("/miracore test", NamedTextColor.AQUA).append(Component.text(" - Run Core diagnostics", NamedTextColor.GRAY)));
        plugin.messages().send(sender, Component.text("/miracore why <player> <permission>", NamedTextColor.AQUA).append(Component.text(" - Explain live permission result", NamedTextColor.GRAY)));
        plugin.messages().send(sender, Component.text("/miracore audit [query]", NamedTextColor.AQUA).append(Component.text(" - Search global Mira audit log", NamedTextColor.GRAY)));
        plugin.messages().send(sender, Component.text("/miracore profiles", NamedTextColor.AQUA).append(Component.text(" - Inspect shared profile cache", NamedTextColor.GRAY)));
        plugin.messages().send(sender, Component.text("/miracore reload", NamedTextColor.AQUA).append(Component.text(" - Reload Core config", NamedTextColor.GRAY)));
        return true;
    }

    private NamedTextColor healthColor(ModuleHealth health) {
        return switch (health) {
            case HEALTHY -> NamedTextColor.GREEN;
            case DEGRADED -> NamedTextColor.YELLOW;
            case UNHEALTHY -> NamedTextColor.RED;
            case DISABLED -> NamedTextColor.DARK_GRAY;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("miracore.admin")) return List.of();
        if (args.length == 1) {
            String typed = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(sub -> sub.startsWith(typed)).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("why")) {
            String typed = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream().map(org.bukkit.entity.Player::getName).filter(n -> n.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
        }
        return List.of();
    }
}
