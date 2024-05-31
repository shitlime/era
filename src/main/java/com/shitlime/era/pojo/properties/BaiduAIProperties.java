package com.shitlime.era.pojo.properties;

import lombok.Data;

import java.util.List;

@Data
public class BaiduAIProperties {
    /**
     * api key
     */
    private String apiKey;
    /**
     * secret key
     */
    private String secretKey;
    /**
     * 模块名称
     */
    private String modelId;
    /**
     * 初始聊天记录
     */
    private List<String> initialChat;
}
