package com.shitlime.era.pojo.properties;

import lombok.Data;

import java.util.List;

@Data
public class BotPreperties {
    private List<Long> admin;
    private String cmd;
}
