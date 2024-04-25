package com.shitlime.era.pojo.entry;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * RSS 订阅数据
 */
@Data
public class RssSubscription {
    private Long id;
    private Long sourceId;
    private Long groupId;
    private Long userId;
    private Boolean enable;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
}
