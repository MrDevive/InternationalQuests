package com.example.internationalquests;

import com.example.internationalquests.commands.QuestCommand;
import com.example.internationalquests.listeners.QuestListener;
import com.example.internationalquests.managers.*;
import com.example.internationalquests.models.PlayerData;
import com.example.internationalquests.models.Quest;
import org.bukkit.plugin.java.JavaPlugin;

public class InternationalQuests extends JavaPlugin {

    private static InternationalQuests instance;
    private ConfigManager configManager;
    private QuestManager questManager;
    private PlayerDataManager playerDataManager;
    private GuiManager guiManager;
    private QuestListener questListener;

    @Override
    public void onEnable() {
        instance = this;

        try {
            configManager = new ConfigManager(this);
            configManager.loadAll();

            questManager = new QuestManager(this);
            playerDataManager = new PlayerDataManager(this);
            guiManager = new GuiManager(this);

            if (getCommand("quests") != null) {
                getCommand("quests").setExecutor(new QuestCommand(this));
            } else {
                getLogger().severe("Команда 'quests' не найдена в plugin.yml!");
            }

            questListener = new QuestListener(this);
            getServer().getPluginManager().registerEvents(questListener, this);
            getServer().getPluginManager().registerEvents(guiManager, this);

            // Только одна таска для PLAY_TIME квестов (раз в секунду)
            getServer().getScheduler().runTaskTimer(this, () -> {
                if (playerDataManager != null && questManager != null) {
                    getServer().getOnlinePlayers().forEach(player -> {
                        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
                        if (data != null) {
                            // Ежедневные квесты
                            data.getActiveQuests().forEach(activeQuest -> {
                                if (!activeQuest.isCompleted()) {
                                    Quest quest = questManager.getDailyQuestById(activeQuest.getQuestId());
                                    if (quest != null && quest.getType() == Quest.QuestType.PLAY_TIME) {
                                        int newProgress = activeQuest.getProgress() + 1;
                                        questManager.updateProgress(player, activeQuest, newProgress, false);
                                    }
                                }
                            });

                            // Еженедельные квесты
                            data.getWeeklyQuests().forEach(activeQuest -> {
                                if (!activeQuest.isCompleted()) {
                                    Quest quest = questManager.getWeeklyQuestById(activeQuest.getQuestId());
                                    if (quest != null && quest.getType() == Quest.QuestType.PLAY_TIME) {
                                        int newProgress = activeQuest.getProgress() + 1;
                                        questManager.updateProgress(player, activeQuest, newProgress, true);
                                    }
                                }
                            });
                        }
                    });
                }
            }, 20L, 20L);

            // Таска для проверки сброса квестов (раз в 5 секунд, не нагружаем)
            getServer().getScheduler().runTaskTimer(this, () -> {
                if (playerDataManager != null && questManager != null) {
                    getServer().getOnlinePlayers().forEach(player -> {
                        PlayerData data = playerDataManager.getPlayerData(player.getUniqueId());
                        if (data != null) {
                            long current = System.currentTimeMillis();

                            long lastDailyReset = data.getLastReset();
                            long dailyResetInterval = configManager.getResetTimeSeconds() * 1000L;
                            if (current - lastDailyReset >= dailyResetInterval) {
                                questManager.checkReset(player);
                            }

                            long lastWeeklyReset = data.getLastWeeklyReset();
                            long weeklyResetInterval = configManager.getWeeklyResetTimeSeconds() * 1000L;
                            if (current - lastWeeklyReset >= weeklyResetInterval) {
                                questManager.checkWeeklyReset(player);
                            }
                        }
                    });
                }
            }, 100L, 100L);

            getLogger().info("InternationalQuests успешно включён!");
            getLogger().info("Интеграция с InternationalLevel: " +
                    (questListener.getLevelHook() != null && questListener.getLevelHook().isEnabled() ? "✓" : "✗"));

        } catch (Exception e) {
            getLogger().severe("Ошибка при включении плагина: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }
        getLogger().info("InternationalQuests выключен!");
    }

    public static InternationalQuests getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public QuestListener getQuestListener() {
        return questListener;
    }
}