package com.minimax.ai.generation;

import com.minimax.ai.datasource.DynamicDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.AlphaComposite;
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
 * 数据看板生成器 (V2.7 自研)
 *
 * <p>从动态数据库生成数据看板 (Dashboard) PNG.
 * 包含 KPI 卡片 + 多个图表 + 数据表, 一图概览业务全貌.</p>
 *
 * <h3>布局</h3>
 * <pre>
 *  ┌────────────────────────────────────────────────┐
 *  │           Dashboard Title (顶部)                │
 *  ├──────────┬──────────┬──────────┬──────────────┤
 *  │  KPI #1  │  KPI #2  │  KPI #3  │   KPI #4     │
 *  ├──────────┴──────────┼──────────┴──────────────┤
 *  │   Chart 1 (主)      │      Chart 2            │
 *  ├─────────────────────┼──────────────────────────┤
 *  │   Chart 3           │      Data Table         │
 *  └─────────────────────┴──────────────────────────┘
 * </pre>
 *
 * <h3>数据流</h3>
 * DashboardBuilder -> DynamicDataSource -> 真实数据库 -> 渲染 PNG
 * 完全使用真实数据, 无 mock / hardcode
 *
 * @author MiniMax Team
 * @since V2.7
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardBuilder {

    private final DynamicDataSource dynamicDataSource;
    private final ChartGenerator chartGenerator;

    /**
     * 看板配置
     */
    public static class DashboardConfig {
        public int width = 1920;             // 画布宽
        public int height = 1080;            // 画布高
        public String title = "数据看板";    // 标题
        public String subtitle;              // 副标题
        public Color background = new Color(248, 250, 252);
        public Color cardBg = Color.WHITE;
        public Color primaryColor = new Color(59, 130, 246);
        public Color textColor = new Color(15, 23, 42);
        public Color secondaryText = new Color(100, 116, 139);
        public String dataSource;            // 数据源名称 (用于水印)
        public List<DashboardCard> cards = new ArrayList<>();

        public static DashboardConfigBuilder builder() {
            return new DashboardConfigBuilder();
        }
    }

    public static class DashboardConfigBuilder {
        private final DashboardConfig cfg = new DashboardConfig();
        public DashboardConfigBuilder size(int w, int h) { cfg.width = w; cfg.height = h; return this; }
        public DashboardConfigBuilder title(String t) { cfg.title = t; return this; }
        public DashboardConfigBuilder subtitle(String s) { cfg.subtitle = s; return this; }
        public DashboardConfigBuilder dataSource(String s) { cfg.dataSource = s; return this; }
        public DashboardConfigBuilder addCard(DashboardCard c) { cfg.cards.add(c); return this; }
        public DashboardConfig build() { return cfg; }
    }

    /**
     * 看板卡片
     */
    public static class DashboardCard {
        public CardType type;
        public String title;
        public Object data;                  // 卡片数据 (KPI = MetricData, Chart = ChartData, Table = TableData)
        public int colSpan = 1;              // 横向占几格
        public int rowSpan = 1;              // 纵向占几格

        public DashboardCard(CardType type, String title, Object data) {
            this.type = type;
            this.title = title;
            this.data = data;
        }

        public DashboardCard colSpan(int n) { this.colSpan = n; return this; }
        public DashboardCard rowSpan(int n) { this.rowSpan = n; return this; }

        public enum CardType {
            KPI,       // 关键指标
            CHART,     // 图表
            TABLE      // 数据表
        }
    }

    /**
     * KPI 数据
     */
    public static class MetricData {
        public double value;
        public String unit;
        public double change;    // 同比
        public String context;   // "vs 上周"

        public MetricData(double value, String unit, double change, String context) {
            this.value = value;
            this.unit = unit;
            this.change = change;
            this.context = context;
        }
    }

    /**
     * 表格数据
     */
    public static class TableData {
        public List<String> columns;
        public List<List<String>> rows;
        public int maxRows = 10;

        public TableData(List<String> columns, List<List<String>> rows) {
            this.columns = columns;
            this.rows = rows;
        }
    }

    /**
     * 主入口: 从动态数据生成完整 Dashboard
     *
     * @param cfg 看板配置 (含 cards 列表)
     * @return PNG 字节
     */
    public byte[] render(DashboardConfig cfg) {
        if (cfg == null) cfg = new DashboardConfig();
        BufferedImage img = new BufferedImage(cfg.width, cfg.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            // 1. 抗锯齿
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            // 2. 背景
            g.setColor(cfg.background);
            g.fillRect(0, 0, cfg.width, cfg.height);
            // 3. 标题区
            drawHeader(g, cfg);
            // 4. 卡片网格
            drawCards(g, cfg);
            // 5. 底部水印
            if (cfg.dataSource != null) drawWatermark(g, cfg);
        } finally {
            g.dispose();
        }
        // 6. PNG 编码
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", baos);
        } catch (Exception e) {
            throw new RuntimeException("PNG encode failed", e);
        }
        return baos.toByteArray();
    }

    /**
     * 渲染头部 (标题 + 副标题)
     */
    private void drawHeader(Graphics2D g, DashboardConfig cfg) {
        g.setColor(cfg.textColor);
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        g.drawString(cfg.title, 40, 60);
        if (cfg.subtitle != null) {
            g.setColor(cfg.secondaryText);
            g.setFont(new Font("SansSerif", Font.PLAIN, 18));
            g.drawString(cfg.subtitle, 40, 90);
        }
        // 时间戳
        g.setColor(cfg.secondaryText);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString(new Date().toString(), cfg.width - 280, 60);
    }

    /**
     * 渲染卡片网格
     * 布局算法: 自动按 colSpan 排版
     * 网格 12 列, 每列宽 = (width - 80) / 12, 边距 40
     */
    private void drawCards(Graphics2D g, DashboardConfig cfg) {
        int margin = 40;
        int gridTop = 120;
        int gridBottom = cfg.height - 40;
        int totalCols = 12;
        int colW = (cfg.width - 2 * margin) / totalCols;
        int gap = 20;
        int cardH = 240;

        int col = 0, row = 0;
        for (DashboardCard c : cfg.cards) {
            int x = margin + col * colW + gap * col;
            int y = gridTop + row * (cardH + gap);
            int w = colW * c.colSpan - gap * (c.colSpan - 1) - gap;
            int h = cardH * c.rowSpan - gap * (c.rowSpan - 1) - gap;
            // 卡片背景 (白底 + 圆角)
            g.setColor(cfg.cardBg);
            g.fillRoundRect(x, y, w, h, 12, 12);
            // 卡片标题
            g.setColor(cfg.secondaryText);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g.drawString(c.title, x + 20, y + 30);
            // 内容
            switch (c.type) {
                case KPI: drawKPI(g, cfg, c, x, y, w, h); break;
                case CHART: drawChartCard(g, c, x, y, w, h); break;
                case TABLE: drawTable(g, c, x, y, w, h); break;
            }
            // 下一格
            col += c.colSpan;
            if (col + c.colSpan > totalCols) {
                col = 0;
                row++;
            }
        }
    }

    /**
     * KPI 卡片
     */
    private void drawKPI(Graphics2D g, DashboardConfig cfg, DashboardCard c, int x, int y, int w, int h) {
        if (!(c.data instanceof MetricData)) return;
        MetricData m = (MetricData) c.data;
        // 数值 (大字号)
        g.setColor(cfg.textColor);
        g.setFont(new Font("SansSerif", Font.BOLD, 56));
        String valStr = formatNumber(m.value) + (m.unit != null ? m.unit : "");
        g.drawString(valStr, x + 20, y + 110);
        // 变化
        if (m.change != 0) {
            Color c1 = m.change > 0 ? new Color(34, 197, 94) : new Color(239, 68, 68);
            g.setColor(c1);
            g.setFont(new Font("SansSerif", Font.PLAIN, 18));
            String arrow = m.change > 0 ? "↑" : "↓";
            g.drawString(String.format("%s %.1f%%", arrow, Math.abs(m.change) * 100), x + 20, y + 145);
        }
        // 上下文
        if (m.context != null) {
            g.setColor(cfg.secondaryText);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.drawString(m.context, x + 20, y + 170);
        }
    }

    /**
     * 图表卡片
     */
    private void drawChartCard(Graphics2D g, DashboardCard c, int x, int y, int w, int h) {
        if (!(c.data instanceof ChartGenerator.ChartData)) return;
        ChartGenerator.ChartData data = (ChartGenerator.ChartData) c.data;
        // 用 ChartGenerator 渲染子图
        data.width = w - 40;
        data.height = h - 60;
        data.title = null;  // 卡片已有标题
        byte[] png = chartGenerator.render(data);
        // 画到卡片
        try {
            BufferedImage chartImg = ImageIO.read(new java.io.ByteArrayInputStream(png));
            g.drawImage(chartImg, x + 20, y + 50, null);
        } catch (Exception e) {
            log.warn("Draw chart failed: {}", e.getMessage());
        }
    }

    /**
     * 表格卡片
     */
    private void drawTable(Graphics2D g, DashboardCard c, int x, int y, int w, int h) {
        if (!(c.data instanceof TableData)) return;
        TableData t = (TableData) c.data;
        if (t.columns == null || t.rows == null) return;
        int rowH = 30;
        int colW = w / t.columns.size();
        // 表头
        g.setColor(new Color(241, 245, 249));
        g.fillRect(x + 20, y + 50, w - 40, rowH);
        g.setColor(new Color(15, 23, 42));
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        for (int i = 0; i < t.columns.size(); i++) {
            g.drawString(t.columns.get(i), x + 30 + i * colW, y + 50 + 20);
        }
        // 数据
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        int maxRows = Math.min(t.maxRows, t.rows.size());
        for (int r = 0; r < maxRows; r++) {
            int ry = y + 80 + r * rowH;
            if (r % 2 == 1) {
                g.setColor(new Color(248, 250, 252));
                g.fillRect(x + 20, ry, w - 40, rowH);
            }
            g.setColor(new Color(51, 65, 85));
            List<String> row = t.rows.get(r);
            for (int i = 0; i < row.size() && i < t.columns.size(); i++) {
                g.drawString(truncate(row.get(i), colW / 8), x + 30 + i * colW, ry + 20);
            }
        }
    }

    /**
     * 数字格式化
     */
    private String formatNumber(double v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000);
        if (v >= 10_000) return String.format("%.1fK", v / 1_000);
        if (v >= 1000) return String.format("%,.0f", v);
        return String.format("%.0f", v);
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    /**
     * 水印
     */
    private void drawWatermark(Graphics2D g, DashboardConfig cfg) {
        g.setColor(new Color(148, 163, 184));
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("数据源: " + cfg.dataSource + " | " + new Date(), 40, cfg.height - 20);
    }

    /**
     * 主入口: 从真实数据构建 Dashboard
     *
     * 完整流程:
     *   1. 调 DynamicDataSource 查真实数据库
     *   2. 推断列类型 (数字 / 字符串 / 日期)
     *   3. 自动选 KPI / 图表类型
     *   4. 渲染
     *
     * @param dataSourceId  数据源 ID
     * @param mainTable     主表
     * @param title         看板标题
     * @return PNG 字节
     */
    public byte[] renderFromData(Long dataSourceId, String mainTable, String title) {
        // 1. 加载 schema + 数据
        List<Map<String, Object>> rows = dynamicDataSource.query(dataSourceId, mainTable, 1000, null);
        if (rows.isEmpty()) {
            log.warn("No data from {}", mainTable);
            return render(DashboardConfig.builder()
                    .title(title)
                    .dataSource("empty")
                    .addCard(new DashboardCard(
                            DashboardCard.CardType.KPI, "无数据", new MetricData(0, "", 0, "表 " + mainTable + " 无数据")))
                    .build());
        }

        // 2. 列类型推断
        Map<String, String> types = DynamicDataSource.inferTypes(rows);
        List<String> numericCols = DynamicDataSource.findNumericColumns(rows);
        List<String> catCols = DynamicDataSource.findCategoricalColumns(rows);

        // 3. 构建 Dashboard
        DashboardConfigBuilder dcb = DashboardConfig.builder()
                .title(title)
                .subtitle("数据源: " + mainTable + " (" + rows.size() + " 行)")
                .dataSource(mainTable);

        // KPI 卡片: 数字列求和, 取前 4 个
        for (int i = 0; i < Math.min(4, numericCols.size()); i++) {
            String col = numericCols.get(i);
            double sum = 0;
            for (Map<String, Object> r : rows) {
                Object v = r.get(col);
                if (v instanceof Number) sum += ((Number) v).doubleValue();
            }
            dcb.addCard(new DashboardCard(
                    DashboardCard.CardType.KPI,
                    col + " 合计",
                    new MetricData(sum, "", 0, "前 " + rows.size() + " 行汇总")
            ));
        }

        // 柱状图: 1 个分类列 + 1 个数字列
        if (!catCols.isEmpty() && !numericCols.isEmpty()) {
            String cat = catCols.get(0);
            String num = numericCols.get(0);
            Map<Object, Double> agg = new LinkedHashMap<>();
            for (Map<String, Object> r : rows) {
                Object k = r.get(cat);
                Object v = r.get(num);
                if (k == null) continue;
                double d = v instanceof Number ? ((Number) v).doubleValue() : 0;
                agg.merge(k, d, Double::sum);
            }
            List<Map.Entry<Object, Double>> sorted = new ArrayList<>(agg.entrySet());
            sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            int top = Math.min(8, sorted.size());
            List<String> labels = new ArrayList<>();
            List<Double> values = new ArrayList<>();
            for (int i = 0; i < top; i++) {
                labels.add(String.valueOf(sorted.get(i).getKey()));
                values.add(sorted.get(i).getValue());
            }
            ChartGenerator.ChartData chart = ChartGenerator.ChartData.builder()
                    .type(ChartGenerator.ChartType.BAR)
                    .title(num + " by " + cat)
                    .categories(labels)
                    .addSeries(num, values)
                    .build();
            dcb.addCard(new DashboardCard(
                    DashboardCard.CardType.CHART, num + " 分布", chart
            ).colSpan(2));
        }

        // 数据表
        if (!numericCols.isEmpty() && !catCols.isEmpty()) {
            List<String> cols = new ArrayList<>();
            cols.add(catCols.get(0));
            for (int i = 0; i < Math.min(3, numericCols.size()); i++) cols.add(numericCols.get(i));
            List<List<String>> tableRows = new ArrayList<>();
            for (int i = 0; i < Math.min(10, rows.size()); i++) {
                List<String> tr = new ArrayList<>();
                for (String c : cols) {
                    Object v = rows.get(i).get(c);
                    tr.add(v == null ? "" : v.toString());
                }
                tableRows.add(tr);
            }
            dcb.addCard(new DashboardCard(
                    DashboardCard.CardType.TABLE, "明细数据", new TableData(cols, tableRows)
            ).colSpan(2));
        }

        return render(dcb.build());
    }

    /**
     * 给 DashboardCard 设置 colSpan (链式)
     * (本类已在文件上部定义, 这里只补 colSpan/rowSpan 方法, 但同名类重复)
     */
}
