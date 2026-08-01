package com.bank.dualrecord.util;

import com.bank.dualrecord.crypto.SM3Util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 * 哈希工具
 */
public final class HashUtil {

    /** SHA-256 长度(64 hex) */
    public static final int SHA256_LEN = 64;
    /** SM3 长度(64 hex) */
    public static final int SM3_LEN = 64;

    private static final Pattern SHA256_PATTERN = Pattern.compile("^[0-9a-fA-F]{64}$");
    private static final Pattern SM3_PATTERN = Pattern.compile("^[0-9a-fA-F]{64}$");

    private HashUtil() {
    }

    /**
     * 计算 SHA-256
     */
    public static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return bytesToHex(md.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 不可用", e);
        }
    }

    public static String sha256(String text) {
        return sha256(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * 验证 SHA-256 格式
     */
    public static boolean isValidSha256(String hash) {
        return hash != null && SHA256_PATTERN.matcher(hash).matches();
    }

    /**
     * 验证 SM3 格式
     */
    public static boolean isValidSm3(String hash) {
        return hash != null && SM3_PATTERN.matcher(hash).matches();
    }

    /**
     * 验证通用 64 字符 hex(同时支持 SHA-256 / SM3)
     */
    public static boolean isValidHash(String hash) {
        return isValidSha256(hash);
    }

    /**
     * 计算 SM3
     */
    public static String sm3(byte[] data) {
        return SM3Util.hashHex(data);
    }

    public static String sm3(String text) {
        return SM3Util.hashHex(text);
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = "0123456789abcdef".charAt(v >>> 4);
            hexChars[j * 2 + 1] = "0123456789abcdef".charAt(v & 0x0F);
        }
        return new String(hexChars);
    }
}
