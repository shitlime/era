package com.shitlime.era.enums;

/**
 * 数据集类型
 */
public enum DatasetTypeEnum {
    /**
     * 文本行类型的数据集。由 `data/xxxx.txt` 这样的文本文件提供数据，每一行为一个单位数据。
     */
    TXT_LINE,

    /**
     * 图片文件类型的数据集。由 `data/` 这样的文件夹提供数据，文件夹中的每一个文件为一个单位数据。
     */
    PIC_FILE,

    /**
     * 图片Zip包类型的数据集。由一个 `*.zip` 这样的压缩包文件提供数据，压缩包内部有一批图片，每张图片为一个单位数据。
     */
    PIC_ZIP,

    /**
     * unicode数据集（内部计算）
     */
    UNICODE,
}
