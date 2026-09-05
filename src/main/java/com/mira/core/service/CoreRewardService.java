package com.mira.core.service;

import com.mira.core.api.AuditService;
import com.mira.core.api.RewardService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public final class CoreRewardService implements RewardService {
    private final JavaPlugin plugin;
    private final AuditService audit;
    private final File rewardsFile;
    private final File codesFile;
    private final File claimsFile;

    private final Map<UUID, LinkedHashMap<UUID, StoredReward>> rewards = new LinkedHashMap<>();
    private final Map<String, Set<UUID>> claimedCodes = new HashMap<>();
    private YamlConfiguration codeConfig;

    public CoreRewardService(JavaPlugin plugin, AuditService audit) {
        this.plugin = plugin;
        this.audit = audit;
        this.rewardsFile = new File(plugin.getDataFolder(), "rewards.yml");
        this.codesFile = new File(plugin.getDataFolder(), "claim-codes.yml");
        this.claimsFile = new File(plugin.getDataFolder(), "claim-code-claims.yml");

        if (!codesFile.exists()) plugin.saveResource("claim-codes.yml", false);
        loadRewards();
        loadClaims();
        reloadClaimCodes();
    }

    @Override
    public UUID queue(UUID playerId, String source, String title,
                      Collection<ItemStack> items, Collection<String> commands) {
        assertPrimaryThread();
        Objects.requireNonNull(playerId, "playerId");

        List<ItemStack> safeItems = items == null ? List.of() : items.stream()
                .filter(item -> item != null && !item.getType().isAir() && item.getAmount() > 0)
                .map(ItemStack::clone).toList();
        List<String> safeCommands = commands == null ? List.of() : commands.stream()
                .filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).toList();
        if (safeItems.isEmpty() && safeCommands.isEmpty()) {
            throw new IllegalArgumentException("Reward must contain at least one item or command");
        }

        UUID id = UUID.randomUUID();
        StoredReward reward = new StoredReward(id, playerId, clean(source, "Mira"), clean(title, "Reward"),
                System.currentTimeMillis(), new ArrayList<>(safeItems), new ArrayList<>(safeCommands));
        rewards.computeIfAbsent(playerId, ignored -> new LinkedHashMap<>()).put(id, reward);
        saveRewards();

        audit.record("MiraCore", "REWARD_QUEUED", null, reward.source(), playerId.toString(),
                "Reward queued", Map.of("rewardId", id.toString(), "title", reward.title(),
                        "items", Integer.toString(reward.items().size()),
                        "commands", Integer.toString(reward.commands().size())));
        return id;
    }

    @Override
    public List<QueuedReward> pending(UUID playerId) {
        if (playerId == null) return List.of();
        LinkedHashMap<UUID, StoredReward> found = rewards.get(playerId);
        if (found == null) return List.of();
        return found.values().stream().map(StoredReward::view).toList();
    }

    @Override
    public Optional<QueuedReward> reward(UUID playerId, UUID rewardId) {
        if (playerId == null || rewardId == null) return Optional.empty();
        StoredReward found = rewards.getOrDefault(playerId, new LinkedHashMap<>()).get(rewardId);
        return found == null ? Optional.empty() : Optional.of(found.view());
    }

    @Override
    public int pendingCount(UUID playerId) {
        if (playerId == null) return 0;
        return rewards.getOrDefault(playerId, new LinkedHashMap<>()).size();
    }

    @Override
    public ClaimResult claim(Player player, UUID rewardId) {
        assertPrimaryThread();
        if (player == null || rewardId == null) return new ClaimResult(false, false, 0, 0, 0, "Invalid reward.");
        LinkedHashMap<UUID, StoredReward> playerRewards = rewards.get(player.getUniqueId());
        StoredReward stored = playerRewards == null ? null : playerRewards.get(rewardId);
        if (stored == null) return new ClaimResult(false, false, 0, 0, 0, "That reward is no longer pending.");

        List<ItemStack> remainingItems = new ArrayList<>();
        int delivered = 0;
        for (ItemStack original : stored.items()) {
            if (original == null || original.getType().isAir() || original.getAmount() <= 0) continue;
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(original.clone());
            if (leftovers.isEmpty()) {
                delivered++;
            } else {
                int leftAmount = leftovers.values().stream().mapToInt(ItemStack::getAmount).sum();
                if (leftAmount < original.getAmount()) delivered++;
                remainingItems.addAll(leftovers.values().stream().map(ItemStack::clone).toList());
            }
        }

        List<String> remainingCommands = new ArrayList<>();
        int commandsExecuted = 0;
        for (String raw : stored.commands()) {
            String command = raw.startsWith("/") ? raw.substring(1) : raw;
            command = command.replace("%player%", player.getName())
                    .replace("%uuid%", player.getUniqueId().toString());
            boolean dispatched;
            try {
                dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Reward command failed for " + player.getUniqueId() + ": " + exception.getMessage());
                dispatched = false;
            }
            if (dispatched) commandsExecuted++;
            else remainingCommands.add(raw);
        }

        boolean complete = remainingItems.isEmpty() && remainingCommands.isEmpty();
        if (complete) {
            playerRewards.remove(rewardId);
            if (playerRewards.isEmpty()) rewards.remove(player.getUniqueId());
        } else {
            playerRewards.put(rewardId, stored.withRemaining(remainingItems, remainingCommands));
        }
        saveRewards();

        audit.record("MiraCore", complete ? "REWARD_CLAIMED" : "REWARD_PARTIALLY_CLAIMED",
                player.getUniqueId(), player.getName(), rewardId.toString(),
                complete ? "Reward fully delivered" : "Reward partially delivered; overflow remains queued",
                Map.of("deliveredStacks", Integer.toString(delivered),
                        "remainingStacks", Integer.toString(remainingItems.size()),
                        "commandsExecuted", Integer.toString(commandsExecuted),
                        "remainingCommands", Integer.toString(remainingCommands.size())));

        String message = complete
                ? "Reward claimed."
                : "Delivered what could fit. Remaining reward contents are still safely queued.";
        return new ClaimResult(true, complete, delivered, remainingItems.size(), commandsExecuted, message);
    }

    @Override
    public CodeResult claimCode(Player player, String rawCode) {
        assertPrimaryThread();
        if (player == null || rawCode == null) return new CodeResult(false, Optional.empty(), "Invalid claim code.");
        String code = normalizeCode(rawCode);
        if (code.isBlank()) return new CodeResult(false, Optional.empty(), "Invalid claim code.");

        ConfigurationSection section = codeSection(code);
        if (section == null || !section.getBoolean("enabled", true)) {
            return new CodeResult(false, Optional.empty(), "That claim code is not active.");
        }
        if (claimedCodes.getOrDefault(code, Set.of()).contains(player.getUniqueId())) {
            return new CodeResult(false, Optional.empty(), "You have already claimed that code.");
        }

        String permission = section.getString("permission", "").trim();
        if (!permission.isBlank() && !player.hasPermission(permission)) {
            return new CodeResult(false, Optional.empty(), "You do not meet the requirements for that claim code.");
        }

        long expiresAt = Math.max(0L, section.getLong("expires-at", 0L));
        if (expiresAt > 0L && System.currentTimeMillis() >= expiresAt) {
            return new CodeResult(false, Optional.empty(), "That claim code has expired.");
        }

        List<ItemStack> items = parseItems(section);
        List<String> commands = section.getStringList("commands");
        if (items.isEmpty() && commands.stream().noneMatch(value -> value != null && !value.isBlank())) {
            return new CodeResult(false, Optional.empty(), "That claim code has no configured reward.");
        }

        String title = clean(section.getString("title"), code + " Reward");
        UUID rewardId = queue(player.getUniqueId(), "Claim Code " + code, title, items, commands);
        claimedCodes.computeIfAbsent(code, ignored -> new LinkedHashSet<>()).add(player.getUniqueId());
        saveClaims();

        audit.record("MiraCore", "CLAIM_CODE_USED", player.getUniqueId(), player.getName(), code,
                "Global claim code accepted", Map.of("rewardId", rewardId.toString()));
        return new CodeResult(true, Optional.of(rewardId), "Claim code accepted. Your reward is waiting in /rewards.");
    }

    @Override
    public void reloadClaimCodes() {
        codeConfig = YamlConfiguration.loadConfiguration(codesFile);
    }

    public void saveAll() {
        saveRewards();
        saveClaims();
    }

    private ConfigurationSection codeSection(String normalized) {
        ConfigurationSection root = codeConfig.getConfigurationSection("codes");
        if (root == null) return null;
        for (String configured : root.getKeys(false)) {
            if (normalizeCode(configured).equals(normalized)) return root.getConfigurationSection(configured);
        }
        return null;
    }

    private List<ItemStack> parseItems(ConfigurationSection section) {
        List<ItemStack> items = new ArrayList<>();
        for (Map<?, ?> row : section.getMapList("items")) {
            Object materialRaw = row.get("material");
            Material material = materialRaw == null ? null : Material.matchMaterial(String.valueOf(materialRaw));
            if (material == null || material.isAir()) continue;
            int amount = number(row.get("amount"), 1);
            amount = Math.max(1, Math.min(material.getMaxStackSize() * 64, amount));
            while (amount > 0) {
                int stackAmount = Math.min(material.getMaxStackSize(), amount);
                items.add(new ItemStack(material, stackAmount));
                amount -= stackAmount;
            }
        }
        return items;
    }

    private int number(Object raw, int fallback) {
        if (raw instanceof Number number) return number.intValue();
        try { return Integer.parseInt(String.valueOf(raw)); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private void loadRewards() {
        rewards.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(rewardsFile);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) return;

        for (String playerRaw : players.getKeys(false)) {
            UUID playerId;
            try { playerId = UUID.fromString(playerRaw); }
            catch (IllegalArgumentException ignored) { continue; }

            ConfigurationSection playerSection = players.getConfigurationSection(playerRaw);
            if (playerSection == null) continue;
            LinkedHashMap<UUID, StoredReward> found = new LinkedHashMap<>();
            for (String rewardRaw : playerSection.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(rewardRaw);
                    String base = rewardRaw + ".";
                    String source = clean(playerSection.getString(base + "source"), "Mira");
                    String title = clean(playerSection.getString(base + "title"), "Reward");
                    long queuedAt = Math.max(0L, playerSection.getLong(base + "queued-at", System.currentTimeMillis()));

                    List<ItemStack> items = new ArrayList<>();
                    for (Object raw : playerSection.getList(base + "items", List.of())) {
                        if (raw instanceof ItemStack item && !item.getType().isAir()) items.add(item.clone());
                    }
                    List<String> commands = playerSection.getStringList(base + "commands").stream()
                            .filter(Objects::nonNull).map(String::trim).filter(value -> !value.isBlank()).toList();
                    if (!items.isEmpty() || !commands.isEmpty()) {
                        found.put(id, new StoredReward(id, playerId, source, title, queuedAt,
                                new ArrayList<>(items), new ArrayList<>(commands)));
                    }
                } catch (RuntimeException ignored) {
                }
            }
            if (!found.isEmpty()) rewards.put(playerId, found);
        }
    }

    private synchronized void saveRewards() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (var playerEntry : rewards.entrySet()) {
            for (StoredReward reward : playerEntry.getValue().values()) {
                String base = "players." + playerEntry.getKey() + "." + reward.id() + ".";
                yaml.set(base + "source", reward.source());
                yaml.set(base + "title", reward.title());
                yaml.set(base + "queued-at", reward.queuedAt());
                yaml.set(base + "items", reward.items().stream().map(ItemStack::clone).toList());
                yaml.set(base + "commands", reward.commands());
            }
        }
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(rewardsFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save rewards.yml: " + exception.getMessage());
        }
    }

    private void loadClaims() {
        claimedCodes.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(claimsFile);
        ConfigurationSection root = yaml.getConfigurationSection("claims");
        if (root == null) return;
        for (String codeRaw : root.getKeys(false)) {
            String code = normalizeCode(codeRaw);
            LinkedHashSet<UUID> users = new LinkedHashSet<>();
            for (String uuidRaw : root.getStringList(codeRaw)) {
                try { users.add(UUID.fromString(uuidRaw)); }
                catch (IllegalArgumentException ignored) { }
            }
            claimedCodes.put(code, users);
        }
    }

    private synchronized void saveClaims() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (var entry : claimedCodes.entrySet()) {
            yaml.set("claims." + entry.getKey(),
                    entry.getValue().stream().map(UUID::toString).sorted().toList());
        }
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(claimsFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save claim-code-claims.yml: " + exception.getMessage());
        }
    }

    private void assertPrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("MiraCore reward mutations must run on the server thread");
        }
    }

    private static String clean(String raw, String fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        return raw.trim();
    }

    private static String normalizeCode(String raw) {
        if (raw == null) return "";
        String clean = raw.trim().toUpperCase(Locale.ROOT);
        return clean.matches("[A-Z0-9_-]{2,32}") ? clean : "";
    }

    private record StoredReward(UUID id, UUID playerId, String source, String title, long queuedAt,
                                List<ItemStack> items, List<String> commands) {
        private QueuedReward view() {
            return new QueuedReward(id, playerId, source, title, Instant.ofEpochMilli(queuedAt), items, commands);
        }

        private StoredReward withRemaining(List<ItemStack> nextItems, List<String> nextCommands) {
            return new StoredReward(id, playerId, source, title, queuedAt,
                    new ArrayList<>(nextItems), new ArrayList<>(nextCommands));
        }
    }
}
