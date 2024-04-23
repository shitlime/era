package com.shitlime.era.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;

public class FileUtils {
    /**
     * 计算文件的 base64 值
     * @param path
     * @return
     */
    public static String fileToBase64(String path) {
        try {
            BufferedInputStream stream = new BufferedInputStream(new FileInputStream(path));
            byte[] bytes = stream.readAllBytes();
            stream.close();
            Base64.Encoder encoder = Base64.getEncoder();
            return encoder.encodeToString(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
