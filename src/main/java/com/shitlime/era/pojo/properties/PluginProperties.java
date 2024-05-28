package com.shitlime.era.pojo.properties;

import lombok.Data;

@Data
public class PluginProperties {
    /**
     * 查字插件配置
     */
    private CharacterSearchProperties characterSearch;
    /**
     * AI 聊天插件配置
     */
    private AIChatProperties aiChat;
}
