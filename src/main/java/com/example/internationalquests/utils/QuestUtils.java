package com.example.internationalquests.utils;

import com.example.internationalquests.models.Quest;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.concurrent.TimeUnit;

public final class QuestUtils {

    private QuestUtils() {
        // Утилитарный класс
    }

    // ==================== Форматирование времени ====================

    public static String formatTime(long seconds) {
        if (seconds <= 0) return "0с";

        long hours = TimeUnit.SECONDS.toHours(seconds);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dч %dм %dс", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dм %dс", minutes, secs);
        } else {
            return String.format("%dс", secs);
        }
    }

    public static String formatTimeShort(long seconds) {
        if (seconds <= 0) return "0с";

        long hours = TimeUnit.SECONDS.toHours(seconds);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;

        if (hours > 0) {
            return String.format("%dч", hours);
        } else if (minutes > 0) {
            return String.format("%dм", minutes);
        } else {
            return String.format("%dс", seconds);
        }
    }

    // ==================== Названия типов квестов ====================

    public static String getQuestTypeName(Quest.QuestType type) {
        switch (type) {
            case KILL_MOBS: return "Убийство мобов";
            case BREAK_BLOCKS: return "Разрушение блоков";
            case PLACE_BLOCKS: return "Установка блоков";
            case CATCH_FISH: return "Рыбалка";
            case WALK_DISTANCE: return "Ходьба";
            case PLAY_TIME: return "Время игры";
            case EAT_ITEM: return "Поедание";
            case CRAFT_ITEM: return "Крафт";
            case SMELT_ITEMS: return "Переплавка";
            case INTERACT_BLOCKS: return "Взаимодействие";
            case TAKE_DAMAGE: return "Получение урона";
            case DEAL_DAMAGE: return "Нанесение урона";
            case GAIN_EXPERIENCE: return "Получение опыта";
            default: return type.toString();
        }
    }

    public static String getQuestCategoryName(Quest.QuestCategory category) {
        switch (category) {
            case DAILY: return "Ежедневный";
            case WEEKLY: return "Еженедельный";
            default: return category.toString();
        }
    }

    // ==================== Цветовое форматирование ====================

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String stripColor(String text) {
        return ChatColor.stripColor(color(text));
    }

    // ==================== Проверки ====================

    public static boolean isInteractableBlock(Material material) {
        if (material == null) return false;

        String name = material.name();
        return material == Material.CHEST ||
                material == Material.TRAPPED_CHEST ||
                material == Material.FURNACE ||
                material == Material.BLAST_FURNACE ||
                material == Material.SMOKER ||
                material == Material.CRAFTING_TABLE ||
                material == Material.ENCHANTING_TABLE ||
                material == Material.ANVIL ||
                material == Material.BREWING_STAND ||
                material == Material.BEACON ||
                material == Material.LOOM ||
                material == Material.STONECUTTER ||
                material == Material.GRINDSTONE ||
                material == Material.CARTOGRAPHY_TABLE ||
                material == Material.SMITHING_TABLE ||
                name.contains("SHULKER_BOX") ||
                name.contains("BED") ||
                name.contains("DOOR") ||
                name.contains("GATE") ||
                name.contains("BUTTON") ||
                name.contains("LEVER");
    }

    public static boolean isEdible(Material material) {
        return material != null && material.isEdible();
    }

    public static boolean isBlock(Material material) {
        return material != null && material.isBlock();
    }

    // ==================== Прогресс-бар ====================

    public static String createProgressBar(int current, int target, int bars) {
        return createProgressBar(current, target, bars, "&a", "&7");
    }

    public static String createProgressBar(int current, int target, int bars, String filledColor, String emptyColor) {
        double percent = Math.min(1.0, (double) current / target);
        int filledBars = (int) (bars * percent);

        StringBuilder builder = new StringBuilder();
        builder.append(filledColor);
        for (int i = 0; i < filledBars; i++) {
            builder.append("█");
        }
        builder.append(emptyColor);
        for (int i = filledBars; i < bars; i++) {
            builder.append("░");
        }

        return color(builder.toString());
    }

    // ==================== Плейсхолдеры ====================

    public static String replacePlaceholders(String text, Object... replacements) {
        if (text == null) return "";

        String result = text;
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String key = String.valueOf(replacements[i]);
                String value = String.valueOf(replacements[i + 1]);
                result = result.replace(key, value);
            }
        }
        return result;
    }

    // ==================== Случайные числа ====================

    public static boolean chance(double percent) {
        return Math.random() * 100 < percent;
    }

    public static int randomInt(int min, int max) {
        return (int) (Math.random() * (max - min + 1)) + min;
    }
}