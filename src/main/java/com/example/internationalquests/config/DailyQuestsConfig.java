package com.example.internationalquests.config;

import com.example.internationalquests.InternationalQuests;
import com.example.internationalquests.models.Quest;

public class DailyQuestsConfig extends BaseConfig {

    public DailyQuestsConfig(InternationalQuests plugin) {
        super(plugin, "DailyQuests.yml", Quest.QuestCategory.DAILY);
    }
}