package com.shitlime.era.handle.impl;

import com.shitlime.era.common.Dataset;
import com.shitlime.era.enums.KeyTypeEnum;
import com.shitlime.era.mapper.DatasetMapper;
import com.shitlime.era.pojo.entry.DataItem;
import com.shitlime.era.pojo.DatasetConfig;
import com.shitlime.era.handle.DatasetSearchHandle;
import com.shitlime.era.utils.DatasetUtils;
import com.shitlime.era.utils.TableUtils;
import com.shitlime.era.utils.UnicodeUtils;
import com.shitlime.era.utils.YamlUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * 文本行搜索处理器
 */
@Slf4j
@Component
public class TxtLineSearchHandle implements DatasetSearchHandle {

    @Autowired
    TableUtils tableUtils;
    @Autowired
    DatasetMapper datasetMapper;
    @Autowired
    private DatasetUtils datasetUtils;

    /**
     * 装载数据集对应数据库
     * @param dataset
     */
    @SneakyThrows(IOException.class)
    @Transactional
    @Override
    public void load(Dataset dataset) {
        File path = dataset.getPath();
        DatasetConfig datasetConfig = dataset.getDatasetConfig();
        String tableName = dataset.getTableName();

        // 校验md5，一致时跳过数据库初始化
        File dataPath = new File(path, datasetConfig.getPath());
        FileInputStream dataInputStream = new FileInputStream(dataPath);
        String md5 = DigestUtils.md5DigestAsHex(dataInputStream);
        dataInputStream.close();
        if (md5.equals(datasetConfig.getMd5()) && tableUtils.isExist(tableName)) {
            return;
        }
        datasetConfig.setMd5(md5);

        // 读取并过滤无效数据
        Pattern originRegex = Pattern.compile(datasetConfig.getFormat().getOrigin());
        String keyRegex = datasetConfig.getFormat().getKey();
        String valuesRegex = datasetConfig.getFormat().getValues();
        ArrayList<DataItem> dataItems = new ArrayList<>();
        BufferedReader dataReader = new BufferedReader(new FileReader(dataPath));
        dataReader.lines()
                .filter(s -> s.matches(datasetConfig.getFormat().getOrigin()))
                .forEach(data -> {
                    String key = originRegex.matcher(data).replaceFirst(keyRegex);
                    String values = originRegex.matcher(data).replaceFirst(valuesRegex);
                    dataItems.add(new DataItem(key, values));
                });
        dataReader.close();
        // 需要处理的 key type
        if (datasetConfig.getFormat().getKeyType() != KeyTypeEnum.COMBINE) {
            for (int i = 0; i < dataItems.size(); i++) {
                for (int j = i + 1; j < dataItems.size(); j++) {
                    String iKey = dataItems.get(i).getKey();
                    String jKey = dataItems.get(j).getKey();
                    if (iKey.equals(jKey)) {
                        switch (datasetConfig.getFormat().getKeyType()) {
                            case UNIQUE_MIN -> {
                                if (DatasetUtils.compareValues(iKey, jKey) < 0) {
                                    dataItems.remove(j);
                                    i--;
                                } else {
                                    dataItems.remove(i);
                                    i--;
                                }
                            }
                            case UNIQUE_MAX -> {
                                if (DatasetUtils.compareValues(iKey, jKey) > 0) {
                                    dataItems.remove(j);
                                    i--;
                                } else {
                                    dataItems.remove(i);
                                    i--;
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }

        // 数据装入数据库
        if (tableUtils.isExist(tableName)) {
            datasetMapper.dropTable(tableName);
        }
        datasetMapper.createTable(tableName);
        datasetUtils.insertAllData(dataItems, tableName);

        // 保存 YAML
        YamlUtils.dumpYaml(datasetConfig, new File(path, "config.yml"));
    }

    /**
     * 查询数据集数据
     * @param dataset
     * @param character 单个字符
     * @return
     */
    @Override
    public String search(Dataset dataset, String character) {
        String tableName = dataset.getTableName();
        StringJoiner joiner = new StringJoiner(dataset.getDatasetConfig()
                .getFormat().getValuesSeparator());
        List<DataItem> dataItems = datasetMapper.selectByKey(tableName, character);
        dataItems.forEach(item -> joiner.add(item.getValues()));
        return joiner.toString();
    }

    /**
     * 查询数据集数据
     * @param dataset
     * @param hex unicode
     * @return
     */
    @Override
    public String searchByHex(Dataset dataset, String hex) {
        return search(dataset, UnicodeUtils.unicodeToCharacter(hex));
    }
}
