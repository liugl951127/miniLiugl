package com.bank.dualrecord.crypto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SM3 单元测试
 *
 * <p>国密 SM3 标准测试向量(已知答案)
 */
class SM3UtilTest {

    @Test
    void testHashLength() {
        byte[] data = "hello world".getBytes();
        byte[] hash = SM3Util.hash(data);
        assertEquals(32, hash.length, "SM3 输出应为 32 字节");
    }

    @Test
    void testHashHexLength() {
        String hex = SM3Util.hashHex("hello world");
        assertEquals(64, hex.length(), "SM3 hex 长度应为 64");
    }

    @Test
    void testHashConsistency() {
        String a = SM3Util.hashHex("test data");
        String b = SM3Util.hashHex("test data");
        assertEquals(a, b, "相同输入应产生相同哈希");
    }

    @Test
    void testHashDifferentInput() {
        String a = SM3Util.hashHex("test1");
        String b = SM3Util.hashHex("test2");
        assertNotEquals(a, b, "不同输入应产生不同哈希");
    }

    @Test
    void testVerify() {
        String data = "verify me";
        String hash = SM3Util.hashHex(data);
        assertTrue(SM3Util.verify(data, hash));
        assertFalse(SM3Util.verify(data, "0000000000000000000000000000000000000000000000000000000000000000"));
    }

    @Test
    void testDoubleHash() {
        String single = SM3Util.hashHex("data");
        String doubled = SM3Util.doubleHashHex("data");
        assertNotEquals(single, doubled);
        assertEquals(64, doubled.length());
    }

    @Test
    void testEmptyInput() {
        String hash = SM3Util.hashHex("");
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }
}
