package com.example.internationalquests.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private long lastReset;
    private long lastWeeklyReset; // НОВОЕ
    private long lastSwapReset;
    private int swapsUsed;
    private List<ActiveQuest> activeQuests;
    private List<ActiveQuest> weeklyQuests; // НОВОЕ

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.lastReset = 0;
        this.lastWeeklyReset = 0; // НОВОЕ
        this.lastSwapReset = 0;
        this.swapsUsed = 0;
        this.activeQuests = new ArrayList<>();
        this.weeklyQuests = new ArrayList<>(); // НОВОЕ
    }

    public UUID getUuid() { return uuid; }
    public long getLastReset() { return lastReset; }
    public void setLastReset(long lastReset) { this.lastReset = lastReset; }

    // НОВЫЕ ГЕТТЕРЫ И СЕТТЕРЫ
    public long getLastWeeklyReset() { return lastWeeklyReset; }
    public void setLastWeeklyReset(long lastWeeklyReset) { this.lastWeeklyReset = lastWeeklyReset; }

    public long getLastSwapReset() { return lastSwapReset; }
    public void setLastSwapReset(long lastSwapReset) { this.lastSwapReset = lastSwapReset; }

    public int getSwapsUsed() { return swapsUsed; }
    public void setSwapsUsed(int swapsUsed) { this.swapsUsed = swapsUsed; }

    public List<ActiveQuest> getActiveQuests() { return activeQuests; }
    public void setActiveQuests(List<ActiveQuest> activeQuests) { this.activeQuests = activeQuests; }

    // НОВЫЕ ГЕТТЕРЫ И СЕТТЕРЫ
    public List<ActiveQuest> getWeeklyQuests() { return weeklyQuests; }
    public void setWeeklyQuests(List<ActiveQuest> weeklyQuests) { this.weeklyQuests = weeklyQuests; }
}