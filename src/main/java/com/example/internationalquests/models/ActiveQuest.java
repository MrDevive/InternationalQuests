package com.example.internationalquests.models;

public class ActiveQuest {
    private final String questId;
    private int progress;
    private boolean completed;
    private long completionTime;

    public ActiveQuest(String questId, int progress) {
        this.questId = questId;
        this.progress = progress;
        this.completed = false;
        this.completionTime = 0;
    }

    public String getQuestId() {
        return questId;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(long completionTime) {
        this.completionTime = completionTime;
    }
}