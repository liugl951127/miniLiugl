package com.minimax.ai;

import com.minimax.ai.compliance.DataMasker;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据脱敏测试 (V2.6 合规)
 */
class DataMaskerTest {

    private final DataMasker masker = new DataMasker();

    @Test
    void testMaskMobile() {
        String result = masker.maskMobile("联系方式 13812345678 是我的");
        assertTrue(result.contains("138****5678"));
        assertFalse(result.contains("13812345678"), "原手机号不应出现");
    }

    @Test
    void testMaskIdCard() {
        String result = masker.maskIdCard("身份证 110101199001011234 有效");
        // 18 位: 保留前 3 + 后 4 = 110***...***1234
        assertFalse(result.contains("110101199001011234"));
        assertTrue(result.contains("110"), "保留前 3");
        assertTrue(result.contains("1234"), "保留后 4");
    }

    @Test
    void testMaskEmail() {
        String result = masker.maskEmail("联系我 zhang.san@example.com");
        assertTrue(result.contains("z****@example.com") || result.contains("z***@example.com"));
    }

    @Test
    void testMaskBankCard() {
        String result = masker.maskBankCard("卡号 6222021234567890123");
        assertTrue(result.contains("6222"));
        assertTrue(result.contains("0123"));
    }

    @Test
    void testMaskIPv4Internal() {
        String result = masker.maskIPv4("服务器 192.168.1.100 端口 8080");
        assertTrue(result.contains("192.168.*.*"));
        assertFalse(result.contains("192.168.1.100"));
    }

    @Test
    void testMaskIPv4Public() {
        String result = masker.maskIPv4("客户端 8.8.8.8");
        // 公网 IP 不脱敏
        assertTrue(result.contains("8.8.8.8"));
    }

    @Test
    void testMaskName() {
        String result = masker.maskName("张三先生");
        assertTrue(result.contains("张*"));
    }

    @Test
    void testMaskPassword() {
        String result = masker.maskPassword("password=secret123&username=admin");
        assertTrue(result.contains("password=******"));
        assertFalse(result.contains("secret123"));
    }

    @Test
    void testMaskJWT() {
        String result = masker.maskJWT("Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature");
        assertTrue(result.contains("eyJ***"));
        assertFalse(result.contains("eyJhbGciOiJI"));
    }

    @Test
    void testContainsMobile() {
        assertTrue(masker.containsMobile("我的电话 13812345678"));
        assertFalse(masker.containsMobile("我的电话 12345"));
    }

    @Test
    void testContainsIdCard() {
        assertTrue(masker.containsIdCard("110101199001011234"));
        assertTrue(masker.containsIdCard("11010119900101123X"));
        assertFalse(masker.containsIdCard("12345"));
    }

    @Test
    void testMaskNull() {
        assertNull(masker.maskMobile(null));
        assertNull(masker.maskIdCard(null));
        assertNull(masker.maskEmail(null));
    }

    @Test
    void testFullMask() {
        String text = "用户张三 13812345678 邮箱 zhang@test.com 身份证 110101199001011234";
        String result = masker.mask(text);
        assertFalse(result.contains("13812345678"));
        assertFalse(result.contains("110101199001011234"));
        assertFalse(result.contains("zhang@test.com"));
    }
}
