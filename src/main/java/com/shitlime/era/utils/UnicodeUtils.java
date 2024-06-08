package com.shitlime.era.utils;

public class UnicodeUtils {
    public static String unicodeToCharacter(String hex) {
        Integer i = Integer.parseInt(hex, 16);
        return new String(Character.toChars(i));
    }

    public static String unicodeToCharacter(int i) {
        return new String(Character.toChars(i));
    }

    public static String characterToUnicode(String character) {
        int[] codePoints = character.codePoints().toArray();
        if (codePoints.length != 1) {
            throw new IllegalArgumentException("Not single character.");
        }
        return Integer.toHexString(codePoints[0]);
    }

    public static String characterUnicodeBlock(String character) {
        int[] codePoints = character.codePoints().toArray();
        if (codePoints.length != 1) {
            throw new IllegalArgumentException("Not single character.");
        }
        // todo 使用更完整的unicode15.1区块信息 （当前Java21仅支持unicode15）
        return "" + Character.UnicodeBlock.of(codePoints[0]);
    }
}
