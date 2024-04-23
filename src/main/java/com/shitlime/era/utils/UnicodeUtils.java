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
        return Character.UnicodeBlock.of(codePoints[0]).toString();
    }
}
