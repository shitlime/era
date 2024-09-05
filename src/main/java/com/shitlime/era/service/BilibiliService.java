package com.shitlime.era.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.model.ArrayMsg;
import com.shitlime.era.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.lang.Math.min;

@Slf4j
@Service
public class BilibiliService {
    /**
     * 自动返回视屏基本信息
     *
     * @return
     */
    public List<ArrayMsg> autoGetVideoInfo(String videoId) {
        String videoInfoJson = getVideoInfoJson(videoId);
        JSONObject jsonObject = JSON.parseObject(videoInfoJson);
        switch (jsonObject.getInteger("code")) {
            case 0 -> {
                JSONObject data = jsonObject.getJSONObject("data");
                // 标题
                String title = data.getString("title");
                // 封面url
                String picUrl = data.getString("pic");
                // up主
                String ownerName = data.getJSONObject("owner").getString("name");
                // 分区
                String tname = data.getString("tname");
                // 播放量
                Integer view = data.getJSONObject("stat").getInteger("view");
                // 收藏
                Integer favorite = data.getJSONObject("stat").getInteger("favorite");
                // 投币
                Integer coin = data.getJSONObject("stat").getInteger("coin");
                // 点赞
                Integer like = data.getJSONObject("stat").getInteger("like");
                // 投稿
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String pubdate = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(data.getLong("pubdate")),
                        ZoneId.systemDefault()
                    ).format(formatter);
                // 简介
                String desc = data.getString("desc");
                // ID
                String aid = data.getString("aid");

                return ArrayMsgUtils.builder()
                        .text(title + "\n")
                        .img("base64://" + FileUtils.fileToBase64(URI.create(picUrl)))
                        .text("up主：" + ownerName + "\n")
                        .text("分区：" + tname + "\n")
                        .text("播放：" + view + " ")
                        .text("收藏：" + favorite + "\n")
                        .text("投币：" + coin + " ")
                        .text("点赞：" + like + "\n")
                        .text("投稿：" + pubdate + "\n")
                        .text("简介：" + desc.substring(0, min(desc.length(), 57)) + "\n")
                        .text("ID：" + aid + "\n")
                        .build();
            }
            case -400 -> {
                return ArrayMsgUtils.builder().text("请求错误").build();
            }
            case -403 -> {
                return ArrayMsgUtils.builder().text("权限不足").build();
            }
            case -404 -> {
                return ArrayMsgUtils.builder().text("无视频").build();
            }
            case 62002 -> {
                return ArrayMsgUtils.builder().text("稿件不可见").build();
            }
            case 62004 -> {
                return ArrayMsgUtils.builder().text("稿件审核中").build();
            }
            default -> {
                return ArrayMsgUtils.builder().text("解析失败，未知错误").build();
            }
        }
    }

    /**
     * 视频基本信息
     * 参考文档 https://github.com/SocialSisterYi/bilibili-API-collect/blob/master/docs/video/info.md
     *
     * @param id 传入BV号或av号字符串
     * @return json字符串
     */
    private String getVideoInfoJson(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException();
        }

        // 判断id类型
        // BV bv 可大写可小写
        // av 只能小写
        String param;
        if (id.startsWith("av")) {
            param = "?aid=" + Integer.parseInt(id.substring(2));
        } else if (id.startsWith("BV") || id.startsWith("bv")) {
            param = "?bvid=" + id;
        } else {
            throw new IllegalArgumentException();
        }

        // 访问api得到数据
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.bilibili.com/x/web-interface/wbi/view" + param))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
