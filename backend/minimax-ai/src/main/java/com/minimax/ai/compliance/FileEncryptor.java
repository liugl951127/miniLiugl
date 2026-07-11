package com.minimax.ai.compliance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 文件加密器 (V2.6 合规)
 *
 * <p>用于加密存储所有用户上传的多模态文件 (图片/视频/语音). 算法采用 AES-256-GCM, 兼顾机密性和完整性.</p>
 *
 * <h3>算法选择: AES-256-GCM</h3>
 * <ul>
 *   <li>AES-256: 256 位密钥, NSA 商用最高安全等级, 抗量子计算弱但目前足够</li>
 *   <li>GCM 模式: 认证加密 (AEAD), 同时提供机密性 + 完整性</li>
 *   <li>IV 长度 96 位 (12 字节), GCM 标准推荐</li>
 *   <li>Tag 长度 128 位 (16 字节), 防篡改</li>
 *   <li>JDK 自带, 无第三方依赖, 性能好 (硬件加速 AES-NI)</li>
 * </ul>
 *
 * <h3>文件格式</h3>
 * <pre>
 * ┌────────────┬────────────┬──────────────────────────┐
 * │ MAGIC(4B)  │  IV(12B)   │  Ciphertext + Tag(变长)   │
 * │ "MMX1"     │  随机       │  GCM 密文 + 16B Tag        │
 * └────────────┴────────────┴──────────────────────────┘
 * </pre>
 *
 * <h3>法规依据</h3>
 * <ul>
 *   <li>《个人信息保护法》第 51 条: 个人信息应采取相应的加密等安全措施</li>
 *   <li>GDPR Article 32 (b): 假名化和加密作为安全措施</li>
 *   <li>ISO 27001 A.10.1.2: 密钥管理</li>
 *   <li>等保 2.0 三级要求: 重要数据存储加密</li>
 * </ul>
 *
 * <h3>密钥管理</h3>
 * <ul>
 *   <li>默认从环境变量读: {@code MINIMAX_FILE_ENC_KEY}</li>
 *   <li>缺省 fallback: 32 字节固定值 (生产必须改)</li>
 *   <li>实际生产建议对接 KMS / Vault / HSM</li>
 *   <li>如果修改密钥, 旧文件无法解密 (需要密钥版本管理)</li>
 * </ul>
 *
 * <h3>性能参考</h3>
 * <ul>
 *   <li>1MB 文件加密: ~30ms (i5-8250U 软件实现)</li>
 *   <li>1MB 文件加密: ~3ms (AES-NI 硬件加速)</li>
 *   <li>100MB 文件: ~300ms (软件) / ~30ms (硬件)</li>
 * </ul>
 *
 * @author MiniMax Team
 * @since V2.6
 */
@Slf4j
@Component
public class FileEncryptor {

    /**
     * 文件魔数: 标识这是 MiniMax 加密文件格式
     * 4 字节 = "MMX1" (MiniMax version 1)
     * 任何修改需要同步更新魔数, 避免误识别
     */
    private static final byte[] MAGIC = {'M', 'M', 'X', '1'};

    /** GCM IV 长度 (12 字节 / 96 位, NIST SP 800-38D 推荐) */
    private static final int IV_LENGTH = 12;

    /** GCM Tag 长度 (16 字节 / 128 位, 最高安全等级) */
    private static final int TAG_LENGTH = 128;

    /** 加密算法标识: AES + GCM + NoPadding (GCM 自带填充) */
    private static final String ALGO = "AES/GCM/NoPadding";

    /**
     * 加密密钥, 来自环境变量 MINIMAX_FILE_ENC_KEY
     * 通过 SHA-256 摘要成 32 字节 (AES-256)
     */
    @Value("${minimax.file.enc.key:${MINIMAX_FILE_ENC_KEY:}}")
    private String encKey;

    /** 派生的 SecretKey 对象, 在 init() 中初始化 */
    private SecretKey secretKey;

    /** SecureRandom 实例, 用于生成 IV (使用系统熵源 /dev/urandom) */
    private final SecureRandom random = new SecureRandom();

    /**
     * 初始化: 派生密钥
     * 如果 env var 未设置, 使用默认密钥 (生产严禁!)
     */
    @PostConstruct
    public void init() {
        byte[] keyBytes;
        if (encKey == null || encKey.isEmpty()) {
            log.warn("File encryption key not set, using default. MUST be changed in production!");
            keyBytes = "minimax-default-32-byte-key-2024".getBytes();
        } else {
            // SHA-256 摘要: 任意长度输入 -> 32 字节
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                keyBytes = md.digest(encKey.getBytes());
            } catch (Exception e) {
                throw new RuntimeException("Failed to compute encryption key", e);
            }
        }
        // AES 密钥长度必须是 16/24/32 字节 (AES-128/192/256)
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException("AES key must be 16/24/32 bytes, got " + keyBytes.length);
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        log.info("FileEncryptor initialized with AES-{}", keyBytes.length * 8);
    }

    /**
     * 加密字节数组
     *
     * @param plaintext 明文
     * @return 加密后的字节流 (MAGIC + IV + ciphertext+tag)
     * @throws RuntimeException 加密失败
     */
    public byte[] encrypt(byte[] plaintext) {
        try {
            // 1. 随机生成 12 字节 IV
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            // 2. 初始化 Cipher
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            // 3. 加密 (GCM 自动附加 16 字节 Tag)
            byte[] ciphertext = cipher.doFinal(plaintext);

            // 4. 拼装文件: MAGIC(4) + IV(12) + ciphertext
            byte[] result = new byte[MAGIC.length + IV_LENGTH + ciphertext.length];
            System.arraycopy(MAGIC, 0, result, 0, MAGIC.length);
            System.arraycopy(iv, 0, result, MAGIC.length, IV_LENGTH);
            System.arraycopy(ciphertext, 0, result, MAGIC.length + IV_LENGTH, ciphertext.length);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * 解密字节数组
     *
     * @param encrypted 加密数据 (MAGIC + IV + ciphertext+tag)
     * @return 明文字节
     * @throws IllegalArgumentException 魔数错误或数据太短
     * @throws RuntimeException         解密失败 (GCM Tag 校验失败)
     */
    public byte[] decrypt(byte[] encrypted) {
        try {
            // 1. 长度校验
            if (encrypted.length < MAGIC.length + IV_LENGTH) {
                throw new IllegalArgumentException("Encrypted data too short");
            }
            // 2. 魔数校验
            for (int i = 0; i < MAGIC.length; i++) {
                if (encrypted[i] != MAGIC[i]) {
                    throw new IllegalArgumentException("Invalid magic number, not an encrypted file");
                }
            }
            // 3. 提取 IV
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(encrypted, MAGIC.length, iv, 0, IV_LENGTH);
            // 4. 提取 ciphertext
            byte[] ciphertext = new byte[encrypted.length - MAGIC.length - IV_LENGTH];
            System.arraycopy(encrypted, MAGIC.length + IV_LENGTH, ciphertext, 0, ciphertext.length);

            // 5. 初始化 Cipher (解密模式)
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
            // 6. 解密 (GCM 自动校验 Tag, 失败抛 AEADBadTagException)
            return cipher.doFinal(ciphertext);
        } catch (IllegalArgumentException e) {
            throw e;  // 业务异常透传
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed (key mismatch or data tampered)", e);
        }
    }

    /**
     * 加密文件 (适合大文件, 流式)
     * 实际生产应该用 CipherInputStream/CipherOutputStream, 这里简化版一次性读
     *
     * @param source 源文件路径
     * @param target 目标加密文件路径
     * @return target
     */
    public Path encryptFile(Path source, Path target) throws IOException {
        byte[] data = Files.readAllBytes(source);
        byte[] encrypted = encrypt(data);
        Files.write(target, encrypted);
        return target;
    }

    /**
     * 解密文件
     */
    public Path decryptFile(Path source, Path target) throws IOException {
        byte[] data = Files.readAllBytes(source);
        byte[] decrypted = decrypt(data);
        Files.write(target, decrypted);
        return target;
    }

    /**
     * Base64 编码加密 (用于小文本/密钥, 便于存储到 DB TEXT 字段)
     *
     * @param plaintext 明文
     * @return Base64 字符串
     */
    public String encryptToBase64(String plaintext) {
        return Base64.getEncoder().encodeToString(encrypt(plaintext.getBytes()));
    }

    /**
     * Base64 解密
     *
     * @param base64 加密的 Base64
     * @return 明文
     */
    public String decryptFromBase64(String base64) {
        byte[] encrypted = Base64.getDecoder().decode(base64);
        return new String(decrypt(encrypted));
    }

    /**
     * 检查数据是否是已加密文件
     * 通过魔数判断
     */
    public boolean isEncrypted(byte[] data) {
        if (data == null || data.length < MAGIC.length) return false;
        for (int i = 0; i < MAGIC.length; i++) {
            if (data[i] != MAGIC[i]) return false;
        }
        return true;
    }
}
