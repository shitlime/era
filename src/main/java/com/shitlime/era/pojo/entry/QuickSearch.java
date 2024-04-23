package com.shitlime.era.pojo.entry;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QuickSearch {
    private Long id;
    private String prefix;
    private String datasetId;
    private Long groupId;
    private Long userId;
    private LocalDateTime createTime;
}
