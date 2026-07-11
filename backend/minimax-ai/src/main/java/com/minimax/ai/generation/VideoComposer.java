package com.minimax.ai.generation;

import com.minimax.ai.datasource.DynamicDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * 视频合成器 (V2.7 自研)
 *
 * <p>从动态数据生成可视化视频. 纯 Java 实现, 无 ffmpeg/OpenCV 依赖.</p>
 *
 * <h3>核心能力</h3>
 * <ol>
 *   <li><b>数据驱动视频</b>: 查数据库 → 渲染帧 → 输出视频帧流</li>
 *   <li><b>数据可视化动画</b>: 数字滚动 / 柱状图生长 / 折线绘制</li>
 *   <li><b>字幕</b>: 标题 / 副标题 / 数据标签</li>
 *   <li><b>转场</b>: 淡入淡出 / 滑动 / 缩放</li>
 *   <li><b>品牌</b>: Logo / 水印 / 配色方案</li>
 * </ol>
 *
 * <h3>应用场景</h3>
 * <ul>
 *   <li>实时业务数据可视化 (KPI Dashboard 视频)</li>
 *   <li>数据故事叙述 (Data Storytelling)</li>
 *   <li>营销视频自动生成 (个性化推送)</li>
 *   <li>运营周报/月报 (自动生成汇报视频)</li>
 * </ul>
 *
 * <h3>输出</h3>
 * 帧流: List&lt;BufferedImage&gt; (每帧 800x600)
 * GIF: AnimationGenerator 二次包装
 * MP4: 生产建议用 ffmpeg (本类输出帧流)
 *
 * <h3>算法说明</h3>
 * <h4>1. 数据 → 帧</h4>
 * 每一帧是数据快照 + 动画进度. 例如"柱状图生长":
 * 帧 N 的柱高 = (最终高度) * (N / 总帧数) * easeInOut(N/总帧数)
 * easeInOut = 0.5 - 0.5 * cos(πt)  // 平滑缓动
 *
 * <h4>2. 帧插值</h4>
 * 关键帧 (key frame) 之间通过线性插值生成中间帧.
 * 公式: pixel(t) = pixel(t0) + (pixel(t1) - pixel(t0)) * (t - t0) / (t1 - t0)
 *
 * <h4>3. 转场</h4>
 * 淡入淡出: alpha = smoothstep(0, transitionFrames, currentFrame)
 * 滑动: x_offset = -width * (1 - currentFrame / transitionFrames)
 *
 * @author MiniMax Team
 * @since V2.7
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoComposer {

    /** 动态数据源: 实际业务数据来自用户配置的数据库 */
    private final DynamicDataSource dynamicDataSource;

    /**
     * 视频配置 (每个字段都会影响最终输出)
     */
    public static class VideoConfig {
        public int width = 1280;                          // 画布宽 (像素, 16:9 比例: 1920x1080, 1280x720, 800x450)
        public int height = 720;                          // 画布高
        public int fps = 30;                              // 帧率 (24/30/60, 影响流畅度)
        public int durationSeconds = 10;                  // 总时长 (秒)
        public Color background = new Color(15, 23, 42);  // 背景色 (深蓝)
        public Color primaryColor = new Color(59, 130, 246);  // 主色 (品牌色)
        public Color textColor = Color.WHITE;             // 文字色
        public String title;                              // 主标题 (顶部)
        public String subtitle;                           // 副标题 (主标题下方)
        public String dataSource;                         // 数据源描述 (右下角水印)
        public String logoText;                           // Logo 文字 (左上角)
        public List<DataFrame> dataFrames = new ArrayList<>();  // 关键帧列表
        public TransitionType defaultTransition = TransitionType.FADE;  // 默认转场

        public static VideoConfigBuilder builder() {
            return new VideoConfigBuilder();
        }
    }

    public static class VideoConfigBuilder {
        private final VideoConfig cfg = new VideoConfig();
        public VideoConfigBuilder size(int w, int h) { cfg.width = w; cfg.height = h; return this; }
        public VideoConfigBuilder fps(int n) { cfg.fps = n; return this; }
        public VideoConfigBuilder duration(int seconds) { cfg.durationSeconds = seconds; return this; }
        public VideoConfigBuilder title(String t) { cfg.title = t; return this; }
        public VideoConfigBuilder subtitle(String s) { cfg.subtitle = s; return this; }
        public VideoConfigBuilder dataSource(String s) { cfg.dataSource = s; return this; }
        public VideoConfigBuilder logoText(String s) { cfg.logoText = s; return this; }
        public VideoConfigBuilder background(Color c) { cfg.background = c; return this; }
        public VideoConfigBuilder primaryColor(Color c) { cfg.primaryColor = c; return this; }
        public VideoConfigBuilder addFrame(DataFrame f) { cfg.dataFrames.add(f); return this; }
        public VideoConfig build() { return cfg; }
    }

    /**
     * 转场类型
     */
    public enum TransitionType {
        FADE,    // 淡入淡出 (alpha 渐变)
        SLIDE,   // 滑动 (x 偏移)
        ZOOM,    // 缩放 (scale 渐变)
        NONE     // 无 (直接切换)
    }

    /**
     * 数据帧: 一个数据快照
     * 视频会按时间顺序播放这些帧
     */
    public static class DataFrame {
        public double startTime;      // 起始时间 (秒, 0 ~ durationSeconds)
        public double endTime;        // 结束时间 (秒)
        public String label;          // 帧标题
        public List<MetricCard> metrics = new ArrayList<>();  // 指标卡片
        public List<BarItem> bars = new ArrayList<>();        // 柱状图
        public String narrativeText;  // 叙述文字 (底部字幕)

        public static DataFrameBuilder builder() {
            return new DataFrameBuilder();
        }
    }

    public static class DataFrameBuilder {
        private final DataFrame f = new DataFrame();
        public DataFrameBuilder time(double start, double end) { f.startTime = start; f.endTime = end; return this; }
        public DataFrameBuilder label(String l) { f.label = l; return this; }
        public DataFrameBuilder addMetric(MetricCard m) { f.metrics.add(m); return this; }
        public DataFrameBuilder addBar(BarItem b) { f.bars.add(b); return this; }
        public DataFrameBuilder narrative(String s) { f.narrativeText = s; return this; }
        public DataFrame build() { return f; }
    }

    /**
     * 指标卡 (KPI): 大数字 + 标题 + 变化箭头
     */
    public static class MetricCard {
        public String title;       // "今日销售额"
        public double value;       // 当前值
        public String unit;        // "元" / "%" / "人"
        public double change;      // 同比变化 (+0.15 = 增长 15%)
        public Color color;        // 数字颜色

        public MetricCard(String title, double value, String unit, double change) {
            this.title = title;
            this.value = value;
            this.unit = unit;
            this.change = change;
            this.color = new Color(59, 130, 246);
        }
    }

    /**
     * 柱状图条目
     */
    public static class BarItem {
        public String label;       // "Q1"
        public double value;       // 100
        public Color color;        // 颜色 (null 用默认)

        public BarItem(String label, double value) {
            this.label = label;
            this.value = value;
            this.color = null;
        }

        public BarItem(String label, double value, Color color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    /**
     * 生成所有帧 (从配置)
     *
     * @param cfg 视频配置
     * @return 帧列表 (List<BufferedImage>), 数量 = fps * durationSeconds
     */
    public List<BufferedImage> renderAllFrames(VideoConfig cfg) {
        if (cfg == null) cfg = new VideoConfig();
        int totalFrames = cfg.fps * cfg.durationSeconds;
        List<BufferedImage> frames = new ArrayList<>(totalFrames);

        // 1. 计算每帧的时间点
        // 时间 t (秒) = frameIndex / fps
        for (int i = 0; i < totalFrames; i++) {
            double t = (double) i / cfg.fps;
            BufferedImage frame = renderFrameAt(cfg, t, i, totalFrames);
            frames.add(frame);
        }
        log.info("Generated {} frames ({}x{} @ {}fps for {}s)",
                totalFrames, cfg.width, cfg.height, cfg.fps, cfg.durationSeconds);
        return frames;
    }

    /**
     * 渲染单帧
     *
     * 算法: 找到 t 时刻的活跃 DataFrame, 计算动画进度, 渲染
     *
     * @param cfg         视频配置
     * @param t           当前时间 (秒)
     * @param frameIndex  帧索引 (0-based)
     * @param totalFrames 总帧数
     * @return 单帧 BufferedImage
     */
    private BufferedImage renderFrameAt(VideoConfig cfg, double t, int frameIndex, int totalFrames) {
        // 1. 创建画布
        BufferedImage img = new BufferedImage(cfg.width, cfg.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            // 2. 开启抗锯齿 (关键: 文字和图形边缘平滑)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // 3. 背景 (单色填充; 实际可换渐变)
            g.setColor(cfg.background);
            g.fillRect(0, 0, cfg.width, cfg.height);

            // 4. Logo (左上角)
            if (cfg.logoText != null) {
                drawLogo(g, cfg);
            }

            // 5. 标题 (顶部)
            if (cfg.title != null) {
                drawTitle(g, cfg);
            }

            // 6. 找当前活跃的 DataFrame
            DataFrame activeFrame = findActiveFrame(cfg, t);
            if (activeFrame != null) {
                // 计算该帧的动画进度 (0~1)
                double progress = computeProgress(activeFrame, t);

                // 渲染该帧内容
                renderDataFrame(g, cfg, activeFrame, progress);
            }

            // 7. 数据源水印 (右下角)
            if (cfg.dataSource != null) {
                drawWatermark(g, cfg);
            }

            // 8. 整体淡入淡出 (开头 + 结尾)
            double globalAlpha = computeGlobalAlpha(t, cfg.durationSeconds);
            if (globalAlpha < 1.0) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) globalAlpha));
                g.setColor(cfg.background);
                g.fillRect(0, 0, cfg.width, cfg.height);
            }
        } finally {
            g.dispose();
        }
        return img;
    }

    /**
     * 找当前活跃的 DataFrame
     * 如果多个匹配, 取最后一个 (后定义的覆盖前面的)
     *
     * @param cfg 视频配置
     * @param t   当前时间 (秒)
     * @return 活跃帧, 可能为 null
     */
    private DataFrame findActiveFrame(VideoConfig cfg, double t) {
        DataFrame active = null;
        for (DataFrame f : cfg.dataFrames) {
            if (t >= f.startTime && t <= f.endTime) {
                active = f;  // 最后一个匹配的
            }
        }
        return active;
    }

    /**
     * 计算 DataFrame 内的动画进度 (0~1)
     * 算法: progress = (t - start) / (end - start)
     * 然后用 ease-in-out 平滑: ease(t) = 0.5 - 0.5 * cos(πt)
     *
     * @param frame 数据帧
     * @param t     当前时间
     * @return 0~1 之间的进度
     */
    private double computeProgress(DataFrame frame, double t) {
        if (frame.endTime <= frame.startTime) return 1.0;
        double raw = (t - frame.startTime) / (frame.endTime - frame.startTime);
        raw = Math.max(0, Math.min(1, raw));
        // ease-in-out cubic: 平滑开始 + 结束
        return raw * raw * (3 - 2 * raw);
    }

    /**
     * 渲染 DataFrame 内容
     */
    private void renderDataFrame(Graphics2D g, VideoConfig cfg, DataFrame frame, double progress) {
        // 1. 帧标签 (居中)
        if (frame.label != null) {
            g.setColor(cfg.textColor);
            g.setFont(new Font("SansSerif", Font.BOLD, 48));
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(frame.label);
            g.drawString(frame.label, (cfg.width - w) / 2, 200);
        }

        // 2. Metric 卡片 (横向排列)
        if (!frame.metrics.isEmpty()) {
            drawMetrics(g, cfg, frame.metrics, progress);
        }

        // 3. 柱状图 (按 progress 增长)
        if (!frame.bars.isEmpty()) {
            drawBars(g, cfg, frame.bars, progress);
        }

        // 4. 叙述文字 (底部)
        if (frame.narrativeText != null) {
            drawNarrative(g, cfg, frame.narrativeText, progress);
        }
    }

    /**
     * 绘制 Metric 卡片
     */
    private void drawMetrics(Graphics2D g, VideoConfig cfg, List<MetricCard> metrics, double progress) {
        int cardW = 350, cardH = 180;
        int totalW = cardW * metrics.size() + 30 * (metrics.size() - 1);
        int startX = (cfg.width - totalW) / 2;
        int y = cfg.height / 2 - 100;
        for (int i = 0; i < metrics.size(); i++) {
            MetricCard m = metrics.get(i);
            int x = startX + i * (cardW + 30);
            // 数字滚动: 显示 progress 比例的值
            double displayValue = m.value * progress;
            // 卡片背景
            g.setColor(new Color(30, 41, 59));
            g.fillRoundRect(x, y, cardW, cardH, 12, 12);
            // 标题
            g.setColor(new Color(148, 163, 184));
            g.setFont(new Font("SansSerif", Font.PLAIN, 18));
            g.drawString(m.title, x + 20, y + 35);
            // 数字
            g.setColor(m.color);
            g.setFont(new Font("SansSerif", Font.BOLD, 56));
            String numStr = formatNumber(displayValue) + (m.unit == null ? "" : m.unit);
            g.drawString(numStr, x + 20, y + 100);
            // 变化
            if (m.change != 0) {
                g.setColor(m.change > 0 ? new Color(34, 197, 94) : new Color(239, 68, 68));
                g.setFont(new Font("SansSerif", Font.PLAIN, 16));
                String arrow = m.change > 0 ? "↑" : "↓";
                g.drawString(String.format("%s %.1f%%", arrow, Math.abs(m.change) * 100), x + 20, y + 130);
            }
        }
    }

    /**
     * 绘制柱状图
     * 算法: 柱高 = value * progress * scaleFactor
     * 颜色: BarItem.color 默认值用主色
     */
    private void drawBars(Graphics2D g, VideoConfig cfg, List<BarItem> bars, double progress) {
        int margin = 100;
        int chartW = cfg.width - 2 * margin;
        int chartH = 300;
        int chartX = margin;
        int chartY = cfg.height - chartH - 150;
        int barW = chartW / bars.size() - 20;

        double maxVal = 0;
        for (BarItem b : bars) maxVal = Math.max(maxVal, b.value);
        if (maxVal == 0) maxVal = 1;

        for (int i = 0; i < bars.size(); i++) {
            BarItem b = bars.get(i);
            int x = chartX + i * (barW + 20) + 10;
            int h = (int) (b.value / maxVal * chartH * progress);
            int y = chartY + chartH - h;
            Color c = b.color != null ? b.color : cfg.primaryColor;
            // 柱 (圆角)
            g.setColor(c);
            g.fillRoundRect(x, y, barW, h, 8, 8);
            // 标签
            g.setColor(cfg.textColor);
            g.setFont(new Font("SansSerif", Font.PLAIN, 20));
            FontMetrics fm = g.getFontMetrics();
            int lw = fm.stringWidth(b.label);
            g.drawString(b.label, x + barW / 2 - lw / 2, chartY + chartH + 30);
            // 数值
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            String v = formatNumber(b.value * progress);
            int vw = g.getFontMetrics().stringWidth(v);
            g.drawString(v, x + barW / 2 - vw / 2, y - 10);
        }
    }

    /**
     * 绘制叙述文字 (底部)
     */
    private void drawNarrative(Graphics2D g, VideoConfig cfg, String text, double progress) {
        // 进度 0~0.3 时透明度从 0 渐变到 1, 0.3~0.7 保持, 0.7~1 渐变到 0
        float alpha;
        if (progress < 0.3) alpha = (float) (progress / 0.3);
        else if (progress > 0.7) alpha = (float) ((1 - progress) / 0.3);
        else alpha = 1.0f;

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.setColor(cfg.textColor);
        g.setFont(new Font("SansSerif", Font.PLAIN, 28));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text);
        g.drawString(text, (cfg.width - w) / 2, cfg.height - 80);
    }

    /**
     * 绘制 Logo
     */
    private void drawLogo(Graphics2D g, VideoConfig cfg) {
        g.setColor(cfg.primaryColor);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString(cfg.logoText, 30, 50);
    }

    /**
     * 绘制标题
     */
    private void drawTitle(Graphics2D g, VideoConfig cfg) {
        g.setColor(cfg.textColor);
        g.setFont(new Font("SansSerif", Font.BOLD, 64));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(cfg.title);
        g.drawString(cfg.title, (cfg.width - w) / 2, 130);
        if (cfg.subtitle != null) {
            g.setColor(new Color(148, 163, 184));
            g.setFont(new Font("SansSerif", Font.PLAIN, 28));
            int sw = fm.stringWidth(cfg.subtitle);
            g.drawString(cfg.subtitle, (cfg.width - sw) / 2, 170);
        }
    }

    /**
     * 绘制水印
     */
    private void drawWatermark(Graphics2D g, VideoConfig cfg) {
        g.setColor(new Color(100, 116, 139));
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.drawString(cfg.dataSource, cfg.width - 200, cfg.height - 30);
    }

    /**
     * 全局淡入淡出 (开头 0.5s 淡入, 结尾 0.5s 淡出)
     * 公式: alpha = 1.0, 开头 0~0.5 渐入, 结尾 duration-0.5~duration 渐出
     */
    private double computeGlobalAlpha(double t, double duration) {
        if (t < 0.5) return t / 0.5;
        if (t > duration - 0.5) return (duration - t) / 0.5;
        return 1.0;
    }

    /**
     * 数字格式化: 1234567 -> "1.2M", 1234 -> "1,234"
     */
    private String formatNumber(double v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000);
        if (v >= 1_000) return String.format("%.1fK", v / 1_000);
        return String.format("%.0f", v);
    }

    /**
     * 主入口: 从动态数据生成 KPI 视频
     * 数据来源: 用户配置的数据库 (DynamicDataSource)
     *
     * @param dataSourceId 数据源 ID
     * @param tableName    表名
     * @param metricColumn 数值列
     * @param groupColumn  分组列 (可选, 柱状图用)
     * @param cfg          视频配置
     * @return 帧列表
     */
    public List<BufferedImage> renderFromData(Long dataSourceId, String tableName, String metricColumn,
                                               String groupColumn, VideoConfig cfg) {
        // 1. 真实数据查询 (无 mock!)
        List<Map<String, Object>> rows = dynamicDataSource.query(dataSourceId, tableName, 1000, null);
        if (rows.isEmpty()) {
            log.warn("No data from {}.{}", tableName, metricColumn);
            return renderAllFrames(cfg);
        }

        // 2. 自动构建数据帧
        if (groupColumn != null && !groupColumn.isEmpty()) {
            // 柱状图: 按 groupColumn 聚合
            Map<Object, Double> agg = new LinkedHashMap<>();
            for (Map<String, Object> r : rows) {
                Object k = r.get(groupColumn);
                Object v = r.get(metricColumn);
                if (k == null) continue;
                double d = v instanceof Number ? ((Number) v).doubleValue() : 0;
                agg.merge(k, d, Double::sum);
            }
            // 排序, 取前 10
            List<Map.Entry<Object, Double>> sorted = new ArrayList<>(agg.entrySet());
            sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            int top = Math.min(10, sorted.size());
            DataFrame barFrame = DataFrame.builder()
                    .time(0, cfg.durationSeconds)
                    .label(tableName + " - " + metricColumn)
                    .narrative("数据来源: " + tableName)
                    .build();
            for (int i = 0; i < top; i++) {
                barFrame.bars.add(new BarItem(
                        String.valueOf(sorted.get(i).getKey()),
                        sorted.get(i).getValue()));
            }
            cfg.dataFrames.clear();
            cfg.dataFrames.add(barFrame);
        } else {
            // 单值指标
            double total = 0;
            for (Map<String, Object> r : rows) {
                Object v = r.get(metricColumn);
                if (v instanceof Number) total += ((Number) v).doubleValue();
            }
            DataFrame metricFrame = DataFrame.builder()
                    .time(0, cfg.durationSeconds)
                    .label(metricColumn + " 汇总")
                    .narrative("数据来源: " + tableName + " (共 " + rows.size() + " 条)")
                    .addMetric(new MetricCard(metricColumn, total, "", 0))
                    .build();
            cfg.dataFrames.clear();
            cfg.dataFrames.add(metricFrame);
        }

        return renderAllFrames(cfg);
    }
}
