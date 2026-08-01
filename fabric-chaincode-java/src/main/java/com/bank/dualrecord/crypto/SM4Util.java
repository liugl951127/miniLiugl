package com.bank.dualrecord.crypto;

import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.SecureRandom;
import java.security.Security;

/**
 * 国密 SM4 对称加密
 *
 * <p>国标 GM/T 0002-2012,分组密码 128 bit
 * <p>链码中用于敏感字段(身份证 / 银行卡 / 影像 URL)加密
 *
 * <p>本实现:CBC 模式 + PKCS7 填充,IV 前置
 *
 * @author Mavis
 */
public final class SM4Util {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /** 块大小 16 字节 */
    public static final int BLOCK_SIZE = 16;

    private SM4Util() {
    }

    /**
     * 加密(输出 IV + Ciphertext 的 hex)
     *
     * @param keyHex 16 字节密钥的 hex
     * @param plaintext 明文
     * @return IV(16B) + 密文 hex
     */
    public static String encrypt(String keyHex, byte[] plaintext) {
        if (keyHex == null || keyHex.length() != 32) {
            throw new IllegalArgumentException("SM4 密钥必须为 16 字节(32 hex 字符)");
        }
        if (plaintext == null) {
            throw new IllegalArgumentException("plaintext cannot be null");
        }

        try {
            byte[] key = HexUtil.fromHex(keyHex);
            byte[] iv = new byte[BLOCK_SIZE];
            new SecureRandom().nextBytes(iv);

            SM4Engine engine = new SM4Engine();
            BlockCipherPadding padding = new PKCS7Padding();
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
                new CBCBlockCipher(engine), padding);

            cipher.init(true, new ParametersWithIV(new KeyParameter(key), iv));
            byte[] out = new byte[cipher.getOutputSize(plaintext.length)];
            int len = cipher.processBytes(plaintext, 0, plaintext.length, out, 0);
            len += cipher.doFinal(out, len);

            // 拼接 IV + 密文
            byte[] result = new byte[BLOCK_SIZE + len];
            System.arraycopy(iv, 0, result, 0, BLOCK_SIZE);
            System.arraycopy(out, 0, result, BLOCK_SIZE, len);
            return HexUtil.toHex(result);
        } catch (Exception e) {
            throw new RuntimeException("SM4 加密失败", e);
        }
    }

    /**
     * 加密字符串
     */
    public static String encryptString(String keyHex, String plaintext) {
        return encrypt(keyHex, plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * 解密
     *
     * @param keyHex 16 字节密钥的 hex
     * @param ciphertextHex IV(16B) + 密文 hex
     */
    public static byte[] decrypt(String keyHex, String ciphertextHex) {
        if (keyHex == null || keyHex.length() != 32) {
            throw new IllegalArgumentException("SM4 密钥必须为 16 字节");
        }
        if (ciphertextHex == null || ciphertextHex.length() < 32) {
            throw new IllegalArgumentException("密文过短");
        }

        try {
            byte[] key = HexUtil.fromHex(keyHex);
            byte[] all = HexUtil.fromHex(ciphertextHex);

            // 拆分 IV + 密文
            byte[] iv = new byte[BLOCK_SIZE];
            byte[] cipherText = new byte[all.length - BLOCK_SIZE];
            System.arraycopy(all, 0, iv, 0, BLOCK_SIZE);
            System.arraycopy(all, BLOCK_SIZE, cipherText, 0, cipherText.length);

            SM4Engine engine = new SM4Engine();
            BlockCipherPadding padding = new PKCS7Padding();
            PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
                new CBCBlockCipher(engine), padding);

            cipher.init(false, new ParametersWithIV(new KeyParameter(key), iv));
            byte[] out = new byte[cipher.getOutputSize(cipherText.length)];
            int len = cipher.processBytes(cipherText, 0, cipherText.length, out, 0);
            len += cipher.doFinal(out, len);

            byte[] result = new byte[len];
            System.arraycopy(out, 0, result, 0, len);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("SM4 解密失败", e);
        }
    }

    /**
     * 解密字符串
     */
    public static String decryptString(String keyHex, String ciphertextHex) {
        return new String(decrypt(keyHex, ciphertextHex), java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 生成随机 16 字节密钥
     */
    public static String generateKey() {
        byte[] key = new byte[BLOCK_SIZE];
        new SecureRandom().nextBytes(key);
        return HexUtil.toHex(key);
    }
}
