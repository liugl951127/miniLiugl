package com.minimax.ai.generation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.BasicStroke;
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
 * 图表生成器 (V2.7 自研)
 *
 * <p>纯 Java 实现的图表渲染引擎, 无第三方依赖. 支持 7 种图表类型, 输出 PNG 字节流.</p>
 *
 * <h3>支持的图表类型</h3>
 * <table border="1">
 *   <tr><th>类型</th><th>适用场景</th><th>数据要求</th></tr>
 *   <tr><td>BAR (柱状图)</td><td>类别对比</td><td>X 标签 + 数值</td></tr>
 *   <tr><td>LINE (折线图)</td><td>趋势</td><td>X 标签 + 1-N 系列</td></tr>
 *   <tr><td>PIE (饼图)</td><td>占比</td><td>类别 + 数值</td></tr>
 *   <tr><td>SCATTER (散点图)</td><td>相关性</td><td>(x, y) 点对</td></tr>
 *   <tr><td>RADAR (雷达图)</td><td>多维评估</td><td>维度 + 1-N 系列</td></tr>
 *   <tr><td>HEATMAP (热力图)</td><td>矩阵</td><td>行 × 列 矩阵</td></tr>
 *   <tr><td>SANKEY (桑基图)</td><td>流向</td><td>source → target → value</td></tr>
 * </table>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 *   ChartData data = ChartData.builder()
 *       .type(ChartType.BAR)
 *       .title("2024 销售统计")
 *       .addSeries("销量", Arrays.asList(100.0, 200.0, 150.0))
 *       .categories(Arrays.asList("Q1", "Q2", "Q3"))
 *       .build();
 *   byte[] png = chartGenerator.render(data);
 * }</pre>
 *
 * <h3>性能</h3>
 * 800x600 PNG 渲染: ~10ms (中端 CPU, 软件渲染)
 *
 * @author MiniMax Team
 * @since V2.7
 */
@Slf4j
@Component
public class ChartGenerator {

    /**
     * 图表类型枚举
     */
    public enum ChartType {
        BAR,      // 柱状图
        LINE,     // 折线图
        PIE,      // 饼图
        SCATTER,  // 散点图
        RADAR,    // 雷达图
        HEATMAP,  // 热力图
        SANKEY    // 桑基图
    }

    /**
     * 数据系列: name + values
     */
    public static class Series {
        public String name;
        public List<Double> values;

        public Series(String name, List<Double> values) {
            this.name = name;
            this.values = values;
        }
    }

    /**
     * 散点: (x, y) 点
     */
    public static class Point {
        public double x, y;
        public String label;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public Point(double x, double y, String label) {
            this.x = x;
            this.y = y;
            this.label = label;
        }
    }

    /**
     * 桑基流: source -> target -> value
     */
    public static class SankeyFlow {
        public String source;
        public String target;
        public double value;

        public SankeyFlow(String source, String target, double value) {
            this.source = source;
            this.target = target;
            this.value = value;
        }
    }

    /**
     * 图表数据 (统一入口)
     */
    public static class ChartData {
        public ChartType type;            // 图表类型
        public String title;              // 标题
        public List<String> categories;   // X 轴标签 / 类别
        public List<Series> series;       // 数据系列 (BAR/LINE/RADAR)
        public List<Point> points;        // 散点 (SCATTER)
        public List<SankeyFlow> flows;    // 桑基流 (SANKEY)
        public double[][] heatmapMatrix;  // 热力图矩阵 (HEATMAP)
        public String[] heatmapRowLabels; // 热力图行标签
        public String[] heatmapColLabels; // 热力图列标签
        public int width = 800;           // 图片宽度 px
        public int height = 600;          // 图片高度 px
        public Color background = Color.WHITE;  // 背景色
        public Color[] palette;           // 自定义调色板 (null 用默认)

        public static ChartDataBuilder builder() {
            return new ChartDataBuilder();
        }
    }

    /**
     * Builder 模式构造 ChartData
     */
    public static class ChartDataBuilder {
        private final ChartData data = new ChartData();

        public ChartDataBuilder type(ChartType type) { data.type = type; return this; }
        public ChartDataBuilder title(String title) { data.title = title; return this; }
        public ChartDataBuilder categories(List<String> c) { data.categories = c; return this; }
        public ChartDataBuilder series(List<Series> s) { data.series = s; return this; }
        public ChartDataBuilder points(List<Point> p) { data.points = p; return this; }
        public ChartDataBuilder flows(List<SankeyFlow> f) { data.flows = f; return this; }
        public ChartDataBuilder heatmap(double[][] matrix, String[] rows, String[] cols) {
            data.heatmapMatrix = matrix;
            data.heatmapRowLabels = rows;
            data.heatmapColLabels = cols;
            return this;
        }
        public ChartDataBuilder size(int w, int h) { data.width = w; data.height = h; return this; }
        public ChartDataBuilder background(Color c) { data.background = c; return this; }
        public ChartDataBuilder palette(Color[] p) { data.palette = p; return this; }
        public ChartDataBuilder addSeries(String name, List<Double> values) {
            if (data.series == null) data.series = new ArrayList<>();
            data.series.add(new Series(name, values));
            return this;
        }
        public ChartData build() { return data; }
    }

    /** 默认调色板: 10 种柔和颜色 */
    private static final Color[] DEFAULT_PALETTE = {
            new Color(91, 156, 214),   // 蓝
            new Color(237, 125, 49),   // 橙
            new Color(112, 173, 71),   // 绿
            new Color(68, 114, 196),   // 深蓝
            new Color(198, 89, 17),    // 深橙
            new Color(128, 100, 162),  // 紫
            new Color(38, 68, 120),    // 深蓝2
            new Color(191, 144, 0),    // 金
            new Color(64, 173, 183),   // 青
            new Color(225, 87, 89)     // 红
    };

    /**
     * 渲染图表为 PNG 字节流
     *
     * @param data 图表数据
     * @return PNG 字节数组
     */
    public byte[] render(ChartData data) {
        if (data == null) throw new IllegalArgumentException("ChartData is null");
        if (data.type == null) throw new IllegalArgumentException("ChartType is required");

        // 创建画布
        BufferedImage img = new BufferedImage(data.width, data.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            // 开启抗锯齿
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            // 背景
            g.setColor(data.background);
            g.fillRect(0, 0, data.width, data.height);

            // 根据类型分发
            switch (data.type) {
                case BAR: renderBar(g, data); break;
                case LINE: renderLine(g, data); break;
                case PIE: renderPie(g, data); break;
                case SCATTER: renderScatter(g, data); break;
                case RADAR: renderRadar(g, data); break;
                case HEATMAP: renderHeatmap(g, data); break;
                case SANKEY: renderSankey(g, data); break;
            }
            // 标题
            drawTitle(g, data);
        } finally {
            g.dispose();
        }

        // 输出 PNG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", baos);
        } catch (Exception e) {
            throw new RuntimeException("PNG encode failed", e);
        }
        return baos.toByteArray();
    }

    /**
     * 绘制标题 (顶部居中)
     */
    private void drawTitle(Graphics2D g, ChartData data) {
        if (data.title == null || data.title.isEmpty()) return;
        g.setColor(new Color(60, 60, 60));
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(data.title);
        g.drawString(data.title, (data.width - w) / 2, 30);
    }

    /**
     * 调色板取色
     */
    private Color paletteColor(ChartData data, int idx) {
        Color[] p = data.palette != null ? data.palette : DEFAULT_PALETTE;
        return p[idx % p.length];
    }

    // ================== 柱状图 ==================

    /**
     * 渲染柱状图
     *
     * <h3>算法说明</h3>
     * 1. 坐标计算: margin 留白, plotX/plotY 是画图区左上角
     * 2. Y 轴归一化: 找到所有系列的最大值, 按比例缩放
     * 3. Y 轴刻度: 5 等分, 标注 0/0.2/0.4/0.6/0.8/1.0
     * 4. 柱分组: 每个 category 一组, 组内每个系列一根
     * 5. 柱宽: groupW / (barCount + 1), +1 是为了留间隙
     *
     * @param g    画笔
     * @param data 图表数据 (含 series + categories)
     */
    private void renderBar(Graphics2D g, ChartData data) {
        if (data.series == null || data.series.isEmpty()) return;
        // 边距: 左右 60px, 顶部 60px (留给标题)
        int margin = 60, titleH = 60;
        int plotX = margin, plotY = titleH;
        int plotW = data.width - 2 * margin;
        int plotH = data.height - titleH - margin;

        // 步骤 1: 计算所有系列的最大值 (Y 轴归一化)
        double maxV = 0;
        for (Series s : data.series) {
            for (double v : s.values) maxV = Math.max(maxV, v);
        }
        if (maxV == 0) maxV = 1;  // 防 0 除

        // 步骤 2: 画坐标轴 (浅灰)
        g.setColor(new Color(200, 200, 200));
        g.setStroke(new BasicStroke(1));
        g.drawLine(plotX, plotY, plotX, plotY + plotH);  // Y 轴 (从 plotY 到 plotY+plotH)
        g.drawLine(plotX, plotY + plotH, plotX + plotW, plotY + plotH);  // X 轴

        // 步骤 3: Y 轴刻度 (5 段, 包含 0 和 max)
        g.setColor(new Color(100, 100, 100));
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (int i = 0; i <= 5; i++) {
            int y = plotY + plotH - i * plotH / 5;  // 5 等分, 从底向上
            double val = maxV * i / 5;  // 当前刻度对应的值
            g.setColor(new Color(220, 220, 220));
            g.drawLine(plotX, y, plotX + plotW, y);  // 横向网格
            g.setColor(new Color(100, 100, 100));
            g.drawString(String.format("%.1f", val), 5, y + 4);  // 左侧标签
        }

        // 步骤 4: 画柱
        // cats: 类别数 (X 轴)
        int cats = data.categories != null ? data.categories.size() : data.series.get(0).values.size();
        int groupW = plotW / cats;                    // 每个类别占的宽度
        int barCount = data.series.size();             // 系列数 (一个类别内的柱数)
        int barW = Math.max(2, groupW / (barCount + 1));  // 柱宽 (+1 是留间隙)

        for (int i = 0; i < cats; i++) {
            int groupX = plotX + i * groupW;           // 当前类别的 X 起点
            for (int s = 0; s < barCount; s++) {
                List<Double> vals = data.series.get(s).values;
                if (i >= vals.size()) continue;        // 缺数据跳过
                double v = vals.get(i);
                int h = (int) (v / maxV * plotH);      // 柱高: 按 maxV 归一化
                int x = groupX + (s + 1) * (groupW / (barCount + 1));  // 第 s 个柱的 X
                int y = plotY + plotH - h;             // Y 从底部往上算
                g.setColor(paletteColor(data, s));
                g.fillRect(x, y, barW, h);             // 填充
                g.setColor(paletteColor(data, s).darker());
                g.drawRect(x, y, barW, h);             // 描边
            }
            // 类别标签 (X 轴下)
            if (data.categories != null && i < data.categories.size()) {
                g.setColor(new Color(80, 80, 80));
                g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                String lbl = data.categories.get(i);
                FontMetrics fm = g.getFontMetrics();
                int lw = fm.stringWidth(lbl);
                g.drawString(lbl, groupX + groupW / 2 - lw / 2, plotY + plotH + 18);
            }
        }

        // 步骤 5: 图例
        drawLegend(g, data, plotX, plotY - 5);
    }

    // ================== 折线图 ==================

    /**
     * 渲染折线图
     *
     * <h3>算法</h3>
     * 1. 同柱状图先归一化 (maxV)
     * 2. 每个系列用调色板中的一种颜色
     * 3. 连接相邻点: 屏幕坐标 (x, y), x 线性分布, y 反向 (高值在上)
     * 4. 点圆点表示数据点
     *
     * @param g    画笔
     * @param data 图表数据
     */
    private void renderLine(Graphics2D g, ChartData data) {
        if (data.series == null || data.series.isEmpty()) return;
        int margin = 60, titleH = 60;
        int plotX = margin, plotY = titleH;
        int plotW = data.width - 2 * margin;
        int plotH = data.height - titleH - margin;

        double maxV = 0, minV = Double.MAX_VALUE;
        for (Series s : data.series) {
            for (double v : s.values) {
                maxV = Math.max(maxV, v);
                minV = Math.min(minV, v);
            }
        }
        if (maxV == minV) { maxV += 1; minV -= 1; }

        g.setColor(new Color(200, 200, 200));
        g.drawLine(plotX, plotY, plotX, plotY + plotH);
        g.drawLine(plotX, plotY + plotH, plotX + plotW, plotY + plotH);

        // 网格 + Y 标签
        for (int i = 0; i <= 5; i++) {
            int y = plotY + plotH - i * plotH / 5;
            double val = minV + (maxV - minV) * i / 5;
            g.setColor(new Color(230, 230, 230));
            g.drawLine(plotX, y, plotX + plotW, y);
            g.setColor(new Color(100, 100, 100));
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.drawString(String.format("%.1f", val), 5, y + 4);
        }

        // 折线
        int cats = data.categories != null ? data.categories.size() : data.series.get(0).values.size();
        for (int s = 0; s < data.series.size(); s++) {
            List<Double> vals = data.series.get(s).values;
            Color c = paletteColor(data, s);
            g.setColor(c);
            g.setStroke(new BasicStroke(2.5f));
            int prevX = -1, prevY = -1;
            for (int i = 0; i < vals.size() && i < cats; i++) {
                int x = plotX + i * plotW / Math.max(1, cats - 1);
                int y = plotY + plotH - (int) ((vals.get(i) - minV) / (maxV - minV) * plotH);
                if (prevX >= 0) g.drawLine(prevX, prevY, x, y);
                g.fillOval(x - 4, y - 4, 8, 8);
                prevX = x;
                prevY = y;
            }
        }

        // X 标签
        if (data.categories != null) {
            g.setColor(new Color(80, 80, 80));
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            FontMetrics fm = g.getFontMetrics();
            for (int i = 0; i < data.categories.size() && i < cats; i++) {
                int x = plotX + i * plotW / Math.max(1, cats - 1);
                String lbl = data.categories.get(i);
                int lw = fm.stringWidth(lbl);
                g.drawString(lbl, x - lw / 2, plotY + plotH + 18);
            }
        }

        drawLegend(g, data, plotX, plotY - 5);
    }

    // ================== 饼图 ==================

    /**
     * 渲染饼图
     *
     * <h3>算法</h3>
     * 1. 累计总和, 计算每个扇形角度 = value / total * 360
     * 2. Java fillArc 从 3 点钟方向开始 (0度), 顺时针
     *    我们从 12 点方向 (-90度) 开始, 顺时针
     * 3. 描边白色分隔扇区
     * 4. 图例在右侧, 含百分比
     *
     * @param g    画笔
     * @param data 饼图数据 (只取第一个系列)
     */
    private void renderPie(Graphics2D g, ChartData data) {
        if (data.series == null || data.series.isEmpty()) return;
        // 饼图只取第一个系列
        List<Double> values = data.series.get(0).values;
        if (data.categories == null || data.categories.size() != values.size()) return;

        int cx = data.width / 2, cy = data.height / 2 + 20;
        int r = Math.min(data.width, data.height) / 3;

        double total = 0;
        for (double v : values) total += v;
        if (total == 0) return;

        double startAngle = -90;  // 从 12 点方向开始
        for (int i = 0; i < values.size(); i++) {
            double sweep = values.get(i) / total * 360;
            g.setColor(paletteColor(data, i));
            g.fillArc(cx - r, cy - r, r * 2, r * 2, (int) startAngle, (int) sweep);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2));
            g.drawArc(cx - r, cy - r, r * 2, r * 2, (int) startAngle, (int) sweep);
            startAngle += sweep;
        }

        // 图例 (右侧)
        int lx = data.width - 180, ly = 80;
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int i = 0; i < values.size(); i++) {
            g.setColor(paletteColor(data, i));
            g.fillRect(lx, ly, 18, 18);
            g.setColor(new Color(60, 60, 60));
            double pct = values.get(i) / total * 100;
            g.drawString(String.format("%s %.1f%%", data.categories.get(i), pct), lx + 24, ly + 14);
            ly += 24;
        }
    }

    // ================== 散点图 ==================

    private void renderScatter(Graphics2D g, ChartData data) {
        if (data.points == null || data.points.isEmpty()) return;
        int margin = 60, titleH = 60;
        int plotX = margin, plotY = titleH;
        int plotW = data.width - 2 * margin;
        int plotH = data.height - titleH - margin;

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Point p : data.points) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }
        if (minX == maxX) { minX -= 1; maxX += 1; }
        if (minY == maxY) { minY -= 1; maxY += 1; }

        g.setColor(new Color(200, 200, 200));
        g.drawLine(plotX, plotY, plotX, plotY + plotH);
        g.drawLine(plotX, plotY + plotH, plotX + plotW, plotY + plotH);

        g.setColor(new Color(100, 100, 100));
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (int i = 0; i <= 5; i++) {
            int yp = plotY + plotH - i * plotH / 5;
            double yv = minY + (maxY - minY) * i / 5;
            g.drawString(String.format("%.1f", yv), 5, yp + 4);
            g.setColor(new Color(230, 230, 230));
            g.drawLine(plotX, yp, plotX + plotW, yp);
            g.setColor(new Color(100, 100, 100));
            int xp = plotX + i * plotW / 5;
            double xv = minX + (maxX - minX) * i / 5;
            g.drawString(String.format("%.1f", xv), xp - 10, plotY + plotH + 16);
        }

        g.setColor(paletteColor(data, 0));
        for (Point p : data.points) {
            int xp = plotX + (int) ((p.x - minX) / (maxX - minX) * plotW);
            int yp = plotY + plotH - (int) ((p.y - minY) / (maxY - minY) * plotH);
            g.fillOval(xp - 5, yp - 5, 10, 10);
        }
    }

    // ================== 雷达图 ==================

    /**
     * 渲染雷达图 (Kiviat 图)
     *
     * <h3>算法</h3>
     * 1. N 个维度均匀分布在一个圆周上: angle = -90 + 360 * i / N
     * 2. 5 层网格多边形 (5 边形/六边形/...)
     * 3. 每个系列的值归一化到 [0, 1] 后映射到多边形
     * 4. 多边形 fill + stroke
     *
     * @param g    画笔
     * @param data 雷达图数据
     */
    private void renderRadar(Graphics2D g, ChartData data) {
        if (data.series == null || data.series.isEmpty() || data.categories == null) return;
        int cx = data.width / 2, cy = data.height / 2 + 20;
        int r = Math.min(data.width, data.height) / 3;
        int n = data.categories.size();

        // 多边形网格 (5 层)
        g.setColor(new Color(220, 220, 220));
        g.setStroke(new BasicStroke(1));
        for (int level = 1; level <= 5; level++) {
            int[] px = new int[n];
            int[] py = new int[n];
            for (int i = 0; i < n; i++) {
                double angle = Math.toRadians(-90 + 360.0 * i / n);
                px[i] = cx + (int) (Math.cos(angle) * r * level / 5);
                py[i] = cy + (int) (Math.sin(angle) * r * level / 5);
            }
            g.drawPolygon(px, py, n);
        }

        // 轴
        g.setColor(new Color(180, 180, 180));
        for (int i = 0; i < n; i++) {
            double angle = Math.toRadians(-90 + 360.0 * i / n);
            int x2 = cx + (int) (Math.cos(angle) * r);
            int y2 = cy + (int) (Math.sin(angle) * r);
            g.drawLine(cx, cy, x2, y2);
        }

        // 标签
        g.setColor(new Color(80, 80, 80));
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < n; i++) {
            double angle = Math.toRadians(-90 + 360.0 * i / n);
            int lx = cx + (int) (Math.cos(angle) * (r + 20)) - fm.stringWidth(data.categories.get(i)) / 2;
            int ly = cy + (int) (Math.sin(angle) * (r + 20)) + 6;
            g.drawString(data.categories.get(i), lx, ly);
        }

        // 系列
        for (int s = 0; s < data.series.size(); s++) {
            List<Double> vals = data.series.get(s).values;
            double maxV = 0;
            for (double v : vals) maxV = Math.max(maxV, v);
            if (maxV == 0) maxV = 1;
            int[] px = new int[n];
            int[] py = new int[n];
            for (int i = 0; i < n; i++) {
                double angle = Math.toRadians(-90 + 360.0 * i / n);
                double v = i < vals.size() ? vals.get(i) / maxV : 0;
                px[i] = cx + (int) (Math.cos(angle) * r * v);
                py[i] = cy + (int) (Math.sin(angle) * r * v);
            }
            Color c = paletteColor(data, s);
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 100));
            g.fillPolygon(px, py, n);
            g.setColor(c);
            g.setStroke(new BasicStroke(2));
            g.drawPolygon(px, py, n);
        }

        drawLegend(g, data, 60, 60);
    }

    // ================== 热力图 ==================

    /**
     * 渲染热力图
     *
     * <h3>算法</h3>
     * 1. 矩阵归一化: ratio = (v - min) / (max - min)
     * 2. 颜色映射: ratio < 0.5 蓝->白, ratio >= 0.5 白->红
     *    r = ratio < 0.5 ? 255*ratio*2 : 255
     *    g = b = ratio < 0.5 ? 255 : 255*(1-ratio)*2
     * 3. 行/列标签
     *
     * @param g    画笔
     * @param data 热力图数据 (matrix + labels)
     */
    private void renderHeatmap(Graphics2D g, ChartData data) {
        if (data.heatmapMatrix == null) return;
        double[][] m = data.heatmapMatrix;
        int rows = m.length, cols = m[0].length;
        int margin = 60, titleH = 60;
        int plotX = margin, plotY = titleH;
        int plotW = data.width - 2 * margin;
        int plotH = data.height - titleH - margin;
        int cellW = plotW / cols;
        int cellH = plotH / rows;

        double minV = Double.MAX_VALUE, maxV = -Double.MAX_VALUE;
        for (double[] row : m) for (double v : row) {
            minV = Math.min(minV, v);
            maxV = Math.max(maxV, v);
        }
        if (minV == maxV) { minV = 0; maxV = 1; }

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double ratio = (m[i][j] - minV) / (maxV - minV);
                // 蓝 -> 白 -> 红
                int r, gr, b;
                if (ratio < 0.5) {
                    r = (int) (255 * ratio * 2);
                    gr = (int) (255 * ratio * 2);
                    b = 255;
                } else {
                    r = 255;
                    gr = (int) (255 * (1 - ratio) * 2);
                    b = (int) (255 * (1 - ratio) * 2);
                }
                g.setColor(new Color(r, gr, b));
                g.fillRect(plotX + j * cellW, plotY + i * cellH, cellW, cellH);
                g.setColor(Color.WHITE);
                g.setFont(new Font("SansSerif", Font.BOLD, 11));
                String s = String.format("%.1f", m[i][j]);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(s, plotX + j * cellW + cellW / 2 - fm.stringWidth(s) / 2,
                        plotY + i * cellH + cellH / 2 + 4);
            }
        }

        if (data.heatmapRowLabels != null) {
            g.setColor(new Color(60, 60, 60));
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            for (int i = 0; i < rows && i < data.heatmapRowLabels.length; i++) {
                g.drawString(data.heatmapRowLabels[i], plotX - 10 - g.getFontMetrics().stringWidth(data.heatmapRowLabels[i]),
                        plotY + i * cellH + cellH / 2 + 4);
            }
        }
        if (data.heatmapColLabels != null) {
            g.setColor(new Color(60, 60, 60));
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            for (int j = 0; j < cols && j < data.heatmapColLabels.length; j++) {
                String s = data.heatmapColLabels[j];
                g.drawString(s, plotX + j * cellW + cellW / 2 - g.getFontMetrics().stringWidth(s) / 2,
                        plotY + plotH + 18);
            }
        }
    }

    // ================== 桑基图 ==================

    /**
     * 简易桑基图: 横向 flow 路径
     *
     * <h3>算法</h3>
     * 1. 节点位置: 左侧 (source) 纵向堆叠, 高度 ∝ out 值
     *               右侧 (target) 纵向堆叠, 高度 ∝ in 值
     * 2. 流路径: 4 顶点四边形 fill (透明度 80)
     *    顶点: (x1, y1-h/2), (x1, y1+h/2), (x2, y2+h/2), (x2, y2-h/2)
     * 3. 节点矩形 (左/右) 着色
     *
     * @param g    画笔
     * @param data 桑基图数据 (flows)
     */
    private void renderSankey(Graphics2D g, ChartData data) {
        if (data.flows == null || data.flows.isEmpty()) return;
        int margin = 60, titleH = 60;
        int plotX = margin, plotY = titleH;
        int plotW = data.width - 2 * margin;
        int plotH = data.height - titleH - margin;

        // 节点统计
        Map<String, Double> outSum = new LinkedHashMap<>();
        Map<String, Double> inSum = new LinkedHashMap<>();
        for (SankeyFlow f : data.flows) {
            outSum.merge(f.source, f.value, Double::sum);
            inSum.merge(f.target, f.value, Double::sum);
        }
        // 所有节点
        Set<String> nodes = new TreeSet<>();
        for (SankeyFlow f : data.flows) {
            nodes.add(f.source);
            nodes.add(f.target);
        }
        List<String> left = new ArrayList<>();
        List<String> right = new ArrayList<>();
        for (String n : nodes) {
            if (outSum.getOrDefault(n, 0.0) > 0) left.add(n);
            if (inSum.getOrDefault(n, 0.0) > 0) right.add(n);
        }

        double totalOut = outSum.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalIn = inSum.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalOut == 0) totalOut = 1;
        if (totalIn == 0) totalIn = 1;

        // 左侧节点位置
        Map<String, double[]> leftPos = new HashMap<>();
        double y = plotY;
        for (String n : left) {
            double h = outSum.get(n) / totalOut * plotH;
            leftPos.put(n, new double[]{y, h});
            y += h + 5;
        }
        Map<String, double[]> rightPos = new HashMap<>();
        y = plotY;
        for (String n : right) {
            double h = inSum.get(n) / totalIn * plotH;
            rightPos.put(n, new double[]{y, h});
            y += h + 5;
        }

        // 画流
        g.setStroke(new BasicStroke(0.5f));
        for (SankeyFlow f : data.flows) {
            double[] ls = leftPos.get(f.source);
            double[] rs = rightPos.get(f.target);
            if (ls == null || rs == null) continue;
            int y1 = (int) (ls[0] + ls[1] / 2);
            int y2 = (int) (rs[0] + rs[1] / 2);
            int x1 = plotX + 30, x2 = plotX + plotW - 30;
            int h = Math.max(1, (int) (f.value / totalOut * plotH * 0.8));
            g.setColor(new Color(paletteColor(data, 0).getRed(), paletteColor(data, 0).getGreen(),
                    paletteColor(data, 0).getBlue(), 80));
            int[] xPoints = {x1, x1, x2, x2};
            int[] yPoints = {y1 - h / 2, y1 + h / 2, y2 + h / 2, y2 - h / 2};
            g.fillPolygon(xPoints, yPoints, 4);
        }

        // 节点矩形
        for (Map.Entry<String, double[]> e : leftPos.entrySet()) {
            g.setColor(paletteColor(data, 0));
            g.fillRect(plotX, (int) e.getValue()[0], 30, (int) e.getValue()[1]);
            g.setColor(new Color(60, 60, 60));
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.drawString(e.getKey(), plotX + 35, (int) e.getValue()[0] + 14);
        }
        for (Map.Entry<String, double[]> e : rightPos.entrySet()) {
            g.setColor(paletteColor(data, 1));
            g.fillRect(plotX + plotW - 30, (int) e.getValue()[0], 30, (int) e.getValue()[1]);
            g.setColor(new Color(60, 60, 60));
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.drawString(e.getKey(), plotX + plotW - 35 - g.getFontMetrics().stringWidth(e.getKey()),
                    (int) e.getValue()[0] + 14);
        }
    }

    /**
     * 绘制图例 (右上角)
     */
    private void drawLegend(Graphics2D g, ChartData data, int x, int y) {
        if (data.series == null || data.series.isEmpty()) return;
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        int lx = x;
        for (int i = 0; i < data.series.size(); i++) {
            g.setColor(paletteColor(data, i));
            g.fillRect(lx, y, 14, 14);
            g.setColor(new Color(60, 60, 60));
            g.drawString(data.series.get(i).name, lx + 18, y + 12);
            lx += 18 + g.getFontMetrics().stringWidth(data.series.get(i).name) + 14;
        }
    }
}
