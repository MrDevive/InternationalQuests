package com.example.internationalquests.managers;

import com.example.internationalquests.InternationalQuests;
import com.example.internationalquests.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public class MainMenuManager {

    private final InternationalQuests plugin;
    private String title;
    private int size;
    private int dailySlot;
    private int weeklySlot;
    private int closeSlot;
    private Material dailyMaterial;
    private String dailyName;
    private List<String> dailyLore;
    private Material weeklyMaterial;
    private String weeklyName;
    private List<String> weeklyLore;
    private Material closeMaterial;
    private String closeName;
    private List<String> closeLore;

    public MainMenuManager(InternationalQuests plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        FileConfiguration config = plugin.getConfigManager().getQuestGui();

        title = ChatColor.translateAlternateColorCodes('&',
                config.getString("gui.title", "&6Меню квестов"));
        size = config.getInt("gui.size", 27);

        dailySlot = config.getInt("gui.daily.slot", 11);
        dailyMaterial = Material.getMaterial(config.getString("gui.daily.material", "CLOCK"));
        if (dailyMaterial == null) dailyMaterial = Material.CLOCK;
        dailyName = ChatColor.translateAlternateColorCodes('&',
                config.getString("gui.daily.name", "&e&lЕжедневные квесты"));
        dailyLore = config.getStringList("gui.daily.lore").stream()
                .map(s -> ChatColor.translateAlternateColorCodes('&', s))
                .collect(Collectors.toList());

        weeklySlot = config.getInt("gui.weekly.slot", 15);
        weeklyMaterial = Material.getMaterial(config.getString("gui.weekly.material", "ENDER_EYE"));
        if (weeklyMaterial == null) weeklyMaterial = Material.ENDER_EYE;
        weeklyName = ChatColor.translateAlternateColorCodes('&',
                config.getString("gui.weekly.name", "&5&lЕженедельные квесты"));
        weeklyLore = config.getStringList("gui.weekly.lore").stream()
                .map(s -> ChatColor.translateAlternateColorCodes('&', s))
                .collect(Collectors.toList());

        closeSlot = config.getInt("gui.close-slot", 26);
        closeMaterial = Material.getMaterial(config.getString("gui.close-button.material", "BARRIER"));
        if (closeMaterial == null) closeMaterial = Material.BARRIER;
        closeName = ChatColor.translateAlternateColorCodes('&',
                config.getString("gui.close-button.name", "&cЗакрыть меню"));
        closeLore = config.getStringList("gui.close-button.lore").stream()
                .map(s -> ChatColor.translateAlternateColorCodes('&', s))
                .collect(Collectors.toList());
        if (closeLore.isEmpty()) {
            closeLore.add(ChatColor.translateAlternateColorCodes('&', "&7Нажмите чтобы закрыть"));
        }
    }

    public void openMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new MainMenuHolder(), size, title);

        ItemStack dailyItem = ItemBuilder.of(dailyMaterial)
                .name(dailyName)
                .lore(dailyLore)
                .build();
        inv.setItem(dailySlot, dailyItem);

        ItemStack weeklyItem = ItemBuilder.of(weeklyMaterial)
                .name(weeklyName)
                .lore(weeklyLore)
                .build();
        inv.setItem(weeklySlot, weeklyItem);

        ItemStack closeItem = ItemBuilder.of(closeMaterial)
                .name(closeName)
                .lore(closeLore)
                .build();
        inv.setItem(closeSlot, closeItem);

        player.openInventory(inv);
    }

    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == dailySlot) {
            plugin.getQuestManager().checkReset(player);
            plugin.getGuiManager().openDailyQuestsGui(player);
        } else if (slot == weeklySlot) {
            plugin.getQuestManager().checkWeeklyReset(player);
            plugin.getGuiManager().openWeeklyQuestsGui(player);
        } else if (slot == closeSlot) {
            player.closeInventory();
        }
    }

    public static class MainMenuHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}