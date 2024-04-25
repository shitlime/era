package com.shitlime.era.utils;

import com.shitlime.era.mapper.SqliteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class TableUtils {
    @Autowired
    private SqliteMapper sqliteMapper;

    /**
     * 判断该数据库表是否存在
     * @param tableName
     * @return
     */
    public boolean isExist(String tableName) {
        return sqliteMapper.getTableByName(tableName) != null;
    }
}
