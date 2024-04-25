package com.shitlime.era.pojo.dto;

import com.shitlime.era.pojo.config.dataset.DatasetConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

/**
 * 描述一份数据集
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Dataset {
    private DatasetConfig datasetConfig;
    private String tableName;
    private File path;
}
