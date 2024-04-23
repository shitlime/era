package com.shitlime.era.enums;

/**
 * 数据集键类型
 */
public enum KeyTypeEnum {
    // ======= 下面是 TXT LINE 专用 =======
    /**
     * 重复键合并
     */
    COMBINE,

    /**
     * 重复键取最短
     */
    UNIQUE_MIN,

    /**
     * 重复键取最长
     */
    UNIQUE_MAX,
    // ======== TXT LINE 专用结束 ========

    // ======= 下面是 PIC FILE 专用 =======
    /**
     * 文件使用unicode命名
     */
    UNICODE,

    /**
     * 文件使用字符命令
     */
    CHARACTER,
    // ======== PIC FILE 专用结束 ========
}
