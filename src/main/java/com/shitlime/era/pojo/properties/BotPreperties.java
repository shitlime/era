package com.shitlime.era.pojo.properties;

import lombok.Data;

import java.util.List;

@Data
public class BotPreperties {
    private String name;
    private Long id;
    private List<Long> admin;
    private String cmd;
}
