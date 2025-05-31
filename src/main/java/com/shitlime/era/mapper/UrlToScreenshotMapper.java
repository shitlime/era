package com.shitlime.era.mapper;

import com.shitlime.era.pojo.entry.UrlToScreenshot;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UrlToScreenshotMapper {
    public static final String tableName = "url_to_screenshot";

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
     * @param urlToScreenshot
     */
    public void insert(UrlToScreenshot urlToScreenshot);

    /**
     * 条件查询数据
     * @param urlToScreenshot
     * @return
     */
    public List<UrlToScreenshot> select(UrlToScreenshot urlToScreenshot);

    /**
     * 根据id删除数据
     * @param id
     */
    public void delete(Long id);

    /**
     * 根据id启用规则
     * @param id
     */
    public void enable(Long id);

    /**
     * 根据id停用规则
     * @param id
     */
    public void disable(Long id);
}
