
package com.bspark.comms.util;

public class HexUtils {

    /**
     * 바이트 배열을 16진수 문자열로 변환
     */
    public static String toHexString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }

    /**
     * 16진수 문자열을 바이트 배열로 변환
     */
    public static byte[] fromHexString(String hex) {
        if (hex == null || hex.isEmpty()) {
            return new byte[0];
        }

        hex = hex.replaceAll("\\s+", ""); // 공백 제거
        int length = hex.length();
        byte[] data = new byte[length / 2];

        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }

        return data;
    }

    /**
     * 단일 바이트를 16진수 문자열로 변환
     */
    public static String toHexString(byte b) {
        return String.format("0x%02X", b & 0xFF);
    }
}