package com.bank.dualrecord.crypto;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.charset.StandardCharsets;
import java.security.Security;

/**
 * 国密 SM3 摘要算法
 *
 * <p>国标 GM/T 0004-2012,输出 256 bit(64 hex chars)
 * <p>Fabric 链码中链上指纹统一用 SM3
 *
 * @author Mavis
 */
public final class SM3Util {

    static {
        // 注册 BouncyCastle Provider(链码容器内)
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private SM3Util() {
    }

    /**
     * 计算 SM3 摘要
     *
     * @param data 原始字节
     * @return 32 字节摘要
     */
    public static byte[] hash(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }
        SM3Digest digest = new SM3Digest();
        digest.update(data, 0, data.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return out;
    }

    /**
     * 计算 SM3 摘要(十六进制)
     */
    public static String hashHex(byte[] data) {
        return HexUtil.toHex(hash(data));
    }

    /**
     * 计算字符串的 SM3 摘要
     */
    public static String hashHex(String text) {
        return hashHex(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 验证 SM3 摘要
     */
    public static boolean verify(byte[] data, String expectedHex) {
        if (data == null || expectedHex == null) {
            return false;
        }
        String actual = hashHex(data);
        return actual.equalsIgnoreCase(expectedHex);
    }

    /**
     * 验证字符串的 SM3 摘要
     */
    public static boolean verify(String text, String expectedHex) {
        if (text == null || expectedHex == null) {
            return false;
        }
        return verify(text.getBytes(StandardCharsets.UTF_8), expectedHex);
    }

    /**
     * 双 SM3 摘要(类似比特币 double-SHA256,部分场景要求)
     */
    public static String doubleHashHex(byte[] data) {
        return hashHex(hash(data));
    }
}
