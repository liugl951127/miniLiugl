package com.bank.dualrecord.crypto;

import java.nio.charset.StandardCharsets;

/**
 * 十六进制工具
 */
public final class HexUtil {

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private HexUtil() {
    }

    public static String toHex(byte[] data) {
        if (data == null) return null;
        char[] out = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int v = data[i] & 0xFF;
            out[i * 2] = HEX_CHARS[v >>> 4];
            out[i * 2 + 1] = HEX_CHARS[v & 0x0F];
        }
        return new String(out);
    }

    public static byte[] fromHex(String hex) {
        if (hex == null) return null;
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("hex length must be even");
        }
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("invalid hex char");
            }
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    public static String utf8ToHex(String s) {
        return toHex(s.getBytes(StandardCharsets.UTF_8));
    }
}
