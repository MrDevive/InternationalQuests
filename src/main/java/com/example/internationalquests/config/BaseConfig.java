package com.example.internationalquests.config;

import com.example.internationalquests.InternationalQuests;
import com.example.internationalquests.models.Quest;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseConfig {

    protected final InternationalQuests plugin;
    protected FileConfiguration config;
    protected final Map<String, Quest> quests = new HashMap<>();
    protected final String fileName;
    protected final Quest.QuestCategory category;

    public BaseConfig(InternationalQuests plugin, String fileName, Quest.QuestCategory category) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.category = category;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        config = YamlConfiguration.loadConfiguration(file);
        loadQuests();
    }

    public void reload() {
        quests.clear();
        load();
    }

    protected void loadQuests() {
        ConfigurationSection section = config.getConfigurationSection("quests");
        if (section == null) {
            plugin.getLogger().warning("Секция 'quests' не найдена в " + fileName);
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection qSec = section.getConfigurationSection(id);
            if (qSec == null) continue;

            Quest quest = parseQuest(id, qSec);
            if (quest != null) {
                quest.setQuestCategory(category);
                quests.put(id, quest);
            }
        }

        plugin.getLogger().info("Загружено " + quests.size() + " квестов из " + fileName);
    }

    protected Quest parseQuest(String id, ConfigurationSection qSec) {
        String typeStr = qSec.getString("type");
        Quest.QuestType type;
        try {
            type = Quest.QuestType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Неизвестный тип квеста: " + typeStr + " для " + id);
            return null;
        }

        int target = qSec.getInt("target");
        String name = ChatColor.translateAlternateColorCodes('&', qSec.getString("name", "Квест"));
        String description = ChatColor.translateAlternateColorCodes('&', qSec.getString("description", ""));
        List<String> rewardCommands = qSec.getStringList("reward_commands");
        double chance = qSec.getDouble("chance", 1.0);

        Material icon = Material.getMaterial(qSec.getString("icon", "STONE"));
        if (icon == null) icon = Material.STONE;

        Quest quest = new Quest(id, type, target, name, description, rewardCommands, chance, icon);

        // Обработка параметров в зависимости от типа
        if (type == Quest.QuestType.KILL_MOBS) {
            String entityName = qSec.getString("entity");
            if (entityName != null) {
                try {
                    quest.setEntityType(EntityType.valueOf(entityName.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Неизвестный тип сущности: " + entityName + " для " + id);
                }
            }
        } else if (type == Quest.QuestType.BREAK_BLOCKS ||
                type == Quest.QuestType.PLACE_BLOCKS ||
                type == Quest.QuestType.INTERACT_BLOCKS) {
            String blockName = qSec.getString("block");
            if (blockName != null) {
                Material blockMat = Material.getMaterial(blockName.toUpperCase());
                if (blockMat != null && blockMat.isBlock()) {
                    quest.setBlockType(blockMat);
                } else {
                    plugin.getLogger().warning("Неверный тип блока: " + blockName + " для " + id);
                }
            }
        } else if (type == Quest.QuestType.EAT_ITEM ||
                type == Quest.QuestType.CRAFT_ITEM ||
                type == Quest.QuestType.SMELT_ITEMS) {
            String itemName = qSec.getString("item");
            if (itemName != null) {
                Material itemMat = Material.getMaterial(itemName.toUpperCase());
                if (itemMat != null) {
                    quest.setItemType(itemMat);
                } else {
                    plugin.getLogger().warning("Неверный тип предмета: " + itemName + " для " + id);
                }
            }
        }
        if (type == Quest.QuestType.GAIN_EXPERIENCE) {
            // Для этого типа не нужны дополнительные параметры
            // Можно добавить source если нужно фильтровать по источнику
            String source = qSec.getString("source");
            if (source != null) {
                // Можно сохранить в метаданных квеста
            }
        }

        return quest;
    }

    public Quest getQuest(String id) {
        return quests.get(id);
    }

    public Map<String, Quest> getQuests() {
        return new HashMap<>(quests);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public List<String> getRewardDisplay(String questId) {
        return config.getStringList("quests." + questId + ".reward_display");
    }
}