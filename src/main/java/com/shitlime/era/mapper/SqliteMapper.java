package com.shitlime.era.mapper;

import com.shitlime.era.pojo.entry.SqliteMaster;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SqliteMapper {
    /**
     * 根据名称获取数据库表信息
     * @param tableName 表名
     * @return 表名或 null
     */
    @Select("select * from sqlite_master where type='table' and name=#{tableName}")
    public SqliteMaster getTableByName(String tableName);
}
