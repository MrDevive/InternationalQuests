package com.example.internationalquests.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder name(String name) {
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        return lore(Arrays.asList(lines));
    }

    public ItemBuilder lore(List<String> lines) {
        List<String> colored = new ArrayList<>();
        for (String line : lines) {
            colored.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(colored);
        return this;
    }

    public ItemBuilder addLore(String... lines) {
        List<String> currentLore = meta.getLore();
        if (currentLore == null) currentLore = new ArrayList<>();

        for (String line : lines) {
            currentLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(currentLore);
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder hideAttributes() {
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        return this;
    }

    public ItemBuilder hideEnchants() {
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemBuilder hideAll() {
        meta.addItemFlags(ItemFlag.values());
        return this;
    }

    public ItemBuilder glow() {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        return this;
    }

    public ItemBuilder apply(Consumer<ItemMeta> consumer) {
        consumer.accept(meta);
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }

    // Статические фабричные методы для удобства
    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    public static ItemBuilder of(ItemStack item) {
        return new ItemBuilder(item);
    }

    // Предустановленные шаблоны
    public static ItemBuilder backButton() {
        return of(Material.ARROW)
                .name("&e← Назад")
                .lore("&7Вернуться в главное меню");
    }

    public static ItemBuilder closeButton() {
        return of(Material.BARRIER)
                .name("&cЗакрыть меню")
                .lore("&7Нажмите чтобы закрыть");
    }

    public static ItemBuilder infoButton(Material material, String title, String... lore) {
        return of(material)
                .name(title)
                .lore(lore);
    }

    public static ItemBuilder placeholder() {
        return of(Material.BLACK_STAINED_GLASS_PANE)
                .name(" ");
    }
}