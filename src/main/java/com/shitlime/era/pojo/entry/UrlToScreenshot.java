package com.shitlime.era.pojo.entry;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UrlToScreenshot {
    private Long id;
    private String domainName;
    private Boolean enable;
    private Long groupId;
    private Long userId;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
}
