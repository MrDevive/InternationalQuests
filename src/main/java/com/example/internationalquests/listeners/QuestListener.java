package com.example.internationalquests.listeners;

import com.example.internationalquests.InternationalQuests;
import com.example.internationalquests.hooks.InternationalLevelHook;
import com.example.internationalquests.models.ActiveQuest;
import com.example.internationalquests.models.PlayerData;
import com.example.internationalquests.models.Quest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class QuestListener implements Listener {

    private final InternationalQuests plugin;
    private InternationalLevelHook levelHook;
    private final Map<UUID, Location> lastBlockLocation = new HashMap<>();
    private final Map<UUID, Double> accumulatedDistance = new HashMap<>();

    // Кэш для hasWalkQuest
    private final Map<UUID, Boolean> walkQuestCache = new HashMap<>();
    private final Map<UUID, Long> walkQuestCacheTime = new HashMap<>();
    private static final long CACHE_DURATION = 1000;

    public QuestListener(InternationalQuests plugin) {
        this.plugin = plugin;
        setupLevelHook();
    }

    private void setupLevelHook() {
        this.levelHook = new InternationalLevelHook(plugin);
        if (levelHook.isEnabled()) {
            // Устанавливаем слушатель через событие
            levelHook.setExpGainListener((player, amount, source) -> {
                PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
                if (data == null) return;

                // Засчитываем для ежедневных квестов
                checkExpGainQuest(player, data.getActiveQuests(), amount, false);
                // Засчитываем для еженедельных квестов
                checkExpGainQuest(player, data.getWeeklyQuests(), amount, true);
            });
            plugin.getServer().getPluginManager().registerEvents(levelHook, plugin);
            plugin.getLogger().info("§aИнтеграция с InternationalLevel через события успешно настроена!");
        }
    }

    private void checkExpGainQuest(Player player, List<ActiveQuest> quests, long amount, boolean isWeekly) {
        for (ActiveQuest aq : quests) {
            Quest quest = isWeekly ?
                    plugin.getQuestManager().getWeeklyQuestById(aq.getQuestId()) :
                    plugin.getQuestManager().getDailyQuestById(aq.getQuestId());

            if (quest != null && quest.getType() == Quest.QuestType.GAIN_EXPERIENCE) {
                if (aq.isCompleted() || aq.getProgress() >= quest.getTarget()) continue;

                int newProgress = (int) Math.min(aq.getProgress() + amount, quest.getTarget());
                plugin.getQuestManager().updateProgress(player, aq, newProgress, isWeekly);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        lastBlockLocation.put(player.getUniqueId(), player.getLocation().getBlock().getLocation());
        plugin.getQuestManager().checkReset(player);
        plugin.getQuestManager().checkWeeklyReset(player);

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data != null) {
            if (data.getActiveQuests().isEmpty()) {
                List<Quest> newQuests = plugin.getQuestManager().getRandomDailyQuests(plugin.getGuiManager().getDailySlotCount());
                List<ActiveQuest> activeQuests = newQuests.stream()
                        .map(q -> new ActiveQuest(q.getId(), 0))
                        .collect(Collectors.toList());
                data.setActiveQuests(activeQuests);
            }
            if (data.getWeeklyQuests().isEmpty()) {
                List<Quest> newWeeklyQuests = plugin.getQuestManager().getRandomWeeklyQuests(getWeeklySlotCount());
                List<ActiveQuest> weeklyQuests = newWeeklyQuests.stream()
                        .map(q -> new ActiveQuest(q.getId(), 0))
                        .collect(Collectors.toList());
                data.setWeeklyQuests(weeklyQuests);
            }
            plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
        }
    }

    private int getWeeklySlotCount() {
        return plugin.getConfigManager().getQuestGuiWeekly().getIntegerList("gui.slots").size();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        lastBlockLocation.remove(playerId);
        accumulatedDistance.remove(playerId);
        walkQuestCache.remove(playerId);
        walkQuestCacheTime.remove(playerId);
    }

    // ==================== KILL_MOBS ====================
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(killer.getUniqueId());
        if (data == null) return;

        checkKillMobsQuest(killer, data.getActiveQuests(), event, false);
        checkKillMobsQuest(killer, data.getWeeklyQuests(), event, true);
    }

    private void checkKillMobsQuest(Player player, List<ActiveQuest> quests, EntityDeathEvent event, boolean isWeekly) {
        for (ActiveQuest aq : quests) {
            Quest quest = isWeekly ? plugin.getQuestManager().getWeeklyQuestById(aq.getQuestId())
                    : plugin.getQuestManager().getDailyQuestById(aq.getQuestId());
            if (quest != null && quest.getType() == Quest.QuestType.KILL_MOBS) {
                if (aq.isCompleted() || aq.getProgress() >= quest.getTarget()) continue;
                if (quest.getEntityType() == null || quest.getEntityType() == event.getEntity().getType()) {
                    int newProgress = aq.getProgress() + 1;
                    plugin.getQuestManager().updateProgress(player, aq, newProgress, isWeekly);
                }
            }
        }
    }

    // ==================== BREAK_BLOCKS ====================
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        Material blockType = event.getBlock().getType();

        checkBreakBlocksQuest(player, data.getActiveQuests(), blockType, false);
        checkBreakBlocksQuest(player, data.getWeeklyQuests(), blockType, true);
    }

    private void checkBreakBlocksQuest(Player player, List<ActiveQuest> quests, Material blockType, boolean isWeekly) {
        for (ActiveQuest aq : quests) {
            Quest quest = isWeekly ? plugin.getQuestManager().getWeeklyQuestById(aq.getQuestId())
                    : plugin.getQuestManager().getDailyQuestById(aq.getQuestId());
            if (quest != null && quest.getType() == Quest.QuestType.BREAK_BLOCKS) {
                if (aq.isCompleted() || aq.getProgress() >= quest.getTarget()) continue;
                if (quest.getBlockType() == null || quest.getBlockType() == blockType) {
                    int newProgress = aq.getProgress() + 1;
                    plugin.getQuestManager().updateProgress(player, aq, newProgress, isWeekly);
                }
            }
        }
    }

    // ==================== PLACE_BLOCKS ====================
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        Material blockType = event.getBlock().getType();

        checkPlaceBlocksQuest(player, data.getActiveQuests(), blockType, false);
        checkPlaceBlocksQuest(player, data.getWeeklyQuests(), blockType, true);
    }

    public void invalidateWalkCache(Player player) {
        UUID playerId = player.getUniqueId();
        walkQuestCache.remove(playerId);
        walkQuestCacheTime.remove(playerId);
    }

    private void checkPlaceBlocksQuest(Player player, List<ActiveQuest> quests, Material blockType, boolean isWeekly) {
        for (ActiveQuest aq : quests) {
            Quest quest = isWeekly ? plugin.getQuestManager().getWeeklyQuestById(aq.getQuestId())
                    : plugin.getQuestManager().getDailyQuestById(aq.getQuestId());
            if (quest != null && quest.getType() == Quest.QuestType.PLACE_BLOCKS) {
                if (aq.isCompleted() || aq.getProgress() >= quest.getTarget()) continue;
                if (quest.getBlockType() == null || quest.getBlockType() == blockType) {
                    int newProgress = aq.getProgress() + 1;
                    plugin.getQuestManager().updateProgress(player, aq, newProgress, isWeekly);
                }
            }
        }
    }

    // ==================== CATCH_FISH ====================
    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        checkCatchFishQuest(player, data.getActiveQuests(), false);
        checkCatchFishQuest(player, data.getWeeklyQuests(), true);
    }

    private void checkCatchFishQuest(Player player, List<ActiveQuest> quests, boolean isWeekly) {
        for (ActiveQuest aq : quests) {
            Quest quest = isWeekly ? plugin.getQuestManager().getWeeklyQuestById(aq.getQuestId())
                    : plugin.getQuestManager().getDailyQuestById(aq.getQuestId());
            if (quest != null && quest.getType() == Quest.QuestType.CATCH_FISH) {
                if (aq.isCompleted() || aq.getProgress() >= quest.getTarget()) continue;
                int newProgress = aq.getProgress() + 1;
                plugin.getQuestManager().updateProgress(player, aq, newProgress, isWeekly);
            }
        }
    }

    // ==================== EAT_ITEM ====================
    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        Material itemType = event.getItem().getType();

        checkEatItemQuest(player, data.getActiveQuests(), itemType, false);
        checkEatItemQuest(player, data.getWeeklyQuests(), itemType, true);
    }

    private void checkEatItemQuest(Player player, List<ActiveQuest> quests, Material itemType, boolean isWeekly) {
        for (ActiveQuest aq : quests) {
            Quest quest = isWeekly ? plugin.getQuestManager().getWeeklyQuestById(aq.getQuestId())
                    : plugin.getQuestManager().getDailyQuestById(aq.getQuestId());
            if (quest != null && quest.getType() == Quest.QuestType.EAT_ITEM) {
                if (aq.isCompleted() || aq.getProgress() >= quest.getTarget()) continue;
                if (quest.getItemType() == null || quest.getItemType() == itemType) {
                    int newProgress = aq.getProgress() + 1;
                    plugin.getQuestManager().updateProgress(player, aq, newProgress, isWeekly);
                }
            }
        }
    }

    // ==================== CRAFT_ITEM ====================
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        ItemStack result = event.getRecipe().getResult();
        Material craftedType = result.getType();
        int amount = result.getAmount();

        if (event.isShiftClick()) {
            amount *= calculateMaxCraftAmount(event);
        }

        checkCraftItemQuest(player, data.getActiveQuests(), craftedType, amount, false);
        checkCraftItemQuest(player, data.getWeeklyQuests(), craftedType, amount, true);
    }

    private void checkCraftItemQuest(Player player, List<ActiveQuest> quests, Material itemType, int amount, boolean isWeekly) {
        for (ActiveQuest aq : quests) {
            Quest quest = isWeekly ? plugin.getQuestManager().getWeeklyQuestById(aq.getQuestId())
                    : plugin.getQuestManager().getDailyQuestById(aq.getQuestId());
            if (quest != null && quest.getType() == Quest.QuestType.CRAFT_ITEM) {
                if (aq.isCompleted() || aq.getProgress() >= quest.getTarget()) continue;
                if (quest.getItemType() == null || quest.getItemType() == itemType) {
                    int newProgress = Math.min(aq.getProgress() + amount, quest.getTarget());
                    plugin.getQuestManager().updateProgress(player, aq, newProgress, isWeekly);
                }
            }
        }
    }

    private int calculateMaxCraftAmount(CraftItemEvent event) {
        ItemStack result = event.getRecipe().getResult();
        int resultAmount = result.getAmount();

        if (!event.isShiftClick()) {
            return resultAmount;
        }

        int maxCrafts = 64;
        Player player = (Player) event.getWhoClicked();
        org.bukkit.inventory.CraftingInventory craftInv = event.getInventory();
        ItemStack[] matrix = craftInv.getMatrix();

        for (ItemStack ingredient : matrix) {
            if (ingredient == null || ingredient.getType() == Material.AIR) continue;

            int available = 0;
            for (ItemStack invItem : player.getInventory().getContents()) {
                if (invItem != null && invItem.isSimilar(ingredient)) {
                    available += invItem.getAmount();
                }
            }

            int possible = available / ingredient.getAmount();
            maxCrafts = Math.min(maxCrafts, possible);
        }

        int freeSpace = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) {
                freeSpace += result.getMaxStackSize();
            } else if (item.isSimilar(result)) {
                freeSpace += result.getMaxStackSize() - item.getAmount();
            }
        }

        int maxBySpace = freeSpace / resultAmount;
        maxCrafts = Math.min(maxCrafts, maxBySpace);

        return Math.max(1, maxCrafts * resultAmount);
    }

    // ==================== SMELT_ITEMS ====================
    @EventHandler
    public void onFurnaceExtract(FurnaceExtractEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        Material itemType = event.getItemType();
        int amount = event.getItemAmount();

        checkSmeltItemsQuest(player, data.getActiveQuests(), itemType, amount, false);
        checkSmeltItemsQuest(player, data.getWeeklyQuests(), itemType, amount, true);
    }

    private void checkSmeltItemsQuest(Player player, List<ActiveQuest> quests, Material itemType, int amount, boolean isWeekly) {
        for (ActiveQuest aq : quests) {
            Quest quest = isWeekly ? plugin.getQuestManager().getWeeklyQuestById(aq.getQuestId())
                    : plugin.getQuestManager().getDailyQuestById(aq.getQuestId());
            if (quest != null && quest.getType() == Quest.QuestType.SMELT_ITEMS) {
                if (aq.isCompleted() || aq.getProgress() >= quest.getTarget()) continue;
                if (quest.getItemType() == null || quest.getItemType() == itemType) {
                    int newProgress = Math.min(aq.getProgress() + amount, quest.getTarget());
                    plugin.getQuestManager().updateProgress(player, aq, newProgress, isWeekly);
                }
            }
        }
    }

    // ==================== INTERACT_BLOCKS ====================
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        Material blockType = event.getClickedBlock().getType();
        if (!isInteractableBlock(blockType)) return;

        checkInteractBlocksQuest(player, data.getActiveQuests(), blockType, false);
        checkInteractBlocksQuest(player, data.getWeeklyQuests(), blockType, true);
    }

    private void checkInteractBlocksQuest(Player player, List<ActiveQuest> quests, Material blockType, boolean isWeekly) {
        for (ActiveQuest aq : quests) {
            Quest quest = isWeekly ? plugin.getQuestManager().getWeeklyQuestById(aq.getQuestId())
                    : plugin.getQuestManager().getDailyQuestById(aq.getQuestId());
            if (quest != null && quest.getType() == Quest.QuestType.INTERACT_BLOCKS) {
                if (aq.isCompleted() || aq.getProgress() >= quest.getTarget()) continue;
                if (quest.getBlockType() == null || quest.getBlockType() == blockType) {
                    int newProgress = aq.getProgress() + 1;
                    plugin.getQuestManager().updateProgress(player, aq, newProgress, isWeekly);
                }
            }
        }
    }

    private boolean isInteractableBlock(Material material) {
        return material == Material.CHEST ||
                material == Material.TRAPPED_CHEST ||
                material == Material.FURNACE ||
                material == Material.BLAST_FURNACE ||
                material == Material.SMOKER ||
                material == Material.CRAFTING_TABLE ||
                material == Material.ENCHANTING_TABLE ||
                material == Material.ANVIL ||
                material == Material.BREWING_STAND ||
                material == Material.BEACON ||
                material == Material.LOOM ||
                material == Material.STONECUTTER ||
                material == Material.GRINDSTONE ||
                material == Material.CARTOGRAPHY_TABLE ||
                material == Material.SMITHING_TABLE ||
                material.toString().contains("SHULKER_BOX") ||
                material.toString().contains("BED");
    }

    // ==================== TAKE_DAMAGE ====================
    @EventHandler
    public void onPlayerTakeDamage(PlayerMoveEvent event) { /* Оставлено для walk distance */ }

    // ==================== WALK_DISTANCE ====================
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null) return;

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        boolean hasWalkQuest = hasWalkQuestCached(player, data);
        if (!hasWalkQuest) return;

        Location from = event.getFrom();

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance < 0.1 || distance > 10) return;

        UUID playerId = player.getUniqueId();
        double totalDistance = accumulatedDistance.getOrDefault(playerId, 0.0) + distance;

        if (totalDistance >= 1.0) {
            int blocksToAdd = (int) totalDistance;
            accumulatedDistance.put(playerId, totalDistance - blocksToAdd);

            updateWalkDistanceQuests(player, data.getActiveQuests(), blocksToAdd, false);
            updateWalkDistanceQuests(player, data.getWeeklyQuests(), blocksToAdd, true);
        } else {
            accumulatedDistance.put(playerId, totalDistance);
        }
    }

    private boolean hasWalkQuestCached(Player player, PlayerData data) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        Long cachedTime = walkQuestCacheTime.get(playerId);
        if (cachedTime != null && (currentTime - cachedTime) < CACHE_DURATION) {
            Boolean cached = walkQuestCache.get(playerId);
            if (cached != null) {
                return cached;
            }
        }

        boolean result = hasWalkQuest(data.getActiveQuests(), false) ||
                hasWalkQuest(data.getWeeklyQuests(), true);

        walkQuestCache.put(playerId, result);
        walkQuestCacheTime.put(playerId, currentTime);

        return result;
    }

    private boolean hasWalkQuest(List<ActiveQuest> quests, boolean isWeekly) {
        for (ActiveQuest aq : quests) {
            Quest quest = isWeekly ? plugin.getQuestManager().getWeeklyQuestById(aq.getQuestId())
                    : plugin.getQuestManager().getDailyQuestById(aq.getQuestId());
            if (quest != null && quest.getType() == Quest.QuestType.WALK_DISTANCE) {
                if (!aq.isCompleted() && aq.getProgress() < quest.getTarget()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void updateWalkDistanceQuests(Player player, List<ActiveQuest> quests, int blocks, boolean isWeekly) {
        for (ActiveQuest aq : quests) {
            Quest quest = isWeekly ? plugin.getQuestManager().getWeeklyQuestById(aq.getQuestId())
                    : plugin.getQuestManager().getDailyQuestById(aq.getQuestId());
            if (quest != null && quest.getType() == Quest.QuestType.WALK_DISTANCE) {
                if (!aq.isCompleted() && aq.getProgress() < quest.getTarget()) {
                    int newProgress = Math.min(aq.getProgress() + blocks, quest.getTarget());
                    plugin.getQuestManager().updateProgress(player, aq, newProgress, isWeekly);
                }
            }
        }
    }

    public InternationalLevelHook getLevelHook() {
        return levelHook;
    }
}