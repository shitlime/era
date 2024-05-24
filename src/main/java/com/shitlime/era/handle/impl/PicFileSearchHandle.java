package com.shitlime.era.handle.impl;

import com.shitlime.era.pojo.dto.Dataset;
import com.shitlime.era.handle.DatasetSearchHandle;
import com.shitlime.era.mapper.DatasetMapper;
import com.shitlime.era.pojo.entry.DataItem;
import com.shitlime.era.pojo.config.dataset.DatasetConfig;
import com.shitlime.era.utils.DatasetUtils;
import com.shitlime.era.utils.TableUtils;
import com.shitlime.era.utils.UnicodeUtils;
import com.shitlime.era.utils.YamlUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 图片文件搜索处理器
 */
@Slf4j
@Component
public class PicFileSearchHandle implements DatasetSearchHandle {

    @Autowired
    TableUtils tableUtils;
    @Autowired
    DatasetMapper datasetMapper;
    @Autowired
    private DatasetUtils datasetUtils;

    @SneakyThrows(value = IOException.class)
    @Override
    public void load(Dataset dataset) {
        File path = dataset.getPath();
        DatasetConfig datasetConfig = dataset.getDatasetConfig();
        String tableName = dataset.getTableName();
        File dataPath = new File(path, datasetConfig.getPath());
        File[] picArray = dataPath.listFiles();

        // 校验md5，一致时跳过数据库初始化
        StringBuilder picPaths = new StringBuilder();
        for (File pic : picArray) {
            picPaths.append(pic.getAbsolutePath());
        }
        String md5 = DigestUtils.md5DigestAsHex(picPaths.toString().getBytes());
        if (md5.equals(datasetConfig.getMd5()) && tableUtils.isExist(tableName)) {
            return;
        }
        datasetConfig.setMd5(md5);

        // 处理数据
        Pattern originRegex = Pattern.compile(datasetConfig.getFormat().getOrigin());
        String keyRegex = datasetConfig.getFormat().getKey();
        ArrayList<DataItem> dataItems = new ArrayList<>();
        for (File pic : picArray) {
            String picName = pic.getName();
            if (picName.matches(datasetConfig.getFormat().getOrigin())) {
                switch (datasetConfig.getFormat().getKeyType()) {
                    case UNICODE -> {
                        String key = UnicodeUtils.unicodeToCharacter(
                                originRegex.matcher(picName).replaceFirst(keyRegex));
                        String values = pic.getAbsolutePath();
                        dataItems.add(new DataItem(key, values));
                    }
                    case CHARACTER -> {
                        String key = originRegex.matcher(picName).replaceFirst(keyRegex);
                        String values = pic.getAbsolutePath();
                        dataItems.add(new DataItem(key, values));
                    }
                }
            }
        }

        // 图片路径插入数据库
        if (tableUtils.isExist(tableName)) {
            datasetMapper.dropTable(tableName);
        }
        datasetMapper.createTable(tableName);
        datasetUtils.insertAllData(dataItems, tableName);

        // 保存 YAML
        YamlUtils.dumpYaml(datasetConfig, new File(path, "config.yml"));
    }

    @Override
    public List<String> search(Dataset dataset, String character) {
        String tableName = dataset.getTableName();
        List<String> list = new ArrayList<>();
        List<DataItem> dataItems = datasetMapper.selectByKey(tableName, character);
        dataItems.forEach(item -> list.add(item.getValues()));
        return list;
    }

    @Override
    public List<String> searchByHex(Dataset dataset, String hex) {
        return search(dataset, UnicodeUtils.unicodeToCharacter(hex));
    }
}
