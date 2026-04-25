package com.example.internationalquests.managers;

import com.example.internationalquests.InternationalQuests;
import com.example.internationalquests.config.DailyQuestsConfig;
import com.example.internationalquests.config.WeeklyQuestsConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final InternationalQuests plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private DailyQuestsConfig dailyQuests;
    private WeeklyQuestsConfig weeklyQuests;
    private FileConfiguration questGui;
    private FileConfiguration questGuiDaily;
    private FileConfiguration questGuiWeekly;
    private FileConfiguration playerData;

    private long resetTimeSeconds;
    private int maxSwaps;
    private long resetSwapsTimeSeconds;
    private long weeklyResetTimeSeconds;

    public ConfigManager(InternationalQuests plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        createDataFolder();
        loadConfig();
        loadMessages();
        dailyQuests = new DailyQuestsConfig(plugin);
        weeklyQuests = new WeeklyQuestsConfig(plugin);
        loadQuestGui();
        loadQuestGuiDaily();
        loadQuestGuiWeekly();
        loadPlayerData();
    }

    public void reloadAll() {
        loadAll();
        if (plugin.getQuestManager() != null) {
            plugin.getQuestManager().reloadQuests();
        }
        if (plugin.getGuiManager() != null) {
            plugin.getGuiManager().reload();
        }
    }

    private void createDataFolder() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
    }

    private void loadConfig() {
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        resetTimeSeconds = config.getLong("reset-time", 86400);
        weeklyResetTimeSeconds = config.getLong("weekly-reset-time", 604800);
        maxSwaps = config.getInt("quest-swap.max-swaps", 3);
        resetSwapsTimeSeconds = config.getLong("quest-swap.reset-swaps-time", 86400);
    }

    private void loadMessages() {
        File file = new File(plugin.getDataFolder(), "message.yml");
        if (!file.exists()) {
            plugin.saveResource("message.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    private void loadQuestGui() {
        File file = new File(plugin.getDataFolder(), "quest_gui.yml");
        if (!file.exists()) {
            plugin.saveResource("quest_gui.yml", false);
        }
        questGui = YamlConfiguration.loadConfiguration(file);
    }

    private void loadQuestGuiDaily() {
        File file = new File(plugin.getDataFolder(), "quest_gui_daily.yml");
        if (!file.exists()) {
            plugin.saveResource("quest_gui_daily.yml", false);
        }
        questGuiDaily = YamlConfiguration.loadConfiguration(file);
    }

    private void loadQuestGuiWeekly() {
        File file = new File(plugin.getDataFolder(), "quest_gui_weekly.yml");
        if (!file.exists()) {
            plugin.saveResource("quest_gui_weekly.yml", false);
        }
        questGuiWeekly = YamlConfiguration.loadConfiguration(file);
    }

    private void loadPlayerData() {
        File file = new File(plugin.getDataFolder(), "player_data.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать player_data.yml: " + e.getMessage());
            }
        }
        playerData = YamlConfiguration.loadConfiguration(file);
    }

    public void savePlayerData() {
        if (playerData == null) return;
        File file = new File(plugin.getDataFolder(), "player_data.yml");
        try {
            playerData.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить player_data.yml: " + e.getMessage());
        }
    }

    public FileConfiguration getMessages() { return messages; }
    public DailyQuestsConfig getDailyQuests() { return dailyQuests; }
    public WeeklyQuestsConfig getWeeklyQuests() { return weeklyQuests; }
    public FileConfiguration getQuestGui() { return questGui; }
    public FileConfiguration getQuestGuiDaily() { return questGuiDaily; }
    public FileConfiguration getQuestGuiWeekly() { return questGuiWeekly; }
    public FileConfiguration getPlayerData() { return playerData; }
    public long getResetTimeSeconds() { return resetTimeSeconds; }
    public long getWeeklyResetTimeSeconds() { return weeklyResetTimeSeconds; }
    public int getMaxSwaps() { return maxSwaps; }
    public long getResetSwapsTimeSeconds() { return resetSwapsTimeSeconds; }

    public String getMessage(String key) {
        if (messages == null) return "§cСообщение не найдено: " + key;
        String message = messages.getString("messages." + key);
        if (message == null) return "§cСообщение не найдено: " + key;
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
}