package com.shitlime.era.handle;

import com.shitlime.era.common.Dataset;

public interface DatasetSearchHandle {
    /**
     * 装载数据集对应数据库
     * @param dataset
     */
    public void load(Dataset dataset);

    /**
     * 查询数据集
     * @param character 单个字符
     */
    public <T> T search(Dataset dataset, String character);

    /**
     * 查询数据集
     * @param hex unicode
     */
    public <T> T searchByHex(Dataset dataset, String hex);
}
