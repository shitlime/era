package com.shitlime.era.pojo;

import com.shitlime.era.enums.DatasetTypeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据集配置
 */
@Data
@NoArgsConstructor
public class DatasetConfig {
    private String id;
    private String name;
    private DatasetTypeEnum type;
    private String md5;
    private String path;
    private DataFormat format;
}
