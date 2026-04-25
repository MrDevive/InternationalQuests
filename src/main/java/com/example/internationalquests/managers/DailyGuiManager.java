package com.example.internationalquests.managers;

import com.example.internationalquests.InternationalQuests;
import com.example.internationalquests.config.BaseConfig;
import com.example.internationalquests.models.ActiveQuest;
import com.example.internationalquests.models.PlayerData;
import com.example.internationalquests.models.Quest;
import com.example.internationalquests.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DailyGuiManager extends BaseGuiManager {

    public DailyGuiManager(InternationalQuests plugin) {
        super(plugin);
        loadConfig();
    }

    @Override
    protected void loadConfig() {
        loadCommonConfig(plugin.getConfigManager().getQuestGuiDaily());
    }

    @Override
    protected List<ActiveQuest> getQuests(PlayerData data) {
        return data.getActiveQuests();
    }

    @Override
    protected Quest getQuestById(String id) {
        return plugin.getQuestManager().getDailyQuestById(id);
    }

    @Override
    protected boolean isWeekly() {
        return false;
    }

    @Override
    protected Inventory createHolder() {
        return Bukkit.createInventory(new DailyGuiHolder(), size, title);
    }

    @Override
    protected BaseConfig getQuestConfig() {
        return plugin.getConfigManager().getDailyQuests();
    }

    @Override
    protected ItemStack createInfoItem(PlayerData data) {
        long current = System.currentTimeMillis();
        long swapResetInterval = plugin.getConfigManager().getResetSwapsTimeSeconds() * 1000L;
        long timeSinceSwapReset = current - data.getLastSwapReset();
        long timeUntilSwapReset = Math.max(0, swapResetInterval - timeSinceSwapReset);

        int swapsLeft = plugin.getConfigManager().getMaxSwaps() - data.getSwapsUsed();
        int maxSwaps = plugin.getConfigManager().getMaxSwaps();

        long resetInterval = plugin.getConfigManager().getResetTimeSeconds() * 1000L;
        long timeSinceReset = current - data.getLastReset();
        long timeUntilReset = Math.max(0, resetInterval - timeSinceReset);

        List<String> infoLore = new ArrayList<>();
        String swapsInfo = plugin.getConfigManager().getMessage("gui-info-swaps")
                .replace("{current}", String.valueOf(swapsLeft))
                .replace("{max}", String.valueOf(maxSwaps));
        infoLore.add(swapsInfo);

        if (timeUntilSwapReset > 0) {
            String swapsResetInfo = plugin.getConfigManager().getMessage("gui-info-swaps-reset")
                    .replace("{time}", plugin.getQuestManager().formatTime(timeUntilSwapReset / 1000));
            infoLore.add(swapsResetInfo);
        }
        infoLore.add("");
        infoLore.add(plugin.getConfigManager().getMessage("gui-info-quests-reset"));
        infoLore.add(ChatColor.WHITE + plugin.getQuestManager().formatTime(timeUntilReset / 1000));

        return ItemBuilder.of(Material.CLOCK)
                .name(plugin.getConfigManager().getMessage("gui-info-title"))
                .lore(infoLore)
                .build();
    }

    @Override
    protected void addExtraLore(List<String> lore, Quest quest, ActiveQuest aq, Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        long current = System.currentTimeMillis();
        long swapResetInterval = plugin.getConfigManager().getResetSwapsTimeSeconds() * 1000L;
        long timeSinceSwapReset = current - data.getLastSwapReset();
        long timeUntilSwapReset = Math.max(0, swapResetInterval - timeSinceSwapReset);

        int swapsLeft = plugin.getConfigManager().getMaxSwaps() - data.getSwapsUsed();
        int maxSwaps = plugin.getConfigManager().getMaxSwaps();

        boolean completed = aq.getProgress() >= quest.getTarget() || aq.isCompleted();

        lore.add("");
        if (!completed) {
            if (swapsLeft > 0) {
                lore.add(plugin.getConfigManager().getMessage("gui-swap-hint"));
                String swapsLeftFormat = plugin.getConfigManager().getMessage("gui-swaps-left")
                        .replace("{current}", String.valueOf(swapsLeft))
                        .replace("{max}", String.valueOf(maxSwaps));
                lore.add(swapsLeftFormat);
            } else {
                lore.add(plugin.getConfigManager().getMessage("gui-no-swaps"));
                if (timeUntilSwapReset > 0) {
                    String resetTimeFormat = plugin.getConfigManager().getMessage("gui-swaps-reset")
                            .replace("{time}", plugin.getQuestManager().formatTime(timeUntilSwapReset / 1000));
                    lore.add(resetTimeFormat);
                }
            }
        } else {
            lore.add(plugin.getConfigManager().getMessage("gui-cannot-swap"));
        }
    }

    @Override
    protected void handleQuestClick(Player player, int slot) {
        int index = slots.indexOf(slot);
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        if (index >= data.getActiveQuests().size()) return;

        ActiveQuest aq = data.getActiveQuests().get(index);
        Quest quest = getQuestById(aq.getQuestId());
        if (quest == null) return;

        if (aq.getProgress() >= quest.getTarget() || aq.isCompleted()) {
            player.sendMessage(plugin.getConfigManager().getMessage("cannot-swap-completed"));
            return;
        }

        if (plugin.getQuestManager().swapQuest(player, aq.getQuestId(), false)) {
            openGui(player);
        }
    }

    public static class DailyGuiHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}