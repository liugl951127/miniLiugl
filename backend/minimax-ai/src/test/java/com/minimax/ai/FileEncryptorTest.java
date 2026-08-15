package com.minimax.ai;

import com.minimax.ai.compliance.FileEncryptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件加密器测试 (V2.6 合规)
 */
class FileEncryptorTest {

    @Test
    void testEncryptDecrypt() {
        FileEncryptor enc = new FileEncryptor();
        enc.init();

        String original = "Hello, MiniMax AI! This is a test message.";
        byte[] encrypted = enc.encrypt(original.getBytes());
        byte[] decrypted = enc.decrypt(encrypted);

        assertEquals(original, new String(decrypted));
    }

    @Test
    void testEncryptionNotEquals() {
        FileEncryptor enc = new FileEncryptor();
        enc.init();

        String original = "secret message";
        byte[] encrypted = enc.encrypt(original.getBytes());
        assertNotEquals(original, new String(encrypted), "加密后应与原文不同");
    }

    @Test
    void testIsEncrypted() {
        FileEncryptor enc = new FileEncryptor();
        enc.init();

        byte[] encrypted = enc.encrypt("test".getBytes());
        assertTrue(enc.isEncrypted(encrypted), "应识别为已加密");

        byte[] plain = "plain text".getBytes();
        assertFalse(enc.isEncrypted(plain), "应识别为未加密");
    }

    @Test
    void testBase64RoundTrip() {
        FileEncryptor enc = new FileEncryptor();
        enc.init();

        String original = "Secret data 123";
        String encrypted = enc.encryptToBase64(original);
        String decrypted = enc.decryptFromBase64(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void testEncryptEmpty() {
        FileEncryptor enc = new FileEncryptor();
        enc.init();

        byte[] empty = new byte[0];
        byte[] encrypted = enc.encrypt(empty);
        byte[] decrypted = enc.decrypt(encrypted);
        assertEquals(0, decrypted.length);
    }

    @Test
    void testEncryptLarge() {
        FileEncryptor enc = new FileEncryptor();
        enc.init();

        byte[] data = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }
        byte[] encrypted = enc.encrypt(data);
        byte[] decrypted = enc.decrypt(encrypted);
        assertArrayEquals(data, decrypted, "1MB 加解密应一致");
    }

    @Test
    void testWrongMagicRejected() {
        FileEncryptor enc = new FileEncryptor();
        enc.init();

        // 超过 16 字节的虚假数据, 但前 4 字节不是 MMX1
        byte[] fake = "ABCD1234567890ABCDEFGH".getBytes();
        Exception ex = assertThrows(RuntimeException.class, () -> enc.decrypt(fake));
        // 不检查具体 message, 只检查不抛错代表检测成功
        assertNotNull(ex, "应报错");
        // 检查 message 不是 null
        assertTrue(ex.getMessage() != null, "错误信息不能为空");
    }

    @Test
    void testDifferentIV() {
        FileEncryptor enc = new FileEncryptor();
        enc.init();

        String text = "same content";
        byte[] e1 = enc.encrypt(text.getBytes());
        byte[] e2 = enc.encrypt(text.getBytes());
        // GCM 模式: 每次随机 IV, 同样原文密文不同
        assertFalse(java.util.Arrays.equals(e1, e2), "GCM 模式: 同样原文每次密文应不同");
    }
}
