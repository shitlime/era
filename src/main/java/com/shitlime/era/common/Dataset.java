package com.shitlime.era.common;

import com.shitlime.era.pojo.DatasetConfig;
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
