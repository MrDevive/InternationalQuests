package com.example.internationalquests.hooks;

import com.example.internationalquests.InternationalQuests;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Polling-версия хука для InternationalLevel
 * Не требует прямой зависимости и не вызывает NoSuchMethodException
 */
public class InternationalLevelHook implements Listener {

    private final InternationalQuests plugin;
    private Plugin levelPlugin;
    private boolean enabled = false;

    private Method getLevelManagerMethod;
    private Method getPlayerDataMethod;
    private Method getExperienceMethod;

    private final Map<UUID, Long> expCache = new ConcurrentHashMap<>();

    private ExpGainListener expGainListener;

    public interface ExpGainListener {
        void onExpGain(Player player, long amount, String source);
    }

    public InternationalLevelHook(InternationalQuests plugin) {
        this.plugin = plugin;
        setupHook();
        startExpTracking();
    }

    private void setupHook() {
        levelPlugin = Bukkit.getPluginManager().getPlugin("InternationalLevel");

        if (levelPlugin != null && levelPlugin.isEnabled()) {
            try {
                // Получаем методы через рефлексию
                Class<?> levelClass = levelPlugin.getClass();
                Class<?> levelManagerClass = Class.forName("com.example.internationallevel.LevelManager");
                Class<?> playerDataClass = Class.forName("com.example.internationallevel.models.PlayerData");

                getLevelManagerMethod = levelClass.getMethod("getLevelManager");
                getPlayerDataMethod = levelManagerClass.getMethod("getPlayerData", Player.class);
                getExperienceMethod = playerDataClass.getMethod("getExperience");

                enabled = true;
                plugin.getLogger().info("§aInternationalLevelHook успешно активирован!");

            } catch (ClassNotFoundException e) {
                plugin.getLogger().warning("§cНе найдены классы InternationalLevel: " + e.getMessage());
                enabled = false;
            } catch (NoSuchMethodException e) {
                plugin.getLogger().warning("§cНе найдены методы InternationalLevel: " + e.getMessage());
                enabled = false;
            }
        } else {
            plugin.getLogger().warning("§cInternationalLevel не найден! Квесты на опыт не будут работать.");
        }
    }

    public void setExpGainListener(ExpGainListener listener) {
        this.expGainListener = listener;
    }

    private void startExpTracking() {
        // Проверяем опыт каждую секунду
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!enabled || levelPlugin == null) return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                checkExpChange(player);
            }
        }, 20L, 20L);
    }

    private void checkExpChange(Player player) {
        if (!enabled) return;

        UUID uuid = player.getUniqueId();

        try {
            Object levelManager = getLevelManagerMethod.invoke(levelPlugin);
            Object playerData = getPlayerDataMethod.invoke(levelManager, player);
            if (playerData == null) return;

            long currentExp = (long) getExperienceMethod.invoke(playerData);
            Long lastExp = expCache.get(uuid);

            if (lastExp != null && currentExp > lastExp) {
                long gained = currentExp - lastExp;
                if (expGainListener != null && gained > 0) {
                    expGainListener.onExpGain(player, gained, "опыт");
                }
            }
            expCache.put(uuid, currentExp);

        } catch (Exception e) {
            // Игрок мог выйти во время проверки или произошла ошибка рефлексии
            // Не логируем, чтобы не засорять консоль
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        cachePlayerExp(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        expCache.remove(event.getPlayer().getUniqueId());
    }

    private void cachePlayerExp(Player player) {
        if (!enabled) return;

        try {
            Object levelManager = getLevelManagerMethod.invoke(levelPlugin);
            Object playerData = getPlayerDataMethod.invoke(levelManager, player);
            if (playerData != null) {
                expCache.put(player.getUniqueId(), (long) getExperienceMethod.invoke(playerData));
            }
        } catch (Exception e) {
            // Игнорируем ошибки кэширования
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void reload() {
        expCache.clear();
        setupHook();
        if (enabled) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                cachePlayerExp(player);
            }
        }
    }
}