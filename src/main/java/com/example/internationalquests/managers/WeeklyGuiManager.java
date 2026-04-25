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

public class WeeklyGuiManager extends BaseGuiManager {

    public WeeklyGuiManager(InternationalQuests plugin) {
        super(plugin);
        loadConfig();
    }

    @Override
    protected void loadConfig() {
        loadCommonConfig(plugin.getConfigManager().getQuestGuiWeekly());
    }

    @Override
    protected List<ActiveQuest> getQuests(PlayerData data) {
        return data.getWeeklyQuests();
    }

    @Override
    protected Quest getQuestById(String id) {
        return plugin.getQuestManager().getWeeklyQuestById(id);
    }

    @Override
    protected boolean isWeekly() {
        return true;
    }

    @Override
    protected Inventory createHolder() {
        return Bukkit.createInventory(new WeeklyGuiHolder(), size, title);
    }

    @Override
    protected BaseConfig getQuestConfig() {
        return plugin.getConfigManager().getWeeklyQuests();
    }

    @Override
    protected ItemStack createInfoItem(PlayerData data) {
        long current = System.currentTimeMillis();
        long weeklyResetInterval = plugin.getConfigManager().getWeeklyResetTimeSeconds() * 1000L;
        long timeSinceWeeklyReset = current - data.getLastWeeklyReset();
        long timeUntilWeeklyReset = Math.max(0, weeklyResetInterval - timeSinceWeeklyReset);

        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.YELLOW + "До сброса еженедельных квестов:");
        infoLore.add(ChatColor.WHITE + plugin.getQuestManager().formatTime(timeUntilWeeklyReset / 1000));

        return ItemBuilder.of(Material.ENDER_EYE)
                .name(ChatColor.LIGHT_PURPLE + "Информация")
                .lore(infoLore)
                .build();
    }

    @Override
    protected void addExtraLore(List<String> lore, Quest quest, ActiveQuest aq, Player player) {
        lore.add("");
        lore.add(ChatColor.LIGHT_PURPLE + "Еженедельный квест");
    }

    @Override
    protected void handleQuestClick(Player player, int slot) {
        // Еженедельные квесты нельзя сменить
    }

    public static class WeeklyGuiHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}