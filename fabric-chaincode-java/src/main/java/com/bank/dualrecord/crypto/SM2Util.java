package com.bank.dualrecord.crypto;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.SM2Signer;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * 国密 SM2 椭圆曲线公钥密码
 *
 * <p>国标 GM/T 0003-2012,用于:
 * <ul>
 *   <li>数字签名(链上各方签名)
 *   <li>密钥协商
 *   <li>公钥加密
 * </ul>
 *
 * <p>曲线:sm2p256v1(NIST P-256 的中国标准变种)
 *
 * @author Mavis
 */
public final class SM2Util {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static final String CURVE_NAME = "sm2p256v1";
    public static final String ALGO_SIGN = "SM3withSM2";
    public static final String ALGO_PUBKEY = "EC";
    public static final String PROVIDER = BouncyCastleProvider.PROVIDER_NAME;

    private SM2Util() {
    }

    // ============================================================
    // 密钥生成
    // ============================================================

    /**
     * 生成 SM2 密钥对
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance(ALGO_PUBKEY, PROVIDER);
            gen.initialize(new ECGenParameterSpec(CURVE_NAME), new SecureRandom());
            return gen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("生成 SM2 密钥对失败", e);
        }
    }

    /**
     * 生成 SM2 密钥对(返回 hex)
     */
    public static SM2KeyPair generateKeyPairHex() {
        KeyPair kp = generateKeyPair();
        byte[] priv = kp.getPrivate().getEncoded();
        byte[] pub = kp.getPublic().getEncoded();
        return new SM2KeyPair(HexUtil.toHex(priv), HexUtil.toHex(pub));
    }

    // ============================================================
    // 签名
    // ============================================================

    /**
     * 用私钥签名(明文)
     *
     * @param privateKeyHex PKCS#8 编码的私钥 hex
     * @param data 原始数据
     * @return 签名 hex(DER 编码)
     */
    public static String sign(String privateKeyHex, byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }
        try {
            // 解析私钥
            byte[] privBytes = HexUtil.fromHex(privateKeyHex);
            KeyFactory kf = KeyFactory.getInstance(ALGO_PUBKEY, PROVIDER);
            PrivateKey privKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));

            // 签名
            Signature signer = Signature.getInstance(ALGO_SIGN, PROVIDER);
            signer.initSign(privKey);
            signer.update(data);
            byte[] sigBytes = signer.sign();
            return HexUtil.toHex(sigBytes);
        } catch (Exception e) {
            throw new RuntimeException("SM2 签名失败", e);
        }
    }

    /**
     * 用私钥签名字符串
     */
    public static String sign(String privateKeyHex, String text) {
        return sign(privateKeyHex, text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 用公钥验签
     *
     * @param publicKeyHex X.509 编码的公钥 hex
     * @param data 原始数据
     * @param signatureHex 签名 hex
     */
    public static boolean verify(String publicKeyHex, byte[] data, String signatureHex) {
        if (data == null || signatureHex == null) {
            return false;
        }
        try {
            byte[] pubBytes = HexUtil.fromHex(publicKeyHex);
            KeyFactory kf = KeyFactory.getInstance(ALGO_PUBKEY, PROVIDER);
            PublicKey pubKey = kf.generatePublic(new X509EncodedKeySpec(pubBytes));

            Signature verifier = Signature.getInstance(ALGO_SIGN, PROVIDER);
            verifier.initVerify(pubKey);
            verifier.update(data);
            return verifier.verify(HexUtil.fromHex(signatureHex));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 用公钥验签字符串
     */
    public static boolean verify(String publicKeyHex, String text, String signatureHex) {
        return verify(publicKeyHex, text.getBytes(StandardCharsets.UTF_8), signatureHex);
    }

    // ============================================================
    // 公钥加密(用于敏感数据上链)
    // ============================================================

    /**
     * SM2 公钥加密(C1C3C2 模式,BC 默认)
     */
    public static String encrypt(String publicKeyHex, byte[] plaintext) {
        try {
            byte[] pubBytes = HexUtil.fromHex(publicKeyHex);
            KeyFactory kf = KeyFactory.getInstance(ALGO_PUBKEY, PROVIDER);
            PublicKey pubKey = kf.generatePublic(new X509EncodedKeySpec(pubBytes));

            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(CURVE_NAME);
            ECDomainParameters domainParams = new ECDomainParameters(
                spec.getCurve(), spec.getG(), spec.getN(), spec.getH());
            ECPublicKeyParameters pubParams = new ECPublicKeyParameters(
                ((java.security.interfaces.ECPublicKey) pubKey).getW(), domainParams);

            SM2Engine engine = new SM2Engine();
            engine.init(true, new ParametersWithRandom(pubParams, new SecureRandom()));
            byte[] cipher = engine.processBlock(plaintext, 0, plaintext.length);
            return HexUtil.toHex(cipher);
        } catch (Exception e) {
            throw new RuntimeException("SM2 加密失败", e);
        }
    }

    /**
     * SM2 私钥解密
     */
    public static byte[] decrypt(String privateKeyHex, String ciphertextHex) {
        try {
            byte[] privBytes = HexUtil.fromHex(privateKeyHex);
            byte[] cipher = HexUtil.fromHex(ciphertextHex);

            KeyFactory kf = KeyFactory.getInstance(ALGO_PUBKEY, PROVIDER);
            PrivateKey privKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));

            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(CURVE_NAME);
            ECDomainParameters domainParams = new ECDomainParameters(
                spec.getCurve(), spec.getG(), spec.getN(), spec.getH());
            ECPrivateKeyParameters privParams = new ECPrivateKeyParameters(
                ((java.security.interfaces.ECPrivateKey) privKey).getS(), domainParams);

            SM2Engine engine = new SM2Engine();
            engine.init(false, privParams);
            return engine.processBlock(cipher, 0, cipher.length);
        } catch (Exception e) {
            throw new RuntimeException("SM2 解密失败", e);
        }
    }

    // ============================================================
    // 公钥格式转换
    // ============================================================

    /**
     * 从压缩公钥 hex(04 || X || Y 格式)解析出公钥对象
     */
    public static PublicKey parsePublicKey(String publicKeyHex) {
        try {
            byte[] pubBytes = HexUtil.fromHex(publicKeyHex);
            KeyFactory kf = KeyFactory.getInstance(ALGO_PUBKEY, PROVIDER);
            return kf.generatePublic(new X509EncodedKeySpec(pubBytes));
        } catch (Exception e) {
            throw new RuntimeException("解析公钥失败", e);
        }
    }

    /**
     * 从压缩点坐标构造公钥 hex(04+X+Y,共 130 字符)
     */
    public static String publicKeyToHex(PublicKey pubKey) {
        return HexUtil.toHex(pubKey.getEncoded());
    }

    /**
     * 从 hex 私钥获取公钥 hex(完整 04+X+Y 形式)
     */
    public static String derivePublicKeyHex(String privateKeyHex) {
        try {
            byte[] privBytes = HexUtil.fromHex(privateKeyHex);
            KeyFactory kf = KeyFactory.getInstance(ALGO_PUBKEY, PROVIDER);
            PrivateKey privKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));

            ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(CURVE_NAME);
            BigInteger d = ((java.security.interfaces.ECPrivateKey) privKey).getS();
            ECPoint Q = spec.getG().multiply(d);
            ECPrivateKeySpec privSpec = new ECPrivateKeySpec(d,
                new ECNamedCurveSpec(CURVE_NAME, spec.getCurve(), spec.getG(), spec.getN()));
            // 用私钥算公钥
            ECPublicKeySpec pubSpec = new ECPublicKeySpec(Q,
                new ECNamedCurveSpec(CURVE_NAME, spec.getCurve(), spec.getG(), spec.getN()));
            return publicKeyToHex(kf.generatePublic(pubSpec));
        } catch (Exception e) {
            throw new RuntimeException("从私钥派生公钥失败", e);
        }
    }

    /**
     * 密钥对封装
     */
    public static class SM2KeyPair {
        private final String privateKeyHex;
        private final String publicKeyHex;

        public SM2KeyPair(String privateKeyHex, String publicKeyHex) {
            this.privateKeyHex = privateKeyHex;
            this.publicKeyHex = publicKeyHex;
        }

        public String getPrivateKeyHex() { return privateKeyHex; }
        public String getPublicKeyHex() { return publicKeyHex; }
    }
}
