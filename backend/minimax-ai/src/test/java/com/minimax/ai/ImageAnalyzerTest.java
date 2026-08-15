package com.minimax.ai;

import com.minimax.ai.embedding.SimpleEmbedding;
import com.minimax.ai.multimodal.ImageAnalyzer;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 图片分析器测试 (V2.6 多模态)
 */
class ImageAnalyzerTest {

    @Test
    void testAnalyzeSyntheticImage() throws Exception {
        // 生成 100x100 红色图片
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 100, 100);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] data = baos.toByteArray();

        ImageAnalyzer analyzer = new ImageAnalyzer(new SimpleEmbedding(null, null), null);
        ImageAnalyzer.ImageAnalysisResult r = analyzer.analyze(data, "test.png");

        assertEquals(100, r.width);
        assertEquals(100, r.height);
        assertEquals("png", r.format);
        assertEquals(64, r.embedding.length, "embedding 64 维");
        assertNotEquals(0, r.phash);
        assertTrue(r.durationMs >= 0);
    }

    @Test
    void testDominantColors() throws Exception {
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 50, 50);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] data = baos.toByteArray();

        ImageAnalyzer analyzer = new ImageAnalyzer(new SimpleEmbedding(null, null), null);
        ImageAnalyzer.ImageAnalysisResult r = analyzer.analyze(data, "blue.png");
        assertEquals(5, r.dominantColors.size());
    }

    @Test
    void testColorTone() throws Exception {
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 50, 50);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] data = baos.toByteArray();

        ImageAnalyzer analyzer = new ImageAnalyzer(new SimpleEmbedding(null, null), null);
        ImageAnalyzer.ImageAnalysisResult r = analyzer.analyze(data, "red.png");
        // 纯红图 = warm
        assertEquals("warm", r.colorTone);
    }

    @Test
    void testPHashSimilarity() throws Exception {
        ImageAnalyzer analyzer = new ImageAnalyzer(new SimpleEmbedding(null, null), null);

        // 完全相同的图
        BufferedImage img1 = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        img1.createGraphics().fillRect(0, 0, 50, 50);
        BufferedImage img2 = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        img2.createGraphics().fillRect(0, 0, 50, 50);

        ByteArrayOutputStream b1 = new ByteArrayOutputStream();
        ByteArrayOutputStream b2 = new ByteArrayOutputStream();
        ImageIO.write(img1, "png", b1);
        ImageIO.write(img2, "png", b2);

        ImageAnalyzer.ImageAnalysisResult r1 = analyzer.analyze(b1.toByteArray(), "a.png");
        ImageAnalyzer.ImageAnalysisResult r2 = analyzer.analyze(b2.toByteArray(), "b.png");
        int dist = analyzer.phashDistance(r1.phash, r2.phash);
        // 纯色图 pHash 应完全相同
        assertEquals(0, dist);
    }

    @Test
    void testVisualSimilarity() {
        ImageAnalyzer analyzer = new ImageAnalyzer(new SimpleEmbedding(null, null), null);
        double[] v1 = {0.1, 0.2, 0.3, 0.4};
        double[] v2 = {0.1, 0.2, 0.3, 0.4};
        double sim = analyzer.visualSimilarity(v1, v2);
        assertEquals(1.0, sim, 1e-6, "相同向量 cosine = 1");

        double[] v3 = {0.4, 0.3, 0.2, 0.1};
        double sim2 = analyzer.visualSimilarity(v1, v3);
        assertTrue(sim2 > 0 && sim2 < 1, "不同向量 cosine 介于 0-1");
    }

    @Test
    void testIsSupported() {
        ImageAnalyzer analyzer = new ImageAnalyzer(new SimpleEmbedding(null, null), null);
        assertTrue(analyzer.isSupported("test.jpg"));
        assertTrue(analyzer.isSupported("test.png"));
        assertTrue(analyzer.isSupported("test.gif"));
        assertFalse(analyzer.isSupported("test.exe"));
    }

    @Test
    void testRejectEmpty() {
        ImageAnalyzer analyzer = new ImageAnalyzer(new SimpleEmbedding(null, null), null);
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze(new byte[0], "x.png"));
        assertThrows(IllegalArgumentException.class, () -> analyzer.analyze(null, "x.png"));
    }
}
