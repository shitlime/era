package com.shitlime.era.config;

import com.shitlime.era.pojo.properties.PluginProperties;
import lombok.Data;
import com.shitlime.era.pojo.properties.BotProperties;
import com.shitlime.era.pojo.properties.ResourcesProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "era")
public class EraConfig {
    private BotProperties bot;
    private ResourcesProperties resources;
    private PluginProperties plugin;
}
