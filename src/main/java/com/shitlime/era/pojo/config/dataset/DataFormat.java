package com.shitlime.era.pojo.config.dataset;

import com.shitlime.era.enums.KeyTypeEnum;
import lombok.Data;

/**
 * 数据格式
 */
@Data
public class DataFormat {
    private String origin;
    private String key;
    private String values;
    private KeyTypeEnum keyType;
    private String valuesSeparator;
}
