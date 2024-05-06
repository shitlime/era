package com.shitlime.era.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageSettingDTO {
    /**
     * 字体列表
     */
    private List<String> fontList;
    /**
     * 最大宽度 (px)
     */
    private int maxWidth;
    /**
     * 边界宽度 (px)
     */
    private int border;
    /**
     * 边界宽度 (px)
     */
    private String borderColor;
    /**
     * 上边距 (px)
     */
    private int paddingTop;
    /**
     * 右边距 (px)
     */
    private int paddingRight;
    /**
     * 下边距 (px)
     */
    private int paddingBottom;
    /**
     * 左边距 (px)
     */
    private int paddingLeft;
    /**
     * 字体大小
     */
    private int fontSize;
    /**
     * 前景颜色
     */
    private String color;
    /**
     * 背景色
     */
    private String backgroundColor;
    /**
     * 内容
     */
    private String content;
}
