package com.minimax.ai.push.integration;

import lombok.extern.slf4j.Slf4j;

import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

/**
 * VAPID (Voluntary Application Server Identification) 工具类 (V3.5.1)
 *
 * <h3>背景</h3>
 * Web Push Protocol (RFC 8030) 要求推送服务器用 VAPID JWT 标识自己:
 *   - 公钥: 浏览器订阅时使用 (P-256 ECDH 曲线)
 *   - 私钥: 推送时签名 (ES256)
 *
 * <h3>算法</h3>
 * 1. 生成 ECDSA P-256 密钥对
 * 2. 公钥 → 65 字节 uncompressed point → Base64 URL-safe
 * 3. JWT header.payload.signature (ES256)
 *
 * <h3>复杂度</h3>
 * 密钥生成: ~50ms (冷启动); 签名: <5ms
 */
@Slf4j
public final class VapidUtil {

    private VapidUtil() {}

    /** VAPID 密钥对 */
    public record VapidKeyPair(String publicKeyBase64Url, String privateKeyBase64Url,
                                ECPublicKey publicKey, ECPrivateKey privateKey) {}

    /**
     * 生成新的 VAPID 密钥对 (P-256)
     */
    public static VapidKeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair kp = kpg.generateKeyPair();
            String pub = encodePublicKey((ECPublicKey) kp.getPublic());
            String priv = encodePrivateKey((ECPrivateKey) kp.getPrivate());
            return new VapidKeyPair(pub, priv,
                    (ECPublicKey) kp.getPublic(),
                    (ECPrivateKey) kp.getPrivate());
        } catch (Exception e) {
            throw new RuntimeException("VAPID 密钥生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 公钥 → Base64 URL (uncompressed point, 65 字节)
     */
    private static String encodePublicKey(ECPublicKey pub) {
        byte[] x = toFixedLength(pub.getW().getAffineX().toByteArray(), 32);
        byte[] y = toFixedLength(pub.getW().getAffineY().toByteArray(), 32);
        byte[] uncompressed = new byte[65];
        uncompressed[0] = 0x04;  // uncompressed point
        System.arraycopy(x, 0, uncompressed, 1, 32);
        System.arraycopy(y, 0, uncompressed, 33, 32);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(uncompressed);
    }

    /**
     * 私钥 → Base64 URL (32 字节)
     */
    private static String encodePrivateKey(ECPrivateKey priv) {
        byte[] s = toFixedLength(priv.getS().toByteArray(), 32);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s);
    }

    /**
     * BigInteger → 固定长度字节 (左补 0)
     */
    private static byte[] toFixedLength(byte[] src, int len) {
        if (src.length == len) return src;
        byte[] out = new byte[len];
        if (src.length > len) {
            // 去掉前导 0 字节
            System.arraycopy(src, src.length - len, out, 0, len);
        } else {
            System.arraycopy(src, 0, out, len - src.length, src.length);
        }
        return out;
    }

    /**
     * 生成 VAPID JWT (header.payload.signature)
     *
     * @param audience "https://fcm.googleapis.com" 或 push service origin
     * @param subject  mailto:admin@example.com
     * @param expiresAtSec JWT exp (秒)
     * @param kp VAPID 密钥对
     * @return JWT 字符串
     */
    public static String createJwt(String audience, String subject, long expiresAtSec, VapidKeyPair kp) {
        try {
            // Header: {"typ":"JWT","alg":"ES256"}
            String header = "{\"typ\":\"JWT\",\"alg\":\"ES256\"}";
            String headerB64 = base64UrlEncode(header.getBytes());
            // Payload: {"aud":"...","exp":...,"sub":"..."}
            String payload = String.format("{\"aud\":\"%s\",\"exp\":%d,\"sub\":\"%s\"}",
                    audience, expiresAtSec, subject);
            String payloadB64 = base64UrlEncode(payload.getBytes());
            // 签名
            String signingInput = headerB64 + "." + payloadB64;
            byte[] sig = signEs256(signingInput.getBytes(), kp.privateKey());
            String sigB64 = base64UrlEncode(sig);
            return signingInput + "." + sigB64;
        } catch (Exception e) {
            throw new RuntimeException("VAPID JWT 生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * ES256 签名 (SHA256withECDSA, P-256)
     */
    private static byte[] signEs256(byte[] data, ECPrivateKey priv) throws Exception {
        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initSign(priv);
        sig.update(data);
        byte[] der = sig.sign();
        // DER → JOSE 紧凑格式 (R || S, 各 32 字节)
        return derToJose(der);
    }

    /**
     * DER 签名 → JOSE R||S (各 32 字节)
     */
    private static byte[] derToJose(byte[] der) {
        // 解析 DER: 0x30 [totalLen] 0x02 [rLen] [r] 0x02 [sLen] [s]
        int idx = 2;  // 跳过 0x30 + totalLen
        if (der[1] >= 0x80) idx += (der[1] & 0x7F);  // long form
        idx++;  // 跳过 0x02
        int rLen = der[idx++] & 0xFF;
        byte[] r = new byte[rLen];
        System.arraycopy(der, idx, r, 0, rLen);
        idx += rLen;
        idx++;  // 跳过 0x02
        int sLen = der[idx++] & 0xFF;
        byte[] s = new byte[sLen];
        System.arraycopy(der, idx, s, 0, sLen);
        byte[] rPadded = stripLeadingZeros(r, 32);
        byte[] sPadded = stripLeadingZeros(s, 32);
        byte[] result = new byte[64];
        System.arraycopy(rPadded, 0, result, 0, 32);
        System.arraycopy(sPadded, 0, result, 32, 32);
        return result;
    }

    private static byte[] stripLeadingZeros(byte[] src, int len) {
        if (src.length == len) return src;
        byte[] out = new byte[len];
        int srcStart = Math.max(0, src.length - len);
        int copyLen = Math.min(len, src.length);
        System.arraycopy(src, srcStart, out, len - copyLen, copyLen);
        return out;
    }

    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
