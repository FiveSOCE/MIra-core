package com.mira.core.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RewardService {
    record QueuedReward(UUID id, UUID playerId, String source, String title, Instant queuedAt,
                        List<ItemStack> items, List<String> commands) {
        public QueuedReward {
            items = items == null ? List.of() : items.stream().filter(item -> item != null && !item.getType().isAir())
                    .map(ItemStack::clone).toList();
            commands = commands == null ? List.of() : List.copyOf(commands);
        }
    }

    record ClaimResult(boolean success, boolean complete, int deliveredStacks,
                       int remainingStacks, int commandsExecuted, String message) { }

    record CodeResult(boolean success, Optional<UUID> rewardId, String message) {
        public CodeResult {
            rewardId = rewardId == null ? Optional.empty() : rewardId;
        }
    }

    UUID queue(UUID playerId, String source, String title,
               Collection<ItemStack> items, Collection<String> commands);

    List<QueuedReward> pending(UUID playerId);

    Optional<QueuedReward> reward(UUID playerId, UUID rewardId);

    int pendingCount(UUID playerId);

    ClaimResult claim(Player player, UUID rewardId);

    CodeResult claimCode(Player player, String code);

    void reloadClaimCodes();
}
