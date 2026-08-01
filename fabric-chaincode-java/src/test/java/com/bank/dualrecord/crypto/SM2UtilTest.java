package com.bank.dualrecord.crypto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SM2 单元测试
 *
 * <p>覆盖:密钥生成、签名、验签
 */
class SM2UtilTest {

    @Test
    void testKeyPairGeneration() {
        SM2Util.SM2KeyPair kp = SM2Util.generateKeyPairHex();
        assertNotNull(kp.getPrivateKeyHex());
        assertNotNull(kp.getPublicKeyHex());
        assertTrue(kp.getPrivateKeyHex().length() > 100);
        assertTrue(kp.getPublicKeyHex().length() > 100);
    }

    @Test
    void testSignAndVerify() {
        SM2Util.SM2KeyPair kp = SM2Util.generateKeyPairHex();
        String data = "重要合同内容 - 不能篡改";
        String signature = SM2Util.sign(kp.getPrivateKeyHex(), data);
        assertNotNull(signature);
        assertTrue(signature.length() > 100, "签名长度应 > 100 hex 字符");
        assertTrue(SM2Util.verify(kp.getPublicKeyHex(), data, signature));
    }

    @Test
    void testVerifyWithWrongData() {
        SM2Util.SM2KeyPair kp = SM2Util.generateKeyPairHex();
        String signature = SM2Util.sign(kp.getPrivateKeyHex(), "原始数据");
        assertFalse(SM2Util.verify(kp.getPublicKeyHex(), "篡改数据", signature));
    }

    @Test
    void testVerifyWithWrongKey() {
        SM2Util.SM2KeyPair kp1 = SM2Util.generateKeyPairHex();
        SM2Util.SM2KeyPair kp2 = SM2Util.generateKeyPairHex();
        String signature = SM2Util.sign(kp1.getPrivateKeyHex(), "data");
        assertFalse(SM2Util.verify(kp2.getPublicKeyHex(), "data", signature));
    }

    @Test
    void testDerivePublicKey() {
        SM2Util.SM2KeyPair kp = SM2Util.generateKeyPairHex();
        String derived = SM2Util.derivePublicKeyHex(kp.getPrivateKeyHex());
        assertNotNull(derived);
        // 派生的公钥应能验证原私钥的签名
        String sig = SM2Util.sign(kp.getPrivateKeyHex(), "test");
        assertTrue(SM2Util.verify(derived, "test", sig));
    }
}
