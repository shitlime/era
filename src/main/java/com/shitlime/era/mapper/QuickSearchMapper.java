package com.shitlime.era.mapper;

import com.shitlime.era.pojo.entry.QuickSearch;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface QuickSearchMapper {
    public static final String tableName = "quick_search";

    /**
     * 创建表
     */
    public void createTable();

    /**
     * 删除表
     */
    public void dropTable();

    /**
     * 插入数据
     * @param quickSearch
     */
    public void insert(QuickSearch quickSearch);

    /**
     * 查询数据
     * @return
     */
    public List<QuickSearch> select(QuickSearch quickSearch);

    /**
     * 根据id删除数据
     * @param id
     */
    public void delete(Long id);

    /**
     * 根据用户id删除
     * @param userId
     */
    public void deleteByUser(Long userId);
}
