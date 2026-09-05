package com.mira.core.listener;

import com.mira.core.api.MessageService;
import com.mira.core.api.RewardService;
import com.mira.core.gui.CoreRewardGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.UUID;

public final class CoreRewardGuiListener implements Listener {
    private final RewardService rewards;
    private final MessageService messages;
    private final CoreRewardGui gui;

    public CoreRewardGuiListener(RewardService rewards, MessageService messages, CoreRewardGui gui) {
        this.rewards = rewards;
        this.messages = messages;
        this.gui = gui;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getView().getTopInventory().getHolder() instanceof CoreRewardGui.ListHolder holder) {
            event.setCancelled(true);
            int raw = event.getRawSlot();
            if (raw >= 0 && raw < 45) {
                UUID id = gui.rewardAt(player, holder.page(), raw);
                if (id != null) gui.openPreview(player, id, 0);
            } else if (raw == 45 && holder.page() > 0) {
                gui.openList(player, holder.page() - 1);
            } else if (raw == 53) {
                gui.openList(player, holder.page() + 1);
            }
            return;
        }

        if (event.getView().getTopInventory().getHolder() instanceof CoreRewardGui.PreviewHolder holder) {
            event.setCancelled(true);
            int raw = event.getRawSlot();
            if (raw == 45 && holder.page() > 0) {
                gui.openPreview(player, holder.rewardId(), holder.page() - 1);
            } else if (raw == 49) {
                RewardService.ClaimResult result = rewards.claim(player, holder.rewardId());
                messages.send(player, result.success() ? "&a" + result.message() : "&c" + result.message());
                if (result.complete()) gui.openList(player, 0);
                else gui.openPreview(player, holder.rewardId(), holder.page());
            } else if (raw == 50) {
                gui.openList(player, 0);
            } else if (raw == 53) {
                gui.openPreview(player, holder.rewardId(), holder.page() + 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof CoreRewardGui.ListHolder
                || event.getView().getTopInventory().getHolder() instanceof CoreRewardGui.PreviewHolder) {
            event.setCancelled(true);
        }
    }
}
