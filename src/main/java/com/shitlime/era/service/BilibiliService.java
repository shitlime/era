package com.shitlime.era.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
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
import java.util.StringJoiner;

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
                // 封面
                String picUrl = data.getString("pic");
                String picBase64 = FileUtils.fileToBase64(URI.create(picUrl));
                String cover = "[视频封面]";
                if (picBase64 != null) {
                    cover = "base64://" + picBase64;
                }
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
                String bvid = data.getString("bvid");

                // 查询跳过片段信息
                String segmentsJson = getVideoSkipSegmentsJson(bvid);
                if (segmentsJson != null) {
                    JSONArray segments = JSON.parseArray(segmentsJson);
                    long segmentsCount = segments.size();
                    StringJoiner segmentsInfo = new StringJoiner("\n");
                    for (Object obj : segments) {
                        JSONObject jo = (JSONObject) obj;
                        JSONArray seg = jo.getJSONArray("segment");
                        if (seg != null && seg.size() == 2) {
                            double start = seg.getDouble(0); // 开始时间
                            double end = seg.getDouble(1);   // 结束时间
                            segmentsInfo.add(formatTime(start) + " - " + formatTime(end));
                        }
                    }
                    if (segmentsCount > 0) {
                        return ArrayMsgUtils.builder()
                                .text("【⚠该视频可能含有广告⚠】\n" + title + "\n")
                                .img(cover)
                                .text("up主：" + ownerName + "\n")
                                .text("分区：" + tname + "\n")
                                .text("播放：" + view + " ")
                                .text("收藏：" + favorite + "\n")
                                .text("投币：" + coin + " ")
                                .text("点赞：" + like + "\n")
                                .text("投稿：" + pubdate + "\n")
                                .text("简介：" + desc.substring(0, min(desc.length(), 57)) + "\n")
                                .text("ID：av" + aid + "\n")
                                .text("⚠疑似广告片段(" + segmentsCount + ")：\n")
                                .text(segmentsInfo.toString())
                                .build();
                    }
                }

                return ArrayMsgUtils.builder()
                        .text(title + "\n")
                        .img(cover)
                        .text("up主：" + ownerName + "\n")
                        .text("分区：" + tname + "\n")
                        .text("播放：" + view + " ")
                        .text("收藏：" + favorite + "\n")
                        .text("投币：" + coin + " ")
                        .text("点赞：" + like + "\n")
                        .text("投稿：" + pubdate + "\n")
                        .text("简介：" + desc.substring(0, min(desc.length(), 57)) + "\n")
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
