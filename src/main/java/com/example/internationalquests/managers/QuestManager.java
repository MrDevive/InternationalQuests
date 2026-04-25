package com.example.internationalquests.managers;

import com.example.internationalquests.InternationalQuests;
import com.example.internationalquests.listeners.QuestListener;
import com.example.internationalquests.models.ActiveQuest;
import com.example.internationalquests.models.PlayerData;
import com.example.internationalquests.models.Quest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.ChatColor;

import java.util.*;
import java.util.stream.Collectors;

public class QuestManager {

    private final InternationalQuests plugin;
    private final Map<String, Quest> dailyQuests = new HashMap<>();
    private final Map<String, Quest> weeklyQuests = new HashMap<>();

    public QuestManager(InternationalQuests plugin) {
        this.plugin = plugin;
        reloadQuests();
    }

    public void reloadQuests() {
        dailyQuests.clear();
        weeklyQuests.clear();

        // Загрузка ежедневных квестов
        dailyQuests.putAll(plugin.getConfigManager().getDailyQuests().getQuests());

        // Загрузка еженедельных квестов
        weeklyQuests.putAll(plugin.getConfigManager().getWeeklyQuests().getQuests());

        plugin.getLogger().info("Загружено " + dailyQuests.size() + " ежедневных и " +
                weeklyQuests.size() + " еженедельных квестов.");
    }

    public Quest getDailyQuestById(String id) {
        return dailyQuests.get(id);
    }

    public Quest getWeeklyQuestById(String id) {
        return weeklyQuests.get(id);
    }

    // Для обратной совместимости
    public Quest getQuestById(String id) {
        Quest quest = dailyQuests.get(id);
        if (quest == null) {
            quest = weeklyQuests.get(id);
        }
        return quest;
    }

    public List<Quest> getRandomDailyQuests(int count) {
        return getRandomQuestsFromMap(dailyQuests, count);
    }

    public List<Quest> getRandomWeeklyQuests(int count) {
        return getRandomQuestsFromMap(weeklyQuests, count);
    }

    // Для обратной совместимости
    public List<Quest> getRandomQuests(int count) {
        return getRandomDailyQuests(count);
    }

    private List<Quest> getRandomQuestsFromMap(Map<String, Quest> questMap, int count) {
        List<Quest> available = new ArrayList<>(questMap.values());
        List<Quest> selected = new ArrayList<>();
        Random random = new Random();

        while (selected.size() < count && !available.isEmpty()) {
            double totalWeight = available.stream().mapToDouble(Quest::getChance).sum();
            double rand = random.nextDouble() * totalWeight;
            double accumulator = 0;
            Iterator<Quest> iterator = available.iterator();
            Quest chosen = null;
            while (iterator.hasNext()) {
                Quest q = iterator.next();
                accumulator += q.getChance();
                if (rand <= accumulator) {
                    chosen = q;
                    iterator.remove();
                    break;
                }
            }
            if (chosen != null) {
                selected.add(chosen);
            } else {
                chosen = available.remove(0);
                selected.add(chosen);
            }
        }
        return selected;
    }

    public boolean swapQuest(Player player, String oldQuestId, boolean isWeekly) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());

        plugin.getQuestListener().invalidateWalkCache(player);

        // Проверяем счётчик смен (только для ежедневных квестов)
        if (!isWeekly) {
            long current = System.currentTimeMillis();
            long lastSwapReset = data.getLastSwapReset();
            long swapResetInterval = plugin.getConfigManager().getResetSwapsTimeSeconds() * 1000L;

            if (current - lastSwapReset >= swapResetInterval) {
                data.setSwapsUsed(0);
                data.setLastSwapReset(current);
            }

            int maxSwaps = plugin.getConfigManager().getMaxSwaps();
            if (data.getSwapsUsed() >= maxSwaps) {
                player.sendMessage(plugin.getConfigManager().getMessage("no-swaps-left"));
                return false;
            }
        }

        List<ActiveQuest> questList = isWeekly ? data.getWeeklyQuests() : data.getActiveQuests();

        // Находим квест для замены
        ActiveQuest toRemove = null;
        for (ActiveQuest aq : questList) {
            if (aq.getQuestId().equals(oldQuestId)) {
                toRemove = aq;
                break;
            }
        }

        if (toRemove == null) return false;

        Quest oldQuest = isWeekly ? getWeeklyQuestById(toRemove.getQuestId()) : getDailyQuestById(toRemove.getQuestId());
        if (oldQuest != null && toRemove.getProgress() >= oldQuest.getTarget()) {
            player.sendMessage(plugin.getConfigManager().getMessage("cannot-swap-completed"));
            return false;
        }

        // Получаем список доступных квестов для замены
        Map<String, Quest> questMap = isWeekly ? weeklyQuests : dailyQuests;
        List<String> currentQuestIds = questList.stream()
                .map(ActiveQuest::getQuestId)
                .collect(Collectors.toList());

        List<Quest> availableQuests = questMap.values().stream()
                .filter(q -> !currentQuestIds.contains(q.getId()))
                .collect(Collectors.toList());

        if (availableQuests.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-available-quests"));
            return false;
        }

        // Взвешенный выбор нового квеста
        double totalWeight = availableQuests.stream().mapToDouble(Quest::getChance).sum();
        double rand = new Random().nextDouble() * totalWeight;
        double accumulator = 0;
        Quest newQuest = null;
        for (Quest q : availableQuests) {
            accumulator += q.getChance();
            if (rand <= accumulator) {
                newQuest = q;
                break;
            }
        }
        if (newQuest == null) {
            newQuest = availableQuests.get(0);
        }

        // Заменяем квест
        questList.remove(toRemove);
        questList.add(new ActiveQuest(newQuest.getId(), 0));

        if (!isWeekly) {
            data.setSwapsUsed(data.getSwapsUsed() + 1);
        }
        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());

        String oldQuestName = oldQuest != null ? oldQuest.getName() : "квест";
        String newQuestName = newQuest.getName();

        String message = plugin.getConfigManager().getMessage("quest-swapped")
                .replace("{old}", oldQuestName)
                .replace("{new}", newQuestName);
        player.sendMessage(message);

        return true;
    }

    // Для обратной совместимости
    public boolean swapQuest(Player player, String oldQuestId) {
        return swapQuest(player, oldQuestId, false);
    }

    public void updateProgress(Player player, ActiveQuest activeQuest, int newProgress, boolean isWeekly) {
        Quest quest = isWeekly ? getWeeklyQuestById(activeQuest.getQuestId()) : getDailyQuestById(activeQuest.getQuestId());
        if (quest == null) return;

        boolean wasCompleted = activeQuest.getProgress() >= quest.getTarget();

        if (newProgress >= quest.getTarget()) {
            if (!wasCompleted) {
                activeQuest.setProgress(quest.getTarget());
                completeQuest(player, activeQuest, isWeekly);
            }
        } else {
            activeQuest.setProgress(newProgress);
            plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
        }
    }

    // Для обратной совместимости
    public void updateProgress(Player player, ActiveQuest activeQuest, int newProgress) {
        updateProgress(player, activeQuest, newProgress, false);
    }

    private void completeQuest(Player player, ActiveQuest activeQuest, boolean isWeekly) {
        Quest quest = isWeekly ? getWeeklyQuestById(activeQuest.getQuestId()) : getDailyQuestById(activeQuest.getQuestId());
        if (quest != null && quest.getType() == Quest.QuestType.WALK_DISTANCE) {
            plugin.getQuestListener().invalidateWalkCache(player);
        }

        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());

        // Выдаем награду
        for (String cmd : quest.getRewardCommands()) {
            String parsedCmd = cmd.replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCmd);
        }

        activeQuest.setProgress(quest.getTarget());
        activeQuest.setCompleted(true);
        activeQuest.setCompletionTime(System.currentTimeMillis());
        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());

        String message = plugin.getConfigManager().getMessage("quest-complete")
                .replace("{quest}", quest.getName());
        player.sendMessage(message);

        // Обновляем GUI если открыт
        plugin.getGuiManager().updateOpenGui(player);
    }

    public boolean checkReset(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        long current = System.currentTimeMillis();
        long lastReset = data.getLastReset();
        long resetInterval = plugin.getConfigManager().getResetTimeSeconds() * 1000L;

        // Если ежедневных квестов нет - выдаем новые
        if (data.getActiveQuests().isEmpty()) {
            List<Quest> newQuests = getRandomDailyQuests(getDailySlotCount());
            List<ActiveQuest> activeQuests = newQuests.stream()
                    .map(q -> new ActiveQuest(q.getId(), 0))
                    .collect(Collectors.toList());
            data.setActiveQuests(activeQuests);
            data.setLastReset(current);
            data.setSwapsUsed(0);
            data.setLastSwapReset(current);
            plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());

            player.sendMessage(plugin.getConfigManager().getMessage("quests-given"));
            return true;
        }

        // Если время полного сброса наступило - сбрасываем ТОЛЬКО выполненные квесты
        if (current - lastReset >= resetInterval) {
            resetAllDailyQuests(player, data, current);
            return true;
        }

        return false;
    }

    public boolean checkWeeklyReset(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return false;

        long current = System.currentTimeMillis();
        long lastWeeklyReset = data.getLastWeeklyReset();
        long weeklyResetInterval = plugin.getConfigManager().getWeeklyResetTimeSeconds() * 1000L;

        // Если еженедельных квестов нет - выдаем новые
        if (data.getWeeklyQuests().isEmpty()) {
            List<Quest> newQuests = getRandomWeeklyQuests(getWeeklySlotCount());
            List<ActiveQuest> weeklyQuests = newQuests.stream()
                    .map(q -> new ActiveQuest(q.getId(), 0))
                    .collect(Collectors.toList());
            data.setWeeklyQuests(weeklyQuests);
            data.setLastWeeklyReset(current);
            plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());

            player.sendMessage(plugin.getConfigManager().getMessage("weekly-quests-given"));
            return true;
        }

        // Если время сброса наступило - сбрасываем ВСЕ еженедельные квесты
        if (current - lastWeeklyReset >= weeklyResetInterval) {
            resetAllWeeklyQuests(player, data, current);
            return true;
        }

        return false;
    }

    private void resetAllDailyQuests(Player player, PlayerData data, long current) {
        List<ActiveQuest> activeQuests = data.getActiveQuests();
        boolean hasChanges = false;
        int replacedCount = 0;

        List<Integer> completedIndexes = new ArrayList<>();
        for (int i = 0; i < activeQuests.size(); i++) {
            ActiveQuest aq = activeQuests.get(i);
            Quest quest = getDailyQuestById(aq.getQuestId());

            if (quest != null && aq.isCompleted()) {
                completedIndexes.add(i);
            }
        }

        if (!completedIndexes.isEmpty()) {
            for (int i : completedIndexes) {
                ActiveQuest aq = activeQuests.get(i);
                Quest oldQuest = getDailyQuestById(aq.getQuestId());

                List<Quest> availableQuests = getAvailableQuestsForReset(data, false);
                if (!availableQuests.isEmpty()) {
                    Quest newQuest = selectRandomQuestFromList(availableQuests);
                    if (newQuest != null) {
                        activeQuests.set(i, new ActiveQuest(newQuest.getId(), 0));
                        hasChanges = true;
                        replacedCount++;

                        if (oldQuest != null) {
                            String message = plugin.getConfigManager().getMessage("quest-auto-replaced")
                                    .replace("{completed}", oldQuest.getName())
                                    .replace("{new}", newQuest.getName());
                            player.sendMessage(message);
                        }
                    }
                }
            }
        }

        data.setLastReset(current);
        data.setSwapsUsed(0);
        data.setLastSwapReset(current);
        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());

        if (hasChanges) {
            player.sendMessage(plugin.getConfigManager().getMessage("quests-reset-partial")
                    .replace("{count}", String.valueOf(replacedCount)));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("quests-reset-timer"));
        }

        plugin.getGuiManager().updateOpenGui(player);
    }

    private void resetAllWeeklyQuests(Player player, PlayerData data, long current) {
        List<Quest> newQuests = getRandomWeeklyQuests(getWeeklySlotCount());
        List<ActiveQuest> weeklyQuests = newQuests.stream()
                .map(q -> new ActiveQuest(q.getId(), 0))
                .collect(Collectors.toList());

        data.setWeeklyQuests(weeklyQuests);
        data.setLastWeeklyReset(current);
        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());

        player.sendMessage(plugin.getConfigManager().getMessage("weekly-quests-reset"));

        plugin.getGuiManager().updateOpenGui(player);
    }

    private List<Quest> getAvailableQuestsForReset(PlayerData data, boolean isWeekly) {
        List<ActiveQuest> questList = isWeekly ? data.getWeeklyQuests() : data.getActiveQuests();
        Map<String, Quest> questMap = isWeekly ? weeklyQuests : dailyQuests;

        Set<String> activeUncompletedQuestIds = questList.stream()
                .filter(aq -> !aq.isCompleted())
                .map(ActiveQuest::getQuestId)
                .collect(Collectors.toSet());

        List<Quest> available = questMap.values().stream()
                .filter(q -> !activeUncompletedQuestIds.contains(q.getId()))
                .collect(Collectors.toList());

        if (available.isEmpty()) {
            available = new ArrayList<>(questMap.values());
        }

        return available;
    }

    private Quest selectRandomQuestFromList(List<Quest> questList) {
        if (questList.isEmpty()) return null;

        double totalWeight = questList.stream().mapToDouble(Quest::getChance).sum();
        double rand = new Random().nextDouble() * totalWeight;
        double accumulator = 0;

        for (Quest q : questList) {
            accumulator += q.getChance();
            if (rand <= accumulator) {
                return q;
            }
        }

        return questList.get(0);
    }

    private int getDailySlotCount() {
        return plugin.getConfigManager().getQuestGuiDaily().getIntegerList("gui.slots").size();
    }

    private int getWeeklySlotCount() {
        return plugin.getConfigManager().getQuestGuiWeekly().getIntegerList("gui.slots").size();
    }

    public String formatTime(long seconds) {
        if (seconds <= 0) return plugin.getConfigManager().getMessage("time-format-zero");

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return plugin.getConfigManager().getMessage("time-format-hours")
                    .replace("{hours}", String.valueOf(hours))
                    .replace("{minutes}", String.valueOf(minutes))
                    .replace("{seconds}", String.valueOf(secs));
        } else if (minutes > 0) {
            return plugin.getConfigManager().getMessage("time-format-minutes")
                    .replace("{minutes}", String.valueOf(minutes))
                    .replace("{seconds}", String.valueOf(secs));
        } else {
            return plugin.getConfigManager().getMessage("time-format-seconds")
                    .replace("{seconds}", String.valueOf(secs));
        }
    }

    public void checkResetAndReplace(Player player) {
        checkReset(player);
        checkWeeklyReset(player);
    }

    public QuestListener getQuestListener() {
        return plugin.getQuestListener();
    }
}