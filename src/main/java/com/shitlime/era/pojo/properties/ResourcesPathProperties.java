package com.shitlime.era.pojo.properties;

import lombok.Data;

@Data
public class ResourcesPathProperties {
    /**
     * sqlite数据库路径
     */
    private String sqlite;
    /**
     * 字体路径
     */
    private String fonts;
    /**
     * 临时文件路径
     */
    private String temp;
}
