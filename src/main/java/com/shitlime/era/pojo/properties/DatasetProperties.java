package com.shitlime.era.pojo.properties;

import lombok.Data;

@Data
public class DatasetProperties {
    /**
     * 数据集路径
     */
    private String path;
    /**
     * 数据集在数据库中的表名前缀
     */
    private String tableNamePrefix;
}
