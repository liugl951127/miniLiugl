package com.bank.dualrecord.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Merkle 树(双 SHA-256,Bitcoin 风格)
 *
 * <p>用于话术节点结果聚合,链上只存根,子节点存链下 / IPFS
 * <p>算法: H_AB = SHA256( SHA256(H_A || H_B) )
 *
 * @author Mavis
 */
public final class MerkleUtil {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private MerkleUtil() {
    }

    /**
     * 计算 Merkle 根
     *
     * @param hashes 子节点哈希列表(已 hex 化,64 字符)
     * @return Merkle 根(64 字符 hex)
     */
    public static String computeRoot(java.util.List<String> hashes) {
        if (hashes == null || hashes.isEmpty()) {
            return "";
        }
        if (hashes.size() == 1) {
            return hashes.get(0);
        }

        // 复制到可变列表
        java.util.List<String> layer = new java.util.ArrayList<>(hashes);

        // 奇数项自动复制最后一项(Bitcoin 风格)
        while (layer.size() > 1) {
            if (layer.size() % 2 != 0) {
                layer.add(layer.get(layer.size() - 1));
            }
            java.util.List<String> next = new java.util.ArrayList<>();
            for (int i = 0; i < layer.size(); i += 2) {
                String concat = layer.get(i) + layer.get(i + 1);
                next.add(doubleSha256Hex(concat));
            }
            layer = next;
        }
        return layer.get(0);
    }

    /**
     * 双 SHA-256
     *
     * @param hexConcatenated 两个 32 字节哈希的拼接(共 64 字符)
     */
    public static String doubleSha256Hex(String hexConcatenated) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] first = sha.digest(HexUtil.fromHex(hexConcatenated));
            byte[] second = sha.digest(first);
            return HexUtil.toHex(second);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 不可用", e);
        }
    }

    /**
     * 计算节点的 SM3 Merkle 根
     */
    public static String computeSm3Root(java.util.List<String> hashes) {
        if (hashes == null || hashes.isEmpty()) {
            return "";
        }
        if (hashes.size() == 1) {
            return hashes.get(0);
        }
        java.util.List<String> layer = new java.util.ArrayList<>(hashes);
        while (layer.size() > 1) {
            if (layer.size() % 2 != 0) {
                layer.add(layer.get(layer.size() - 1));
            }
            java.util.List<String> next = new java.util.ArrayList<>();
            for (int i = 0; i < layer.size(); i += 2) {
                byte[] a = HexUtil.fromHex(layer.get(i));
                byte[] b = HexUtil.fromHex(layer.get(i + 1));
                byte[] combined = new byte[a.length + b.length];
                System.arraycopy(a, 0, combined, 0, a.length);
                System.arraycopy(b, 0, combined, a.length, b.length);
                next.add(SM3Util.hashHex(combined));
            }
            layer = next;
        }
        return layer.get(0);
    }
}
