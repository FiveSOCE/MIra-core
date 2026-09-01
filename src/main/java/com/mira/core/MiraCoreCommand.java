package com.mira.core;

import com.mira.core.api.DiagnosticCheck;
import com.mira.core.api.DiagnosticReport;
import com.mira.core.api.ModuleHealth;
import com.mira.core.api.ModuleSnapshot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Locale;

public final class MiraCoreCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("status", "test", "reload", "help");
    private final MiraCorePlugin plugin;

    public MiraCoreCommand(MiraCorePlugin plugin) {
        this.plugin = plugin;
    }

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
            case "help" -> help(sender);
            default -> help(sender);
        };
    }

    private boolean showStatus(CommandSender sender) {
        plugin.messages().send(sender, Component.text("MiraCore v" + plugin.api().version(), NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(" | Paper " + plugin.getServer().getMinecraftVersion(), NamedTextColor.GRAY)));
        List<ModuleSnapshot> modules = plugin.api().modules().all();
        plugin.messages().send(sender, Component.text("Registered Mira modules: " + modules.size(), NamedTextColor.AQUA));
        for (ModuleSnapshot module : modules) {
            NamedTextColor color = healthColor(module.health());
            Component line = Component.text(" • " + module.displayName() + " v" + module.version() + " ", NamedTextColor.GRAY)
                    .append(Component.text("[" + module.health().name() + "]", color));
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
        plugin.messages().send(sender, Component.text("MiraCore configuration reloaded.", NamedTextColor.GREEN));
        return true;
    }

    private boolean help(CommandSender sender) {
        plugin.messages().send(sender, Component.text("/miracore status", NamedTextColor.AQUA).append(Component.text(" - Shows Mira module health", NamedTextColor.GRAY)));
        plugin.messages().send(sender, Component.text("/miracore test", NamedTextColor.AQUA).append(Component.text(" - Runs MiraCore self-tests", NamedTextColor.GRAY)));
        plugin.messages().send(sender, Component.text("/miracore reload", NamedTextColor.AQUA).append(Component.text(" - Reloads config.yml", NamedTextColor.GRAY)));
        plugin.messages().send(sender, Component.text("/miracore help", NamedTextColor.AQUA).append(Component.text(" - Shows this help", NamedTextColor.GRAY)));
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
        if (args.length != 1) return List.of();
        String typed = args[0].toLowerCase(Locale.ROOT);
        return SUBCOMMANDS.stream().filter(sub -> sub.startsWith(typed)).toList();
    }
}
