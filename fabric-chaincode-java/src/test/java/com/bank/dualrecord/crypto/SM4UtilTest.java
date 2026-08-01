package com.bank.dualrecord.crypto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SM4UtilTest {

    @Test
    void testGenerateKey() {
        String key = SM4Util.generateKey();
        assertEquals(32, key.length(), "SM4 密钥应为 16 字节(32 hex)");
    }

    @Test
    void testEncryptDecrypt() {
        String key = SM4Util.generateKey();
        String plaintext = "身份证号: 110101199001011234";
        String ciphertext = SM4Util.encryptString(key, plaintext);
        assertNotNull(ciphertext);
        assertNotEquals(plaintext, ciphertext);
        String decrypted = SM4Util.decryptString(key, ciphertext);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void testEncryptProducesDifferentCiphertext() {
        // CBC 模式:相同明文 + 相同密钥 → 不同密文(IV 随机)
        String key = SM4Util.generateKey();
        String plaintext = "敏感数据";
        String c1 = SM4Util.encryptString(key, plaintext);
        String c2 = SM4Util.encryptString(key, plaintext);
        assertNotEquals(c1, c2, "CBC 模式 IV 随机,密文应不同");
    }

    @Test
    void testDecryptWithWrongKey() {
        String key1 = SM4Util.generateKey();
        String key2 = SM4Util.generateKey();
        String ciphertext = SM4Util.encryptString(key1, "data");
        assertThrows(RuntimeException.class, () -> SM4Util.decryptString(key2, ciphertext));
    }

    @Test
    void testEncryptLongData() {
        String key = SM4Util.generateKey();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) sb.append("ABCDEFGHIJ");
        String longText = sb.toString();
        String enc = SM4Util.encryptString(key, longText);
        String dec = SM4Util.decryptString(key, enc);
        assertEquals(longText, dec);
    }
}
