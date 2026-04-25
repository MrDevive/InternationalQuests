package com.example.internationalquests.models;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.List;

public class Quest {

    private final String id;
    private final QuestType type;
    private final int target;
    private final String name;
    private final String description;
    private final List<String> rewardCommands;
    private final double chance;
    private final Material icon;

    // Дополнительные параметры в зависимости от типа
    private EntityType entityType; // для KILL_MOBS
    private Material blockType;    // для BREAK_BLOCKS, PLACE_BLOCKS, INTERACT_BLOCKS
    private Material itemType;     // для EAT_ITEM, CRAFT_ITEM, SMELT_ITEMS

    private QuestCategory questCategory;

    public enum QuestCategory {
        DAILY,
        WEEKLY
    }

    public enum QuestType {
        KILL_MOBS,
        BREAK_BLOCKS,
        CATCH_FISH,
        WALK_DISTANCE,
        PLAY_TIME,
        EAT_ITEM,
        CRAFT_ITEM,
        PLACE_BLOCKS,
        SMELT_ITEMS,
        INTERACT_BLOCKS,
        TAKE_DAMAGE,
        DEAL_DAMAGE,
        GAIN_EXPERIENCE  // Получение опыта из InternationalLevel
    }

    public Quest(String id, QuestType type, int target, String name, String description,
                 List<String> rewardCommands, double chance, Material icon) {
        this.id = id;
        this.type = type;
        this.target = target;
        this.name = name;
        this.description = description;
        this.rewardCommands = rewardCommands;
        this.chance = chance;
        this.icon = icon;
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public QuestType getType() { return type; }
    public int getTarget() { return target; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getRewardCommands() { return rewardCommands; }
    public double getChance() { return chance; }
    public Material getIcon() { return icon; }

    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }

    public Material getBlockType() { return blockType; }
    public void setBlockType(Material blockType) { this.blockType = blockType; }

    public Material getItemType() { return itemType; }
    public void setItemType(Material itemType) { this.itemType = itemType; }

    public QuestCategory getQuestCategory() { return questCategory; }
    public void setQuestCategory(QuestCategory questCategory) { this.questCategory = questCategory; }
}