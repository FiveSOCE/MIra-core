package com.mira.core;

import com.mira.core.api.MessageService;
import com.mira.core.api.RewardService;
import com.mira.core.gui.CoreRewardGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class CorePlayerRewardCommand implements CommandExecutor {
    private final RewardService rewards;
    private final MessageService messages;
    private final CoreRewardGui gui;

    public CorePlayerRewardCommand(RewardService rewards, MessageService messages, CoreRewardGui gui) {
        this.rewards = rewards;
        this.messages = messages;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "&cThis command is player-only.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("rewards")) {
            gui.openList(player, 0);
            return true;
        }

        if (command.getName().equalsIgnoreCase("claim")) {
            if (args.length < 1) {
                messages.send(player, "&eUsage: /claim <code>");
                return true;
            }
            RewardService.CodeResult result = rewards.claimCode(player, args[0]);
            messages.send(player, result.success() ? "&a" + result.message() : "&c" + result.message());
            if (result.success()) gui.openList(player, 0);
            return true;
        }

        return true;
    }
}
