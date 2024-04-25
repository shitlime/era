package com.shitlime.era.utils;

import com.shitlime.era.mapper.DatasetMapper;
import com.shitlime.era.pojo.entry.DataItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class DatasetUtils {
    @Autowired
    private DatasetMapper datasetMapper;

    /**
     * 比较两个值的大小
     * @param v1
     * @param v2
     * @return
     */
    public static int compareValues(String v1, String v2) {
        int i = v1.length() - v2.length();
        return i != 0? i : v1.compareTo(v2);
    }

    /**
     * 一次插入所有数据。分隔子列表保证数据长度不会超出SQL限制。
     * @param dataItems
     * @param tableName
     */
    public void insertAllData(ArrayList<DataItem> dataItems, String tableName) {
        final int step = 5000;
        for (int i = 0; i< dataItems.size(); i+=step) {
            int end = Math.min(i + step, dataItems.size());
            datasetMapper.insertList(tableName, dataItems.subList(i, end));
        }
    }
}
