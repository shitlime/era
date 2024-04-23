package com.shitlime.era.config;

import lombok.Data;
import com.shitlime.era.pojo.properties.BotPreperties;
import com.shitlime.era.pojo.properties.ResourcesProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "era")
public class EraConfig {
    private ResourcesProperties resources;
    private BotPreperties bot;
}
