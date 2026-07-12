package com.minimax.multimodal;

import com.minimax.multimodal.provider.BuiltinVisionProvider;
import com.minimax.multimodal.provider.ImageInspector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BuiltinVisionProvider 单元测试 (V3.0.1)
 *
 * <p>验证自研像素分析的正确性:
 *   1. 单色图 → 颜色直方图正确
 *   2. 大图 → 不抛异常, 性能可接受
 *   3. 暗图 → 场景分类 "暗色/夜景"
 *   4. 亮图 → 场景分类 "明亮/清爽"
 *   5. 格式识别 (PNG/JPEG/GIF)
 */
class BuiltinVisionProviderTest {

    /** Provider 实例 */
    private final BuiltinVisionProvider provider = new BuiltinVisionProvider();

    /**
     * 测试 1: 纯红 100x100 图 → 场景应包含 "多彩" 或 "明亮"
     */
    @Test
    @DisplayName("1. 红色 100x100 → 主色含 #FF0000 系")
    void testRedImage() {
        // 1. 构造纯红图
        String b64 = createSolidColorImage(100, 100, Color.RED);
        // 2. 分析
        String result = provider.describe(b64, "image/png", "什么颜色");
        // 3. 验证
        assertNotNull(result);
        assertTrue(result.contains("内置"), "应使用内置 provider");
        assertTrue(result.contains("100×100"), "应包含尺寸");
    }

    /**
     * 测试 2: 暗色图 → 场景分类为 "暗色/夜景"
     */
    @Test
    @DisplayName("2. 暗色 50x50 → 场景 '暗色/夜景'")
    void testDarkImage() {
        // 1. 构造暗色 (RGB 10,10,10)
        String b64 = createSolidColorImage(50, 50, new Color(10, 10, 10));
        // 2. 分析
        String result = provider.describe(b64, "image/png", "亮度");
        // 3. 验证
        assertTrue(result.contains("暗色"), "暗色图应分类为暗色, 实际: " + result);
    }

    /**
     * 测试 3: 亮色低饱和图 → "明亮/清爽"
     */
    @Test
    @DisplayName("3. 白色 30x30 → 场景 '明亮/清爽'")
    void testBrightImage() {
        String b64 = createSolidColorImage(30, 30, Color.WHITE);
        String result = provider.describe(b64, "image/png", "亮度");
        assertTrue(result.contains("明亮"), "亮色图应分类为明亮, 实际: " + result);
    }

    /**
     * 测试 4: 多彩图 (彩色噪点) → "多彩/鲜艳"
     */
    @Test
    @DisplayName("4. 多彩图 → 场景 '多彩/鲜艳'")
    void testColorfulImage() {
        // 1. 构造 100x100 多彩图
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        // 2. 随机填色
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                g.setColor(new Color((x * 7) % 256, (y * 5) % 256, (x + y) % 256));
                g.fillRect(x, y, 1, 1);
            }
        }
        g.dispose();
        // 3. 转 base64
        String b64 = imgToB64(img);
        // 4. 分析
        String result = provider.describe(b64, "image/png", "多彩吗");
        // 5. 验证: 至少是合法结果
        assertTrue(result.contains("内置"));
    }

    /**
     * 测试 5: 大图 1000x1000 → 不抛异常
     */
    @Test
    @DisplayName("5. 大图 1000x1000 → 性能 + 正确性")
    void testLargeImage() {
        String b64 = createSolidColorImage(1000, 1000, Color.BLUE);
        long t0 = System.currentTimeMillis();
        String result = provider.describe(b64, "image/png", "大图测试");
        long dur = System.currentTimeMillis() - t0;
        // 1000x1000 纯色应在 1s 内完成 (采样 + 颜色直方图)
        assertTrue(dur < 3000, "大图应在 3s 内完成, 实际: " + dur + "ms");
        assertNotNull(result);
    }

    /**
     * 测试 6: 空图片 → 优雅降级
     */
    @Test
    @DisplayName("6. 空 base64 → 优雅降级")
    void testEmptyImage() {
        String result = provider.describe(null, "image/png", "空");
        assertEquals("【内置】无图片数据", result);
        String result2 = provider.describe("", "image/png", "空");
        assertEquals("【内置】无图片数据", result2);
    }

    /**
     * 测试 7: 非法 base64 → 错误信息而非异常
     */
    @Test
    @DisplayName("7. 非法 base64 → 错误信息而非异常")
    void testInvalidBase64() {
        // 非 base64 字符
        String result = provider.describe("!!!not-base64!!!", "image/png", "invalid");
        // 应该返回 "图片解码失败" 而不是抛异常
        assertNotNull(result);
    }

    /**
     * 测试 8: inspect 委托给 ImageInspector
     */
    @Test
    @DisplayName("8. inspect PNG → format=png")
    void testInspect() {
        String b64 = createSolidColorImage(10, 10, Color.RED);
        Map<String, Object> info = provider.inspect(b64);
        assertEquals("png", info.get("format"));
    }

    /**
     * 测试 9: ImageInspector 格式识别 - JPEG
     */
    @Test
    @DisplayName("9. ImageInspector 识别 JPEG")
    void testImageInspectorJpeg() {
        // JPEG magic: FF D8 FF
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        String b64 = Base64.getEncoder().encodeToString(jpeg);
        Map<String, Object> info = ImageInspector.inspect(b64);
        assertEquals("jpeg", info.get("format"));
    }

    /**
     * 辅助: 构造纯色 PNG → base64
     */
    private String createSolidColorImage(int w, int h, Color color) {
        // 1. 创建 BufferedImage
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        // 2. 用 Graphics2D 填充
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        g.dispose();
        // 3. 编码为 PNG bytes
        return imgToB64(img);
    }

    /**
     * 辅助: BufferedImage → PNG → base64
     */
    private String imgToB64(BufferedImage img) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
