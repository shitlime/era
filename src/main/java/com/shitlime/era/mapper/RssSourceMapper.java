package com.shitlime.era.mapper;

import com.shitlime.era.pojo.entry.RssSource;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RssSourceMapper {
    public static final String tableName = "rss_source";

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
     * @param rssSource
     */
    public void insert(RssSource rssSource);

    /**
     * 根据 id 查询
     * @param id
     */
    public RssSource selectById(Long id);

    /**
     * 根据 url 查询
     * @param url
     * @return
     */
    public RssSource selectByUrl(String url);

    /**
     * 根据 id 列表查询
     * @param ids
     * @return
     */
    public List<RssSource> selectByIds(List<Long> ids);

    /**
     * 根据 id 删除
     * @param id
     */
    public void delete(Long id);

    /**
     * 更新拉取信息
     */
    public void fetch(RssSource rssSource);
}
