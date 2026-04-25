package com.example.internationalquests.managers;

import com.example.internationalquests.InternationalQuests;
import com.example.internationalquests.models.ActiveQuest;
import com.example.internationalquests.models.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final InternationalQuests plugin;
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    public PlayerDataManager(InternationalQuests plugin) {
        this.plugin = plugin;
        loadAll();
    }

    public void loadAll() {
        playerDataMap.clear();
        FileConfiguration dataConfig = plugin.getConfigManager().getPlayerData();
        ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
        if (playersSection == null) return;

        for (String uuidStr : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection pSec = playersSection.getConfigurationSection(uuidStr);
                if (pSec == null) continue;

                long lastReset = pSec.getLong("last_reset", 0);
                long lastWeeklyReset = pSec.getLong("last_weekly_reset", 0);
                long lastSwapReset = pSec.getLong("last_swap_reset", 0);
                int swapsUsed = pSec.getInt("swaps_used", 0);

                List<ActiveQuest> activeQuests = loadQuestsFromConfig(pSec, "active_quests");
                List<ActiveQuest> weeklyQuests = loadQuestsFromConfig(pSec, "weekly_quests");

                PlayerData data = new PlayerData(uuid);
                data.setLastReset(lastReset);
                data.setLastWeeklyReset(lastWeeklyReset);
                data.setLastSwapReset(lastSwapReset);
                data.setSwapsUsed(swapsUsed);
                data.setActiveQuests(activeQuests);
                data.setWeeklyQuests(weeklyQuests);
                playerDataMap.put(uuid, data);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Неверный UUID в player_data.yml: " + uuidStr);
            }
        }
    }

    private List<ActiveQuest> loadQuestsFromConfig(ConfigurationSection pSec, String sectionName) {
        List<ActiveQuest> quests = new ArrayList<>();
        ConfigurationSection questsSec = pSec.getConfigurationSection(sectionName);
        if (questsSec != null) {
            for (String questId : questsSec.getKeys(false)) {
                int progress = questsSec.getInt(questId + ".progress", 0);
                boolean completed = questsSec.getBoolean(questId + ".completed", false);
                long completionTime = questsSec.getLong(questId + ".completion_time", 0);

                ActiveQuest aq = new ActiveQuest(questId, progress);
                aq.setCompleted(completed);
                aq.setCompletionTime(completionTime);
                quests.add(aq);
            }
        }
        return quests;
    }

    public void saveAll() {
        FileConfiguration dataConfig = plugin.getConfigManager().getPlayerData();
        dataConfig.set("players", null);

        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerData data = entry.getValue();
            String path = "players." + uuid.toString();
            dataConfig.set(path + ".last_reset", data.getLastReset());
            dataConfig.set(path + ".last_weekly_reset", data.getLastWeeklyReset());
            dataConfig.set(path + ".last_swap_reset", data.getLastSwapReset());
            dataConfig.set(path + ".swaps_used", data.getSwapsUsed());

            saveQuestsToConfig(dataConfig, path + ".active_quests", data.getActiveQuests());
            saveQuestsToConfig(dataConfig, path + ".weekly_quests", data.getWeeklyQuests());
        }
        plugin.getConfigManager().savePlayerData();
    }

    private void saveQuestsToConfig(FileConfiguration dataConfig, String path, List<ActiveQuest> quests) {
        for (ActiveQuest aq : quests) {
            dataConfig.set(path + "." + aq.getQuestId() + ".progress", aq.getProgress());
            dataConfig.set(path + "." + aq.getQuestId() + ".completed", aq.isCompleted());
            dataConfig.set(path + "." + aq.getQuestId() + ".completion_time", aq.getCompletionTime());
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) {
            data = new PlayerData(uuid);
            playerDataMap.put(uuid, data);
        }
        return data;
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) return;

        FileConfiguration dataConfig = plugin.getConfigManager().getPlayerData();
        String path = "players." + uuid.toString();
        dataConfig.set(path + ".last_reset", data.getLastReset());
        dataConfig.set(path + ".last_weekly_reset", data.getLastWeeklyReset());
        dataConfig.set(path + ".last_swap_reset", data.getLastSwapReset());
        dataConfig.set(path + ".swaps_used", data.getSwapsUsed());

        dataConfig.set(path + ".active_quests", null);
        for (ActiveQuest aq : data.getActiveQuests()) {
            dataConfig.set(path + ".active_quests." + aq.getQuestId() + ".progress", aq.getProgress());
        }

        dataConfig.set(path + ".weekly_quests", null);
        for (ActiveQuest aq : data.getWeeklyQuests()) {
            dataConfig.set(path + ".weekly_quests." + aq.getQuestId() + ".progress", aq.getProgress());
        }

        plugin.getConfigManager().savePlayerData();
    }

    public void setPlayerData(UUID uuid, PlayerData data) {
        playerDataMap.put(uuid, data);
        savePlayerData(uuid);
    }
}