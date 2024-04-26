package com.shitlime.era.pojo.entry;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * rss 源
 */
@Data
public class RssSource {
    private Long id;
    private String url;
    private String title;
    private String latestFeed;
    private LocalDateTime fetchTime;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
}
