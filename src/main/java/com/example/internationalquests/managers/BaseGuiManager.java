package com.example.internationalquests.managers;

import com.example.internationalquests.InternationalQuests;
import com.example.internationalquests.config.BaseConfig;
import com.example.internationalquests.models.ActiveQuest;
import com.example.internationalquests.models.PlayerData;
import com.example.internationalquests.models.Quest;
import com.example.internationalquests.utils.ItemBuilder;
import com.example.internationalquests.utils.QuestUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseGuiManager {

    protected final InternationalQuests plugin;
    protected String title;
    protected int size;
    protected List<Integer> slots;
    protected int infoSlot;
    protected int backSlot;
    protected int closeSlot;
    protected Material backMaterial;
    protected String backName;
    protected List<String> backLore;
    protected Material closeMaterial;
    protected String closeName;
    protected List<String> closeLore;

    public BaseGuiManager(InternationalQuests plugin) {
        this.plugin = plugin;
    }

    protected abstract void loadConfig();
    protected abstract List<ActiveQuest> getQuests(PlayerData data);
    protected abstract Quest getQuestById(String id);
    protected abstract boolean isWeekly();
    protected abstract Inventory createHolder();
    protected abstract BaseConfig getQuestConfig();

    protected void loadCommonConfig(org.bukkit.configuration.file.FileConfiguration config) {
        title = ChatColor.translateAlternateColorCodes('&',
                config.getString("gui.title", "&6Квесты"));
        size = config.getInt("gui.size", 54);
        slots = config.getIntegerList("gui.slots");

        infoSlot = config.getInt("gui.info-slot", 49);
        backSlot = config.getInt("gui.back-slot", 48);
        closeSlot = config.getInt("gui.close-slot", 53);

        backMaterial = Material.getMaterial(config.getString("gui.back-button.material", "ARROW"));
        if (backMaterial == null) backMaterial = Material.ARROW;
        backName = ChatColor.translateAlternateColorCodes('&',
                config.getString("gui.back-button.name", "&e← Назад"));
        backLore = config.getStringList("gui.back-button.lore").stream()
                .map(s -> ChatColor.translateAlternateColorCodes('&', s))
                .collect(Collectors.toList());

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

    public int getSlotCount() {
        return slots.size();
    }

    protected void fillQuests(Inventory inv, Player player, PlayerData data) {
        List<ActiveQuest> quests = getQuests(data);

        for (int i = 0; i < quests.size() && i < slots.size(); i++) {
            ActiveQuest aq = quests.get(i);
            Quest quest = getQuestById(aq.getQuestId());
            if (quest == null) continue;

            int slot = slots.get(i);
            ItemStack icon = createQuestItem(quest, aq, player);
            inv.setItem(slot, icon);
        }
    }

    protected ItemStack createQuestItem(Quest quest, ActiveQuest aq, Player player) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + quest.getDescription());
        lore.add("");

        boolean completed = aq.getProgress() >= quest.getTarget() || aq.isCompleted();

        if (completed) {
            lore.add(plugin.getConfigManager().getMessage("gui-completed-status"));
            lore.add(plugin.getConfigManager().getMessage("gui-completed-hint"));
        } else {
            String progressFormat = plugin.getConfigManager().getMessage("gui-progress-format")
                    .replace("{progress}", String.valueOf(aq.getProgress()))
                    .replace("{target}", String.valueOf(quest.getTarget()));
            lore.add(progressFormat);
        }

        lore.add("");
        addRewardsToLore(lore, quest);
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Тип: " + ChatColor.WHITE + QuestUtils.getQuestTypeName(quest.getType()));

        addExtraLore(lore, quest, aq, player);

        return ItemBuilder.of(quest.getIcon())
                .name(quest.getName())
                .lore(lore)
                .build();
    }

    protected void addRewardsToLore(List<String> lore, Quest quest) {
        BaseConfig questConfig = getQuestConfig();
        List<String> rewardDisplay = questConfig.getRewardDisplay(quest.getId());

        if (!rewardDisplay.isEmpty()) {
            lore.add(ChatColor.GOLD + "Награда:");
            for (String reward : rewardDisplay) {
                lore.add(ChatColor.translateAlternateColorCodes('&', reward));
            }
        }
    }

    protected void addExtraLore(List<String> lore, Quest quest, ActiveQuest aq, Player player) {
        // Переопределяется в наследниках
    }

    protected void addInfoButton(Inventory inv, PlayerData data) {
        ItemStack infoItem = createInfoItem(data);
        inv.setItem(infoSlot, infoItem);
    }

    protected abstract ItemStack createInfoItem(PlayerData data);

    protected void addBackButton(Inventory inv) {
        ItemStack backItem = ItemBuilder.of(backMaterial)
                .name(backName)
                .lore(backLore)
                .build();
        inv.setItem(backSlot, backItem);
    }

    protected void addCloseButton(Inventory inv) {
        ItemStack closeItem = ItemBuilder.of(closeMaterial)
                .name(closeName)
                .lore(closeLore)
                .build();
        inv.setItem(closeSlot, closeItem);
    }

    public void openGui(Player player) {
        Inventory inv = createHolder();

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        fillQuests(inv, player, data);
        addInfoButton(inv, data);
        addBackButton(inv);
        addCloseButton(inv);

        player.openInventory(inv);
    }

    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == closeSlot) {
            player.closeInventory();
            return;
        }

        if (slot == backSlot) {
            plugin.getGuiManager().openMainMenu(player);
            return;
        }

        if (slot == infoSlot) {
            return;
        }

        if (!slots.contains(slot)) return;

        handleQuestClick(player, slot);
    }

    protected abstract void handleQuestClick(Player player, int slot);
}