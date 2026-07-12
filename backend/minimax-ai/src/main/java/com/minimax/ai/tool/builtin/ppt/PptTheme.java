package com.minimax.ai.tool.builtin.ppt;

import org.apache.poi.sl.usermodel.ColorStyle;

import java.awt.Color;

/**
 * PPT 主题/样式系统 (V3.0.2 自研)
 *
 * <p>预定义 4 套主题配色, 每套含:
 *   - 背景色
 *   - 标题色
 *   - 正文色
 *   - 强调色 (callout/数字)
 *   - 副标题色
 *
 * <p>每行代码注释: 参数色值, 用途, RGB 来源
 */
public enum PptTheme {

    // === 商务蓝 (默认, 适合企业汇报) ===
    BUSINESS {
        @Override
        public String getName() { return "商务蓝"; }
        @Override
        public Color getBackground() { return new Color(0xF8, 0xFA, 0xFC); }  // 极浅蓝灰
        @Override
        public Color getTitle() { return new Color(0x1E, 0x40, 0x76); }       // 深蓝
        @Override
        public Color getSubtitle() { return new Color(0x5B, 0x7A, 0xA6); }    // 中蓝
        @Override
        public Color getBody() { return new Color(0x33, 0x33, 0x33); }         // 接近黑
        @Override
        public Color getAccent() { return new Color(0xE6, 0x7E, 0x22); }       // 橙色 (对比)
    },

    // === 暗夜 (科技/数据演示) ===
    DARK {
        @Override
        public String getName() { return "暗夜"; }
        @Override
        public Color getBackground() { return new Color(0x1A, 0x1A, 0x2E); }   // 深紫黑
        @Override
        public Color getTitle() { return new Color(0xF1, 0xC4, 0x0F); }       // 金黄
        @Override
        public Color getSubtitle() { return new Color(0x95, 0xA5, 0xA6); }    // 灰
        @Override
        public Color getBody() { return new Color(0xEC, 0xF0, 0xF1); }         // 浅灰
        @Override
        public Color getAccent() { return new Color(0x00, 0xBC, 0xD4); }       // 青色
    },

    // === 自然绿 (健康/环保/教学) ===
    NATURE {
        @Override
        public String getName() { return "自然绿"; }
        @Override
        public Color getBackground() { return new Color(0xF1, 0xF8, 0xE9); }   // 极浅绿
        @Override
        public Color getTitle() { return new Color(0x2E, 0x7D, 0x32); }       // 深绿
        @Override
        public Color getSubtitle() { return new Color(0x68, 0x8E, 0x53); }    // 草绿
        @Override
        public Color getBody() { return new Color(0x21, 0x21, 0x21); }         // 黑
        @Override
        public Color getAccent() { return new Color(0xFF, 0x6F, 0x00); }       // 橙 (对比)
    },

    // === 暖橙 (营销/演讲) ===
    WARM {
        @Override
        public String getName() { return "暖橙"; }
        @Override
        public Color getBackground() { return new Color(0xFF, 0xF8, 0xE1); }   // 米黄
        @Override
        public Color getTitle() { return new Color(0xBF, 0x36, 0x0C); }       // 砖红
        @Override
        public Color getSubtitle() { return new Color(0xE6, 0x51, 0x00); }    // 橙
        @Override
        public Color getBody() { return new Color(0x3E, 0x27, 0x21); }         // 咖啡黑
        @Override
        public Color getAccent() { return new Color(0x1B, 0x5E, 0x20); }       // 深绿
    };

    /** 主题名 */
    public abstract String getName();

    /** 背景色 */
    public abstract Color getBackground();

    /** 标题色 */
    public abstract Color getTitle();

    /** 副标题色 */
    public abstract Color getSubtitle();

    /** 正文字色 */
    public abstract Color getBody();

    /** 强调色 (高亮/数字) */
    public abstract Color getAccent();

    /**
     * 按名查找主题 (大小写不敏感)
     *
     * @param name 主题名 (e.g. "商务蓝" / "business" / "DARK")
     * @return 主题, 未找到用 BUSINESS
     */
    public static PptTheme fromName(String name) {
        if (name == null || name.isBlank()) return BUSINESS;       // 兜底默认
        String lower = name.toLowerCase().trim();
        for (PptTheme t : values()) {
            if (t.name().toLowerCase().equals(lower)) return t;   // 英文名
            if (t.getName().equals(name.trim())) return t;        // 中文名
        }
        return BUSINESS;                                            // 未匹配, 兜底
    }
}
