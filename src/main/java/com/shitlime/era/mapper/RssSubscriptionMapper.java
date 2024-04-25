package com.shitlime.era.mapper;

import com.shitlime.era.pojo.entry.RssSubscription;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RssSubscriptionMapper {
    public static final String tableName = "rss_subscribe";

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
     * @param rssSubscription
     */
    public void insert(RssSubscription rssSubscription);

    /**
     * 根据 uid, gid 展示
     * @param groupId
     * @param userId
     * @return
     */
    public List<RssSubscription> show(Long groupId, Long userId);

    /**
     * 根据id删除
     * @param id
     */
    public void delete(Long id);

    /**
     * 启用
     * @param id
     */
    public void enable(Long id);

    /**
     * 禁用
     * @param id
     */
    public void disable(Long id);

    /**
     * 获取所有启用的 source id
     */
    public List<Long> selectAllSourceId();

    public List<RssSubscription> selectBySourceId(Long sourceId);
}
