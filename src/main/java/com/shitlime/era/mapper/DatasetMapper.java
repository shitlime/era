package com.shitlime.era.mapper;

import com.shitlime.era.pojo.entry.DataItem;
import lombok.NonNull;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DatasetMapper {
    /**
     * 创建表
     * @param tableName
     */
    public void createTable(String tableName);

    /**
     * 删除表
     * @param tableName
     */
    public void dropTable(String tableName);

    /**
     * 根据键查询数据
     * @param tableName
     * @param key
     * @return
     */
    public List<DataItem> selectByKey(String tableName, String key);

    /**
     * 插入多个数据
     * @param tableName
     * @param dataItems
     */
    public void insertList(String tableName, List<DataItem> dataItems);

    /**
     * 插入单个数据
     * @param tableName
     * @param dataItem
     */
    public void insert(String tableName, DataItem dataItem);

    /**
     * 根据key删除数据
     * @param tableName
     * @param key
     */
    public void deleteByKey(String tableName, String key);

    /**
     * 计算数据量
     * @param tableName
     */
    public Long count(String tableName);
}