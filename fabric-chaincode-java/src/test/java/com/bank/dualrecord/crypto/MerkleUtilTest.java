package com.bank.dualrecord.crypto;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MerkleUtilTest {

    @Test
    void testEmptyInput() {
        assertEquals("", MerkleUtil.computeRoot(null));
        assertEquals("", MerkleUtil.computeRoot(java.util.Collections.emptyList()));
    }

    @Test
    void testSingleHash() {
        String h = "a1b2c3d4e5f6789012345678901234567890123456789012345678901234abcd";
        assertEquals(h, MerkleUtil.computeRoot(Arrays.asList(h)));
    }

    @Test
    void testTwoHashes() {
        List<String> hashes = Arrays.asList(
            "a1b2c3d4e5f6789012345678901234567890123456789012345678901234abcd",
            "b2c3d4e5f6789012345678901234567890123456789012345678901234abcdef"
        );
        String root = MerkleUtil.computeRoot(hashes);
        assertEquals(64, root.length());
    }

    @Test
    void testOddCount() {
        List<String> hashes = Arrays.asList(
            "a1b2c3d4e5f6789012345678901234567890123456789012345678901234abcd",
            "b2c3d4e5f6789012345678901234567890123456789012345678901234abcdef",
            "c3d4e5f6789012345678901234567890123456789012345678901234abcdef01"
        );
        String root = MerkleUtil.computeRoot(hashes);
        assertEquals(64, root.length());
    }

    @Test
    void testDeterministic() {
        List<String> hashes = Arrays.asList(
            "a1b2c3d4e5f6789012345678901234567890123456789012345678901234abcd",
            "b2c3d4e5f6789012345678901234567890123456789012345678901234abcdef"
        );
        String r1 = MerkleUtil.computeRoot(hashes);
        String r2 = MerkleUtil.computeRoot(hashes);
        assertEquals(r1, r2);
    }

    @Test
    void testDifferentInputs() {
        List<String> a = Arrays.asList(
            "a1b2c3d4e5f6789012345678901234567890123456789012345678901234abcd",
            "b2c3d4e5f6789012345678901234567890123456789012345678901234abcdef"
        );
        List<String> b = Arrays.asList(
            "a1b2c3d4e5f6789012345678901234567890123456789012345678901234abcd",
            "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
        );
        assertNotEquals(MerkleUtil.computeRoot(a), MerkleUtil.computeRoot(b));
    }

    @Test
    void testSm3Root() {
        List<String> hashes = Arrays.asList(
            "a1b2c3d4e5f6789012345678901234567890123456789012345678901234abcd",
            "b2c3d4e5f6789012345678901234567890123456789012345678901234abcdef"
        );
        String root = MerkleUtil.computeSm3Root(hashes);
        assertEquals(64, root.length());
    }
}
