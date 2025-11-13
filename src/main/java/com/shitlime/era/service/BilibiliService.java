package com.shitlime.era.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mikuac.shiro.common.utils.ArrayMsgUtils;
import com.mikuac.shiro.model.ArrayMsg;
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
import java.util.StringJoiner;

import static java.lang.Math.min;

@Slf4j
@Service
public class BilibiliService {
    private final ObjectMapper objectMapper;

    public BilibiliService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 自动返回视屏基本信息
     *
     * @return
     */
    public List<ArrayMsg> autoGetVideoInfo(String videoId) {
        try {
            String videoInfoJson = getVideoInfoJson(videoId);
            JsonNode rootNode = objectMapper.readTree(videoInfoJson);
            switch (rootNode.path("code").asInt()) {
                case 0 -> {
                    ArrayMsgUtils msgBuilder = ArrayMsgUtils.builder();
                    JsonNode data = rootNode.path("data");
                    // 标题
                    String title = data.path("title").asText();
                    // 封面
                    String picUrl = data.path("pic").asText();
                    // up主
                    String ownerName = data.path("owner").path("name").asText();
                    // 分区
                    String tname = data.path("tname").asText();
                    // 播放量
                    Integer view = data.path("stat").path("view").asInt();
                    // 收藏
                    Integer favorite = data.path("stat").path("favorite").asInt();
                    // 投币
                    Integer coin = data.path("stat").path("coin").asInt();
                    // 点赞
                    Integer like = data.path("stat").path("like").asInt();
                    // 投稿
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    String pubdate = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(data.path("pubdate").asLong()),
                            ZoneId.systemDefault()
                    ).format(formatter);
                    // 简介
                    String desc = data.path("desc").asText();
                    // ID
                    String aid = data.path("aid").asText();
                    String bvid = data.path("bvid").asText();
                    // 查询跳过片段信息
                    String segmentsJson = getVideoSkipSegmentsJson(bvid);
                    if (segmentsJson != null) {
                        JsonNode segments = objectMapper.readTree(segmentsJson);
                        int adSegCount = 0;
                        StringJoiner segmentsInfo = new StringJoiner("\n");
                        for (JsonNode jn : segments) {
                            // 广告
                            if ("sponsor".equals(jn.path("category").asText())) {
                                adSegCount++;
                                JsonNode seg = jn.path("segment");
                                if (seg != null && seg.size() == 2) {
                                    double start = seg.path(0).asDouble(); // 开始时间
                                    double end = seg.path(1).asDouble();   // 结束时间
                                    segmentsInfo.add(formatTime(start) + " - " + formatTime(end));
                                }
                            }
                        }
                        if (adSegCount > 0) {
                            msgBuilder.text("【⚠该视频被标记广告⚠】\n")
                                    .text("⚠标记广告片段(" + adSegCount + ")：\n")
                                    .text(segmentsInfo.toString() + "\n\n");
                        }
                    }
                    // 警告信息
                    String argueMsg = data.path("argue_info").path("argue_msg").asText();
                    if (!argueMsg.isBlank()) {
                        msgBuilder.text("⚠" + argueMsg + "\n");
                    }

                    return msgBuilder
                            .text("「" + title + "」\n")
                            .img(picUrl)
                            .text("up主：" + ownerName + "\n")
                            .text("分区：" + tname + "\n")
                            .text("播放：" + String.format("%7d", view) + "　")
                            .text("收藏：" + String.format("%7d", favorite) + "\n")
                            .text("投币：" + String.format("%7d", coin) + "　")
                            .text("点赞：" + String.format("%7d", like) + "\n")
                            .text("投稿：" + pubdate + "\n")
                            .text("简介：" + desc.substring(0, min(desc.length(), 100)) + "\n")
                            .text("ID：av" + aid)
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
                case 62012 -> {
                    return ArrayMsgUtils.builder().text("仅UP主自己可见").build();
                }
            }
        } catch (IOException e) {
            log.error(e.toString());
        }
        return ArrayMsgUtils.builder().text("解析失败，发生错误").build();
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

    /**
     * 获取跳过片段信息
     * 参考文档 https://github.com/hanydd/BilibiliSponsorBlock/wiki/API
     *
     * @param id 传入视频的BV号字符串
     * @return json字符串
     */
    private String getVideoSkipSegmentsJson(String id) {
        // 访问api得到数据
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://bsbsb.top/api/skipSegments?videoID=" + id))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            }
            return null;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将秒数转换为时:分:秒格式
     *
     * @param seconds 秒数
     * @return 时:分:秒格式的字符串
     */
    private static String formatTime(double seconds) {
        int totalSeconds = (int) seconds; // 取整
        int hours = totalSeconds / 3600;  // 计算小时
        int minutes = (totalSeconds % 3600) / 60; // 计算分钟
        int secs = totalSeconds % 60; // 计算秒数

        // 格式化输出
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
}
