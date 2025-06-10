package com.shitlime.era.handle.impl;

import com.shitlime.era.handle.DatasetSearchHandle;
import com.shitlime.era.mapper.DatasetMapper;
import com.shitlime.era.pojo.config.dataset.DatasetConfig;
import com.shitlime.era.pojo.dto.Dataset;
import com.shitlime.era.pojo.entry.DataItem;
import com.shitlime.era.utils.DatasetUtils;
import com.shitlime.era.utils.TableUtils;
import com.shitlime.era.utils.UnicodeUtils;
import com.shitlime.era.utils.YamlUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
@Component
public class PicZipSearchHandle implements DatasetSearchHandle {
    @Autowired
    TableUtils tableUtils;
    @Autowired
    DatasetMapper datasetMapper;
    @Autowired
    DatasetUtils datasetUtils;

    @SneakyThrows(IOException.class)
    @Override
    public void load(Dataset dataset) {
        File datasetPath = dataset.getPath();
        DatasetConfig datasetConfig = dataset.getDatasetConfig();
        String tableName = dataset.getTableName();
        File zipFile = new File(datasetPath, datasetConfig.getPath());

        // 计算整个ZIP文件的MD5并校验
        String md5;
        try (InputStream is = Files.newInputStream(zipFile.toPath())) {
            md5 = DigestUtils.md5DigestAsHex(is);
        }
        if (md5.equals(datasetConfig.getMd5()) && tableUtils.isExist(tableName)) {
            return;
        }
        datasetConfig.setMd5(md5);

        // 处理ZIP中的图片数据
        Pattern originRegex = Pattern.compile(datasetConfig.getFormat().getOrigin());
        String keyRegex = datasetConfig.getFormat().getKey();
        ArrayList<DataItem> dataItems = new ArrayList<>();
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                String fileName = new File(entryName).getName();
                log.debug("读取zip内文件: {}", entryName);
                if (fileName.matches(datasetConfig.getFormat().getOrigin())) {
                    switch (datasetConfig.getFormat().getKeyType()) {
                        case UNICODE -> {
                            String key = UnicodeUtils.unicodeToCharacter(
                                    originRegex.matcher(fileName).replaceFirst(keyRegex));
                            dataItems.add(new DataItem(key, entryName));
                        }
                        case CHARACTER -> {
                            String key = originRegex.matcher(fileName).replaceFirst(keyRegex);
                            dataItems.add(new DataItem(key, entryName));
                        }
                    }
                }
            }
        }

        // 图片信息插入数据库
        if (tableUtils.isExist(tableName)) {
            datasetMapper.dropTable(tableName);
        }
        datasetMapper.createTable(tableName);
        datasetUtils.insertAllData(dataItems, tableName);

        // 保存YAML
        YamlUtils.dumpYaml(datasetConfig, new File(datasetPath, "config.yml"));
    }

    @SneakyThrows(IOException.class)
    @Override
    public List<byte[]> search(Dataset dataset, String character) {
        List<DataItem> dataItems = datasetMapper.selectByKey(dataset.getTableName(), character);
        File file = new File(dataset.getPath(), dataset.getDatasetConfig().getPath());
        ArrayList<byte[]> list = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(file)) {
            for (DataItem dataItem : dataItems) {
                ZipEntry entry = zipFile.getEntry(dataItem.getValues());
                if (entry == null) {
                    log.error("压缩包{}内找不到{}", file, dataItem.getValues());
                    throw new FileNotFoundException();
                }
                try (InputStream is = zipFile.getInputStream(entry)) {
                    list.add(is.readAllBytes());
                }
            }
        }
        return list;
    }

    @Override
    public List<byte[]> searchByHex(Dataset dataset, String hex) {
        return search(dataset, UnicodeUtils.unicodeToCharacter(hex));
    }
}
