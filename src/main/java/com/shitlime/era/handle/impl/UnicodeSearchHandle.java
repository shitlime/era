package com.shitlime.era.handle.impl;

import com.shitlime.era.pojo.dto.Dataset;
import com.shitlime.era.handle.DatasetSearchHandle;
import com.shitlime.era.utils.UnicodeUtils;
import com.sun.tools.javac.Main;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Unicode搜索处理器
 */
@Slf4j
@Component
public class UnicodeSearchHandle implements DatasetSearchHandle {
    private List<Map<Map<Integer, Integer>, String>> unicodeBlockInfo;

    public void load() {
        load(null);
    }

    @Override
    public void load(Dataset dataset) {
        // 加载 Unicode 区块信息
        ClassLoader classLoader = Main.class.getClassLoader();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(
                        classLoader.getResourceAsStream("unicode/Blocks_cn.txt"))))
        ) {
            this.unicodeBlockInfo = reader.lines()
                    .filter(s -> (!s.isBlank() && !s.startsWith("#")))
                    .map(s -> {
                        String[] line = s.split(";");
                        String[] range = line[0].split("\\.\\.");
                        return Map.of(
                                Map.of(Integer.parseInt(range[0], 16),
                                        Integer.parseInt(range[1], 16)),
                                line[1].strip()
                        );
                    }).toList();
        } catch (IOException e) {
            log.error("读取 unicode/Blocks.txt 文件出错: {}", e.getMessage());
        }
    }

    @Override
    public String search(Dataset dataset, String character) {
        return String.format("U+%s (%s)",
                UnicodeUtils.characterToUnicode(character).toUpperCase(),
                characterUnicodeBlock(character));
    }

    @Override
    public String searchByHex(Dataset dataset, String hex) {
        return search(dataset, UnicodeUtils.unicodeToCharacter(hex));
    }

    public String searchCharacter(Dataset dataset, String hex) {
        return UnicodeUtils.unicodeToCharacter(hex);
    }

    /**
     * 使用资源文件中的 Blocks.txt 来查询字符的 Unicode 区块
     * Blocks.txt 来源： http://www.unicode.org/Public/UNIDATA/Blocks.txt
     *
     * @param character
     * @return
     */
    private String characterUnicodeBlock(String character) {
        int[] codePoints = character.codePoints().toArray();
        if (codePoints.length != 1) {
            throw new IllegalArgumentException("Not single character.");
        }
        int characterCode = codePoints[0];
        for (Map<Map<Integer, Integer>, String> ubi : unicodeBlockInfo) {
            for (Map.Entry<Map<Integer, Integer>, String> entry : ubi.entrySet()) {
                Map<Integer, Integer> range = entry.getKey();
                int start = range.keySet().iterator().next();
                int end = range.values().iterator().next();
                if (start <= characterCode && characterCode <= end) {
                    return entry.getValue();  // return block name
                }
            }
        }
        return "Unencoded | 未编码";
    }
}
