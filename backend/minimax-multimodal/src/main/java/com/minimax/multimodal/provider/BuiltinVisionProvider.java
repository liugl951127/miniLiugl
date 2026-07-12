package com.minimax.multimodal.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内置自研视觉提供者 (V3.0.1) - 零外部依赖
 *
 * <p>基于 Java AWT 的纯像素分析, 无需任何 AI 模型/外部 API:
 *   - 解析图片尺寸/格式
 *   - RGB 颜色直方图 (Top 5 主色)
 *   - 亮度/对比度/饱和度统计
 *   - 简单场景分类 (亮/暗/多彩/单色)
 *
 * <p>使用场景:
 *   - 离线环境 (无外网)
 *   - 隐私场景 (数据不出本地)
 *   - 快速元信息提取 (不要描述, 只要统计)
 *   - 单元测试 baseline
 *
 * <h3>算法复杂度</h3>
 * O(W × H) — 扫描所有像素
 *
 * <h3>扩展方向</h3>
 * 接入 ONNX Runtime Java 加载真实视觉模型 (V3.1+ 计划)
 */
@Slf4j
@Component
public class BuiltinVisionProvider implements MultimodalModelProvider {

    /** Provider 名 */
    @Override
    public String name() {
        return "builtin";
    }

    @Override
    public String description() {
        return "内置自研像素分析 (零依赖, 颜色直方图 + 亮度对比度 + 场景分类)";
    }

    /** 内置永远就绪 (只要 JRE 支持 AWT) */
    @Override
    public boolean isReady() {
        return true;
    }

    /**
     * 主入口: 生成图片的结构化描述
     */
    @Override
    public String describe(String imageBase64, String mimeType, String prompt) {
        // 1. 空值保护
        if (imageBase64 == null || imageBase64.isBlank()) {
            return "【内置】无图片数据";
        }
        try {
            // 2. 解析图片为 BufferedImage
            BufferedImage img = decode(imageBase64, mimeType);
            if (img == null) {
                return "【内置】图片解码失败";
            }

            // 3. 调用各分析模块
            Map<String, Object> stats = computeStats(img);     // 基础统计
            String scene = classifyScene(stats);                // 场景分类
            String colors = topColors(img, 5);                  // 主色板

            // 4. 拼装结构化描述
            StringBuilder sb = new StringBuilder();
            sb.append("【内置自研视觉分析】\n");
            sb.append("尺寸: ").append(img.getWidth()).append("×").append(img.getHeight()).append(" px\n");
            sb.append("场景: ").append(scene).append("\n");
            sb.append("亮度: ").append(formatDouble(stats.get("brightness"))).append("/255\n");
            sb.append("对比度: ").append(formatDouble(stats.get("contrast"))).append("\n");
            sb.append("饱和度: ").append(formatDouble(stats.get("saturation"))).append("\n");
            sb.append("Top 5 主色: ").append(colors).append("\n");
            if (prompt != null && !prompt.isBlank()) {
                sb.append("用户提示: ").append(prompt).append("\n");
            }
            sb.append("\n(此为像素统计, 未使用神经网络; 详细场景理解请切换 openai/local provider)");
            return sb.toString();
        } catch (Exception e) {
            // 5. 异常降级
            log.warn("[builtin] describe failed: {}", e.getMessage());
            return "【内置】分析失败: " + e.getMessage();
        }
    }

    /**
     * 解码 base64 → BufferedImage
     *
     * @param b64  base64 字符串
     * @param mime MIME 类型 (image/png, image/jpeg 等)
     * @return BufferedImage, 失败返回 null
     */
    private BufferedImage decode(String b64, String mime) throws Exception {
        // 1. base64 解码
        byte[] bytes = Base64.getDecoder().decode(b64);
        // 2. 用 ImageIO 读取 (内置支持 png/jpeg/gif/bmp)
        //    ByteArrayInputStream 把字节数组包装成流
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    /**
     * 计算图片统计: 亮度 / 对比度 / 饱和度
     *
     * <p>算法:
     *   - 亮度: 像素 (R+G+B)/3 的平均值
     *   - 对比度: 像素亮度标准差
     *   - 饱和度: HSL 空间中 S 分量的平均值
     *
     * @param img BufferedImage
     * @return { brightness, contrast, saturation }
     */
    private Map<String, Object> computeStats(BufferedImage img) {
        // 1. 获取尺寸
        int w = img.getWidth();
        int h = img.getHeight();
        // 2. 防御: 0 尺寸
        if (w <= 0 || h <= 0) {
            return Map.of("brightness", 0.0, "contrast", 0.0, "saturation", 0.0);
        }

        // 3. 累加器
        long sumBright = 0;        // 亮度总和
        long sumBrightSq = 0;      // 亮度平方总和 (用于标准差)
        long sumSat = 0;            // 饱和度总和
        int pixelCount = 0;         // 像素总数

        // 4. 遍历所有像素 (为了性能, 隔行采样)
        //    大图不必每像素都看, 采样也能反映整体
        int step = Math.max(1, Math.min(w, h) / 200);  // 采样步长 (max 200x200 采样)
        for (int y = 0; y < h; y += step) {            // 步长遍历 y
            for (int x = 0; x < w; x += step) {        // 步长遍历 x
                // 5. 取 RGB
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;            // 提取 R (高 8 位)
                int g = (rgb >> 8) & 0xFF;             // 提取 G (中 8 位)
                int b = rgb & 0xFF;                    // 提取 B (低 8 位)
                // 6. 亮度: 简单平均 (Rec.601 公式: 0.299R + 0.587G + 0.114B 更准确, 这里用平均简化)
                int bright = (r + g + b) / 3;
                sumBright += bright;                   // 累加
                sumBrightSq += (long) bright * bright; // 平方累加
                // 7. 饱和度: max - min (HSL 中 S 简化版)
                int max = Math.max(r, Math.max(g, b));
                int min = Math.min(r, Math.min(g, b));
                int sat = (max == 0) ? 0 : ((max - min) * 255 / max);  // 0-255
                sumSat += sat;                          // 累加
                pixelCount++;                           // 计数
            }
        }

        // 8. 防御: 0 像素 (空图)
        if (pixelCount == 0) {
            return Map.of("brightness", 0.0, "contrast", 0.0, "saturation", 0.0);
        }

        // 9. 计算平均值
        double avgBright = (double) sumBright / pixelCount;        // 平均亮度
        double avgSat = (double) sumSat / pixelCount;                // 平均饱和度
        // 10. 计算标准差 (对比度)
        double variance = ((double) sumBrightSq / pixelCount) - (avgBright * avgBright);  // 方差
        double stddev = variance > 0 ? Math.sqrt(variance) : 0;     // 标准差

        // 11. 返回结果
        Map<String, Object> out = new HashMap<>();
        out.put("brightness", avgBright);    // 平均亮度
        out.put("contrast", stddev);          // 标准差作为对比度
        out.put("saturation", avgSat);        // 平均饱和度
        return out;
    }

    /**
     * 简单场景分类 (基于亮度和饱和度)
     *
     * <p>规则 (启发式, 非 ML):
     *   - 高亮 + 低饱和 → "明亮/清爽"
     *   - 低亮 + 低饱和 → "暗色/夜景"
     *   - 高饱和 → "多彩/鲜艳"
     *   - 中等 + 中等 → "自然/日常"
     *
     * @param stats 来自 computeStats
     * @return 中文场景标签
     */
    private String classifyScene(Map<String, Object> stats) {
        // 1. 提取数值 (Object → double)
        double bright = toDouble(stats.get("brightness"));   // 亮度 0-255
        double sat = toDouble(stats.get("saturation"));       // 饱和度 0-255
        // 2. 规则判定
        if (bright > 200 && sat < 50) return "明亮/清爽";    // 高亮低饱和
        if (bright < 60) return "暗色/夜景";                  // 低亮
        if (sat > 150) return "多彩/鲜艳";                    // 高饱和
        return "自然/日常";                                    // 兜底
    }

    /**
     * 提取 Top N 主色 (RGB 直方图)
     *
     * <p>算法:
     *   1. 量化颜色到 6-bit (64 桶) 减少噪声
     *   2. 统计每桶出现次数
     *   3. 取出现最多的 N 桶
     *   4. 反量化到 8-bit RGB 输出
     *
     * @param img BufferedImage
     * @param n   返回颜色数
     * @return 逗号分隔的 #RRGGBB 列表
     */
    private String topColors(BufferedImage img, int n) {
        // 1. 颜色直方图: key=RGB 桶号, value=像素数
        //    用普通 HashMap 即可, 桶数固定 64^3 不可能全满
        Map<Integer, Integer> histogram = new HashMap<>();
        int w = img.getWidth();
        int h = img.getHeight();
        // 2. 步长采样 (同 computeStats)
        int step = Math.max(1, Math.min(w, h) / 200);
        for (int y = 0; y < h; y += step) {
            for (int x = 0; x < w; x += step) {
                // 3. 取 RGB
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                // 4. 量化到 6-bit (高 2 位清零, 64 桶/通道)
                int bucket = ((r >> 2) << 12) | ((g >> 2) << 6) | (b >> 2);
                // 5. 直方图累加
                histogram.merge(bucket, 1, Integer::sum);
            }
        }
        // 6. 按计数降序排列
        //    LinkedHashMap 保持插入顺序, 用 stream sorted 收集
        Map<Integer, Integer> sorted = new LinkedHashMap<>();
        histogram.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))  // 降序
                .limit(n)                                                  // 取前 n
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        // 7. 格式化为 #RRGGBB
        StringBuilder sb = new StringBuilder();
        for (Integer bucket : sorted.keySet()) {
            // 8. 反量化: 6-bit → 8-bit (左移 2 位 + 重复高 2 位到低 2 位)
            int r = ((bucket >> 12) & 0x3F) << 2 | ((bucket >> 12) & 0x3) << 0;
            int g = ((bucket >> 6) & 0x3F) << 2 | ((bucket >> 6) & 0x3) << 0;
            int b = (bucket & 0x3F) << 2 | (bucket & 0x3) << 0;
            // 9. 拼 #RRGGBB
            if (sb.length() > 0) sb.append(", ");   // 分隔符
            sb.append(String.format("#%02X%02X%02X", r, g, b));
        }
        return sb.toString();
    }

    /** Object → double 工具 */
    private double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(o.toString()); }
        catch (Exception e) { return 0.0; }
    }

    /** 格式化 double (保留 1 位小数) */
    private String formatDouble(Object o) {
        return String.format("%.1f", toDouble(o));
    }

    /** inspect 委托给 ImageInspector */
    @Override
    public Map<String, Object> inspect(String imageBase64) {
        return ImageInspector.inspect(imageBase64);
    }
}
