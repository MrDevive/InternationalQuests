package com.example.internationalquests.managers;

import com.example.internationalquests.InternationalQuests;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GuiManager implements Listener {

    private final InternationalQuests plugin;
    private final MainMenuManager mainMenuManager;
    private final DailyGuiManager dailyGuiManager;
    private final WeeklyGuiManager weeklyGuiManager;

    public GuiManager(InternationalQuests plugin) {
        this.plugin = plugin;
        this.mainMenuManager = new MainMenuManager(plugin);
        this.dailyGuiManager = new DailyGuiManager(plugin);
        this.weeklyGuiManager = new WeeklyGuiManager(plugin);
    }

    public void reload() {
        // Конфиги перезагружаются через ConfigManager
    }

    public int getSlotCount() {
        return dailyGuiManager.getSlotCount();
    }

    public int getDailySlotCount() {
        return dailyGuiManager.getSlotCount();
    }

    public void openMainMenu(Player player) {
        mainMenuManager.openMenu(player);
    }

    public void openDailyQuestsGui(Player player) {
        dailyGuiManager.openGui(player);
    }

    public void openWeeklyQuestsGui(Player player) {
        weeklyGuiManager.openGui(player);
    }

    public void openQuestsGui(Player player) {
        openMainMenu(player);
    }

    public void updateOpenGui(Player player) {
        Inventory topInv = player.getOpenInventory().getTopInventory();
        if (topInv.getHolder() instanceof MainMenuManager.MainMenuHolder) {
            openMainMenu(player);
        } else if (topInv.getHolder() instanceof DailyGuiManager.DailyGuiHolder) {
            openDailyQuestsGui(player);
        } else if (topInv.getHolder() instanceof WeeklyGuiManager.WeeklyGuiHolder) {
            openWeeklyQuestsGui(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof MainMenuManager.MainMenuHolder) {
            event.setCancelled(true);
            mainMenuManager.handleClick(event);
        } else if (holder instanceof DailyGuiManager.DailyGuiHolder) {
            event.setCancelled(true);
            dailyGuiManager.handleClick(event);
        } else if (holder instanceof WeeklyGuiManager.WeeklyGuiHolder) {
            event.setCancelled(true);
            weeklyGuiManager.handleClick(event);
        }
    }

    // Для обратной совместимости
    public static class QuestGuiHolder extends DailyGuiManager.DailyGuiHolder {
    }
}