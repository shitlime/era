package com.shitlime.era.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Slf4j
public class FileUtils {
    /**
     * 计算文件的 base64 值
     *
     * @param path
     * @return
     */
    public static String fileToBase64(String path) {
        try {
            BufferedInputStream stream = new BufferedInputStream(new FileInputStream(path));
            byte[] bytes = stream.readAllBytes();
            stream.close();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取并计算网络文件的 base64 值
     *
     * @param uri 网络地址（URL）
     * @return
     */
    public static String fileToBase64(URI uri) {
        for (int count = 1; count < 3; count++) {
            // 访问uri得到文件的数据
            // 转换成base64字符串返回
            try (HttpClient httpClient = HttpClient.newHttpClient()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri)
                        .build();
                // 发送请求并获取响应
                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    // 获取响应的字节数组
                    byte[] responseBody = response.body();

                    // 将字节数组编码为 Base64 字符串
                    return Base64.getEncoder().encodeToString(responseBody);
                } else {
                    log.warn("HTTP request failed with status code: {}", response.statusCode());
                }
            } catch (IOException | InterruptedException e) {
                log.warn(e.getMessage());
            }
        }
        return null;
    }
}
