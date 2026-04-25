package com.example.internationalquests.commands;

import com.example.internationalquests.InternationalQuests;
import com.example.internationalquests.managers.GuiManager;
import com.example.internationalquests.models.ActiveQuest;
import com.example.internationalquests.models.PlayerData;
import com.example.internationalquests.models.Quest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class QuestCommand implements CommandExecutor {

    private final InternationalQuests plugin;

    public QuestCommand(InternationalQuests plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                plugin.getGuiManager().openMainMenu(player);
            } else {
                sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("internationalquests.reload")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }
            plugin.getConfigManager().reloadAll();
            sender.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
            return true;
        }

        // Команда сброса квестов
        if (args[0].equalsIgnoreCase("reset")) {
            // Определяем тип сбрасываемых квестов
            String type = "all"; // daily, weekly, all
            String targetName = null;

            if (args.length >= 2) {
                if (args[1].equalsIgnoreCase("daily") || args[1].equalsIgnoreCase("weekly") || args[1].equalsIgnoreCase("all")) {
                    type = args[1].toLowerCase();
                    if (args.length >= 3) {
                        targetName = args[2];
                    }
                } else {
                    targetName = args[1];
                }
            }

            if (targetName == null) {
                // Сброс своих квестов
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cИспользование: /quests reset [daily|weekly|all] [игрок]");
                    return true;
                }

                if (!sender.hasPermission("internationalquests.reset.self")) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }

                Player player = (Player) sender;
                resetPlayerQuests(player, type);
                sender.sendMessage("§aВаши " + getTypeName(type) + " квесты были сброшены!");
                return true;
            }

            // Сброс квестов другого игрока
            if (!sender.hasPermission("internationalquests.reset.others")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }

            Player target = plugin.getServer().getPlayer(targetName);
            if (target == null) {
                sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
                return true;
            }

            resetPlayerQuests(target, type);
            sender.sendMessage("§a" + getTypeName(type) + " квесты игрока " + target.getName() + " были сброшены!");
            target.sendMessage("§aВаши " + getTypeName(type) + " квесты были сброшены администратором!");
            return true;
        }

        sender.sendMessage(plugin.getConfigManager().getMessage("invalid-usage"));
        return true;
    }

    private String getTypeName(String type) {
        switch (type) {
            case "daily": return "ежедневные";
            case "weekly": return "еженедельные";
            default: return "";
        }
    }

    private void resetPlayerQuests(Player player, String type) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        long currentTime = System.currentTimeMillis();

        // Сброс ежедневных квестов
        if (type.equals("daily") || type.equals("all")) {
            List<Quest> newDailyQuests = plugin.getQuestManager().getRandomDailyQuests(plugin.getGuiManager().getDailySlotCount());
            List<ActiveQuest> dailyActiveQuests = newDailyQuests.stream()
                    .map(q -> new ActiveQuest(q.getId(), 0))
                    .collect(Collectors.toList());
            data.setActiveQuests(dailyActiveQuests);
            data.setLastReset(currentTime);
            data.setSwapsUsed(0);
            data.setLastSwapReset(currentTime);
        }

        // Сброс еженедельных квестов
        if (type.equals("weekly") || type.equals("all")) {
            int weeklySlotCount = plugin.getConfigManager().getQuestGuiWeekly().getIntegerList("gui.slots").size();
            List<Quest> newWeeklyQuests = plugin.getQuestManager().getRandomWeeklyQuests(weeklySlotCount);
            List<ActiveQuest> weeklyActiveQuests = newWeeklyQuests.stream()
                    .map(q -> new ActiveQuest(q.getId(), 0))
                    .collect(Collectors.toList());
            data.setWeeklyQuests(weeklyActiveQuests);
            data.setLastWeeklyReset(currentTime);
        }

        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());

        // Обновляем GUI если открыт
        plugin.getGuiManager().updateOpenGui(player);
    }
}