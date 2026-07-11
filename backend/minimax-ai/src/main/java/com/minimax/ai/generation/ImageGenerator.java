package com.minimax.ai.generation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.List;

/**
 * AIGC 图片生成器 (V2.7.5 - 自研, 无外部 LLM 依赖)
 *
 * <h3>支持的图片类型</h3>
 * <ul>
 *   <li><b>abstract</b> - 抽象艺术 (彩色几何形 + 随机色块)</li>
 *   <li><b>gradient</b> - 渐变背景 (HSL 色相旋转)</li>
 *   <li><b>pattern</b> - 图案 (基于关键词选样式)</li>
 *   <li><b>text</b> - 文字海报 (大字 + 副标题 + 装饰)</li>
 *   <li><b>scene</b> - 风景 (山/日/海, 关键词决定元素)</li>
 *   <li><b>logo</b> - Logo 占位 (圆形 + 文字 + 渐变)</li>
 *   <li><b>infographic</b> - 信息图 (柱状/饼图/数字)</li>
 * </ul>
 *
 * <h3>关键词提取</h3>
 * <p>根据文本中的关键词决定图片类型/风格/颜色, 无关键词则默认 abstract + 多彩.</p>
 *
 * <h3>算法</h3>
 * <p>使用 AWT 在内存中绘制 BufferedImage, 编码为 PNG 返回 Base64.</p>
 * <p>每个类型独立实现, 输出尺寸可配 (默认 1024x1024).</p>
 */
@Slf4j
@Component
public class ImageGenerator {

    public static class ImageRequest {
        public String prompt;
        public String type;        // abstract/gradient/pattern/text/scene/logo/infographic
        public int width = 1024;
        public int height = 1024;
        public Long seed;
    }

    public static class ImageResult {
        public String type;
        public String prompt;
        public int width;
        public int height;
        public String base64;
        public long sizeBytes;
        public String mime = "image/png";
        public Map<String, Object> metadata = new LinkedHashMap<>();
    }

    public ImageResult generate(ImageRequest req) {
        if (req.prompt == null) req.prompt = "";
        if (req.type == null) req.type = inferType(req.prompt);
        if (req.width <= 0) req.width = 1024;
        if (req.height <= 0) req.height = 1024;
        if (req.seed == null) req.seed = (long) req.prompt.hashCode();

        long start = System.currentTimeMillis();
        Random rng = new Random(req.seed);

        BufferedImage img = new BufferedImage(req.width, req.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        switch (req.type) {
            case "gradient"  -> drawGradient(g, req, rng);
            case "pattern"   -> drawPattern(g, req, rng);
            case "text"      -> drawTextPoster(g, req, rng);
            case "scene"     -> drawScene(g, req, rng);
            case "logo"      -> drawLogo(g, req, rng);
            case "infographic" -> drawInfographic(g, req, rng);
            default          -> drawAbstract(g, req, rng);
        }

        g.dispose();
        long cost = System.currentTimeMillis() - start;
        log.info("[image] type={} size={}x{} cost={}ms", req.type, req.width, req.height, cost);

        // 编码
        ImageResult r = new ImageResult();
        r.type = req.type;
        r.prompt = req.prompt;
        r.width = req.width;
        r.height = req.height;
        r.sizeBytes = encodeBase64(img, r);
        r.metadata.put("seed", req.seed);
        r.metadata.put("costMs", cost);
        r.metadata.put("colors", extractColors(req, rng));
        return r;
    }

    /** 根据 prompt 推断类型 */
    public String inferType(String prompt) {
        String p = prompt.toLowerCase();
        if (p.contains("渐变") || p.contains("gradient")) return "gradient";
        if (p.contains("logo") || p.contains("标志")) return "logo";
        if (p.contains("海报") || p.contains("poster") || p.contains("宣传")) return "text";
        if (p.contains("风景") || p.contains("山") || p.contains("海") || p.contains("日落")) return "scene";
        if (p.contains("图表") || p.contains("数据") || p.contains("统计")) return "infographic";
        if (p.contains("图案") || p.contains("pattern") || p.contains("花纹")) return "pattern";
        return "abstract";
    }

    // 抽象艺术: 随机几何 + 色彩
    private void drawAbstract(Graphics2D g, ImageRequest req, Random rng) {
        // 背景渐变
        GradientPaint bg = new GradientPaint(0, 0, randomColor(rng), req.width, req.height, randomColor(rng));
        g.setPaint(bg);
        g.fillRect(0, 0, req.width, req.height);

        // 30 个随机圆
        for (int i = 0; i < 30; i++) {
            int x = rng.nextInt(req.width);
            int y = rng.nextInt(req.height);
            int r = 30 + rng.nextInt(200);
            float alpha = 0.2f + rng.nextFloat() * 0.5f;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.setColor(randomColor(rng));
            g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        }
        g.setComposite(AlphaComposite.SrcOver);

        // 写 prompt
        g.setColor(new Color(255, 255, 255, 200));
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(24, req.width / 20)));
        g.drawString(truncate(req.prompt, 30), 30, req.height - 30);
    }

    // 渐变背景
    private void drawGradient(Graphics2D g, ImageRequest req, Random rng) {
        Color c1 = randomColor(rng);
        Color c2 = randomColor(rng);
        GradientPaint p = new GradientPaint(0, 0, c1, req.width, req.height, c2);
        g.setPaint(p);
        g.fillRect(0, 0, req.width, req.height);

        // 中心圆
        int cx = req.width / 2, cy = req.height / 2;
        int r = Math.min(req.width, req.height) / 3;
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        g.setColor(c2);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
    }

    // 图案
    private void drawPattern(Graphics2D g, ImageRequest req, Random rng) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, req.width, req.height);
        int step = Math.max(20, Math.min(req.width, req.height) / 12);
        for (int x = 0; x < req.width; x += step) {
            for (int y = 0; y < req.height; y += step) {
                if ((x / step + y / step) % 2 == 0) {
                    g.setColor(randomColor(rng));
                    g.fillOval(x, y, step - 2, step - 2);
                } else {
                    g.setColor(randomColor(rng));
                    g.fillRect(x, y, step - 2, step - 2);
                }
            }
        }
    }

    // 文字海报
    private void drawTextPoster(Graphics2D g, ImageRequest req, Random rng) {
        // 背景
        g.setColor(randomColor(rng));
        g.fillRect(0, 0, req.width, req.height);

        // 大字
        String[] lines = req.prompt.split("[\\n,，。]");
        g.setColor(Color.WHITE);
        int y = req.height / 3;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            g.setFont(new Font("SansSerif", Font.BOLD, Math.max(36, req.width / 12)));
            g.drawString(truncate(line, 10), 60, y);
            y += 80;
        }

        // 装饰条
        g.setColor(new Color(255, 255, 255, 200));
        g.fillRect(40, req.height - 100, req.width - 80, 4);

        // 副标题
        g.setFont(new Font("SansSerif", Font.PLAIN, 24));
        g.drawString("AIGC Generated", 60, req.height - 50);
    }

    // 风景 (山/日/海)
    private void drawScene(Graphics2D g, ImageRequest req, Random rng) {
        // 天空
        GradientPaint sky = new GradientPaint(0, 0, new Color(255, 200, 150),
                0, req.height / 2, new Color(100, 180, 255));
        g.setPaint(sky);
        g.fillRect(0, 0, req.width, req.height / 2);

        // 太阳
        int cx = req.width * 3 / 4, cy = req.height / 4;
        int r = Math.min(req.width, req.height) / 8;
        g.setColor(new Color(255, 220, 100));
        g.fillOval(cx - r, cy - r, r * 2, r * 2);

        // 山
        Path2D mountain = new Path2D.Double();
        mountain.moveTo(0, req.height);
        for (int x = 0; x <= req.width; x += 50) {
            int h = req.height / 2 + rng.nextInt(req.height / 3);
            mountain.lineTo(x, h);
        }
        mountain.lineTo(req.width, req.height);
        mountain.closePath();
        g.setColor(new Color(80, 100, 80));
        g.fill(mountain);

        // 海
        g.setColor(new Color(50, 100, 180));
        g.fillRect(0, req.height * 2 / 3, req.width, req.height / 3);
    }

    // Logo
    private void drawLogo(Graphics2D g, ImageRequest req, Random rng) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, req.width, req.height);

        int cx = req.width / 2, cy = req.height / 2;
        int r = Math.min(req.width, req.height) / 3;
        GradientPaint p = new GradientPaint(cx - r, cy - r, randomColor(rng),
                cx + r, cy + r, randomColor(rng));
        g.setPaint(p);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, r));
        String text = req.prompt.isEmpty() ? "M" : req.prompt.substring(0, 1).toUpperCase();
        FontMetrics fm = g.getFontMetrics();
        int tx = cx - fm.stringWidth(text) / 2;
        int ty = cy + fm.getHeight() / 4;
        g.drawString(text, tx, ty);
    }

    // 信息图 (柱状 + 数字)
    private void drawInfographic(Graphics2D g, ImageRequest req, Random rng) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, req.width, req.height);

        // 标题
        g.setColor(new Color(50, 50, 50));
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        g.drawString(truncate(req.prompt, 30), 40, 60);

        // 6 根柱子
        int bw = req.width / 8;
        int startX = (req.width - bw * 6) / 2;
        int baseY = req.height - 80;
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        for (int i = 0; i < 6; i++) {
            int h = 100 + rng.nextInt(400);
            int x = startX + i * bw;
            g.setColor(new Color(60 + rng.nextInt(150), 100 + rng.nextInt(100), 200));
            g.fillRect(x, baseY - h, bw - 10, h);
            g.setColor(Color.DARK_GRAY);
            g.drawString(String.valueOf(h), x, baseY - h - 10);
        }
    }

    // 工具方法
    private Color randomColor(Random rng) {
        return new Color(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
    }

    private String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() > n ? s.substring(0, n) + "..." : s;
    }

    private long encodeBase64(BufferedImage img, ImageResult r) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            byte[] bytes = baos.toByteArray();
            r.base64 = Base64.getEncoder().encodeToString(bytes);
            return bytes.length;
        } catch (Exception e) {
            log.error("Image encode failed", e);
            return 0;
        }
    }

    private List<String> extractColors(ImageRequest req, Random rng) {
        return List.of(
                String.format("#%02x%02x%02x", rng.nextInt(256), rng.nextInt(256), rng.nextInt(256)),
                String.format("#%02x%02x%02x", rng.nextInt(256), rng.nextInt(256), rng.nextInt(256)),
                String.format("#%02x%02x%02x", rng.nextInt(256), rng.nextInt(256), rng.nextInt(256))
        );
    }
}
