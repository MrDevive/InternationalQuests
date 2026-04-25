package com.example.internationalquests.config;

import com.example.internationalquests.InternationalQuests;
import com.example.internationalquests.models.Quest;

public class WeeklyQuestsConfig extends BaseConfig {

    public WeeklyQuestsConfig(InternationalQuests plugin) {
        super(plugin, "WeeklyQuests.yml", Quest.QuestCategory.WEEKLY);
    }
}