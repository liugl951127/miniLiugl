package com.bank.dualrecord.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HashUtilTest {

    @Test
    void testSha256() {
        String hash = HashUtil.sha256("hello");
        assertEquals(64, hash.length());
        // 已知 SHA-256("hello") = 2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hash);
    }

    @Test
    void testSha256Empty() {
        String hash = HashUtil.sha256("");
        assertEquals(64, hash.length());
        // SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
    }

    @Test
    void testIsValidSha256() {
        assertTrue(HashUtil.isValidSha256("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"));
        assertFalse(HashUtil.isValidSha256("invalid"));
        assertFalse(HashUtil.isValidSha256("ABCD"));
        assertFalse(HashUtil.isValidSha256(null));
    }

    @Test
    void testSm3() {
        String hash = HashUtil.sm3("test");
        assertEquals(64, hash.length());
    }

    @Test
    void testIsValidSm3() {
        assertTrue(HashUtil.isValidSm3("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"));
        assertFalse(HashUtil.isValidSm3("short"));
    }
}
