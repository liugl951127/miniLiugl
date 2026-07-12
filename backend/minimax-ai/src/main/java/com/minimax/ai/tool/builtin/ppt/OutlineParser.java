package com.minimax.ai.tool.builtin.ppt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PPT 大纲解析器 (V3.0.2 自研)
 *
 * <p>支持 3 种输入格式:
 * <ol>
 *   <li><b>Markdown 风格</b>: "# 标题" + "- 要点" (1 级)
 *       <pre>
 *       # 我的演讲
 *       ## 第一部分
 *       - 要点 1
 *       - 要点 2
 *       ## 第二部分
 *       - 要点 3
 *       </pre>
 *   </li>
 *   <li><b>JSON 风格</b>: [{"title":"...","bullets":["..."]}]
 *       <pre>
 *       [{"title":"封面","subtitle":"副标题"},
 *        {"title":"第一章","bullets":["要点1","要点2"]}]
 *       </pre>
 *   </li>
 *   <li><b>纯文本风格</b>: 每行一个 slide, 标题 + ":" + 要点 (用换行)
 *       <pre>
 *       封面: 副标题
 *       第一章:
 *       - 要点 1
 *       - 要点 2
 *       </pre>
 *   </li>
 * </ol>
 *
 * <p>自动检测格式, 统一输出 List&lt;Slide&gt;
 */
public class OutlineParser {

    /** Slide 数据结构 (标题 + 副标题 + 要点) */
    public static class Slide {
        public String title;       // 标题
        public String subtitle;    // 副标题 (可选)
        public List<String> bullets = new ArrayList<>();  // 要点列表
        public String type = "content";  // 类型: cover/title/content/closing

        @Override
        public String toString() {
            // 用于日志/调试
            return String.format("Slide[%s: %s, %d bullets]", type, title, bullets.size());
        }
    }

    /** Markdown 标题正则: # 或 ## */
    private static final Pattern MD_TITLE = Pattern.compile("^(#{1,2})\\s+(.+)$");
    /** JSON 数组开始 */
    private static final Pattern JSON_ARRAY = Pattern.compile("^\\s*\\[.*\\]\\s*$", Pattern.DOTALL);

    /**
     * 解析大纲 (主入口)
     *
     * @param rawText 原始文本
     * @return Slide 列表
     */
    public List<Slide> parse(String rawText) {
        // 1. 空值保护
        if (rawText == null || rawText.trim().isEmpty()) {
            return defaultOutline();
        }
        // 2. 自动检测格式
        String trimmed = rawText.trim();
        if (JSON_ARRAY.matcher(trimmed).matches()) {
            return parseJson(trimmed);                    // JSON 风格
        } else if (trimmed.contains("# ")) {
            return parseMarkdown(trimmed);                 // Markdown 风格
        } else {
            return parsePlain(trimmed);                    // 纯文本风格
        }
    }

    /**
     * 解析 Markdown 风格
     *
     * <p>规则:
     *   - "# 标题" → 新 slide (cover)
     *   - "## 标题" → 新 slide (content)
     *   - "- 文本" → 加入当前 slide 的 bullets
     *   - 空行 → 忽略
     */
    private List<Slide> parseMarkdown(String text) {
        List<Slide> slides = new ArrayList<>();          // 结果
        Slide current = null;                             // 当前 slide
        String[] lines = text.split("\\r?\\n");           // 按行分割
        for (String line : lines) {
            // 1. 去除行首尾空白
            String l = line.trim();
            if (l.isEmpty()) continue;                    // 空行跳过
            // 2. 匹配标题
            Matcher m = MD_TITLE.matcher(l);
            if (m.find()) {
                // 2a. # 标题 → 封面; ## 标题 → content
                int level = m.group(1).length();
                current = new Slide();
                current.title = m.group(2).trim();
                current.type = (level == 1 && slides.isEmpty()) ? "cover" : "content";
                slides.add(current);
            } else if (l.startsWith("- ")) {
                // 3. - 要点 → 加入当前 slide
                if (current == null) {
                    // 3a. 没有标题, 兜底创建
                    current = new Slide();
                    current.title = "未命名章节";
                    current.type = "content";
                    slides.add(current);
                }
                current.bullets.add(l.substring(2).trim());
            } else {
                // 4. 其它行: 当作当前 slide 的副标题或附加要点
                if (current == null) {
                    current = new Slide();
                    current.title = l;
                    current.type = "content";
                    slides.add(current);
                } else if (current.bullets.isEmpty() && current.subtitle == null) {
                    current.subtitle = l;                 // 副标题
                } else {
                    current.bullets.add(l);               // 附加要点
                }
            }
        }
        return slides.isEmpty() ? defaultOutline() : slides;
    }

    /**
     * 解析 JSON 风格 (简化, 不引入 JSON 库避免依赖)
     *
     * <p>格式: [{"title":"...", "subtitle":"...", "bullets":["..."]}, ...]
     * 简化解析: 用正则提取 title 和 bullets
     */
    private List<Slide> parseJson(String text) {
        List<Slide> slides = new ArrayList<>();
        // 1. 匹配每个 {} 对象
        Pattern objPat = Pattern.compile("\\{([^}]*)\\}");
        Matcher objM = objPat.matcher(text);
        while (objM.find()) {
            String body = objM.group(1);
            Slide s = new Slide();
            s.type = "content";
            // 2. 提取 title
            Matcher tM = Pattern.compile("\"title\"\\s*:\\s*\"([^\"]*)\"").matcher(body);
            if (tM.find()) s.title = tM.group(1);
            // 3. 提取 subtitle
            Matcher stM = Pattern.compile("\"subtitle\"\\s*:\\s*\"([^\"]*)\"").matcher(body);
            if (stM.find()) s.subtitle = stM.group(1);
            // 4. 提取 bullets 数组
            Matcher bM = Pattern.compile("\"bullets\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(body);
            if (bM.find()) {
                String bulletsStr = bM.group(1);
                Matcher bm = Pattern.compile("\"([^\"]*)\"").matcher(bulletsStr);
                while (bm.find()) s.bullets.add(bm.group(1));
            }
            // 5. 提取 type
            Matcher tyM = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]*)\"").matcher(body);
            if (tyM.find()) s.type = tyM.group(1);
            if (s.title != null) slides.add(s);
        }
        return slides.isEmpty() ? defaultOutline() : slides;
    }

    /**
     * 解析纯文本风格
     *
     * <p>规则:
     *   - "标题: 副标题" → 新 slide
     *   - "标题:" → 新 slide, 副标题待定
     *   - "- 文本" → 加入当前 slide 的 bullets
     */
    private List<Slide> parsePlain(String text) {
        List<Slide> slides = new ArrayList<>();
        Slide current = null;
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String l = line.trim();
            if (l.isEmpty()) continue;
            if (l.startsWith("- ")) {
                if (current == null) {
                    current = new Slide();
                    current.title = "未命名";
                    slides.add(current);
                }
                current.bullets.add(l.substring(2).trim());
            } else if (l.contains(":")) {
                int idx = l.indexOf(":");
                current = new Slide();
                current.title = l.substring(0, idx).trim();
                current.subtitle = l.substring(idx + 1).trim();
                current.type = "content";
                slides.add(current);
            } else {
                if (current == null) {
                    current = new Slide();
                    current.title = l;
                    current.type = "content";
                    slides.add(current);
                } else {
                    current.bullets.add(l);
                }
            }
        }
        return slides.isEmpty() ? defaultOutline() : slides;
    }

    /**
     * 默认大纲 (兜底)
     */
    private List<Slide> defaultOutline() {
        List<Slide> slides = new ArrayList<>();
        // 1. 封面
        Slide cover = new Slide();
        cover.title = "演示文稿";
        cover.subtitle = "由 MiniMax AI 自动生成";
        cover.type = "cover";
        slides.add(cover);
        // 2. 内容
        Slide content = new Slide();
        content.title = "主要内容";
        content.bullets = new ArrayList<>(Arrays.asList("第一要点", "第二要点", "第三要点"));
        content.type = "content";
        slides.add(content);
        // 3. 结尾
        Slide end = new Slide();
        end.title = "谢谢观看";
        end.subtitle = "Q & A";
        end.type = "closing";
        slides.add(end);
        return slides;
    }

    /**
     * 简化大纲生成 (根据主题, 自动生成 N 页)
     *
     * <p>用于用户没给大纲时, AI 自动构造
     */
    public List<Slide> autoGenerate(String topic, int pageCount) {
        // 1. 兜底页数
        if (pageCount < 3) pageCount = 3;
        if (pageCount > 20) pageCount = 20;
        List<Slide> slides = new ArrayList<>();
        // 2. 封面
        Slide cover = new Slide();
        cover.title = topic;
        cover.subtitle = "MiniMax AI 自动生成的演示文稿";
        cover.type = "cover";
        slides.add(cover);
        // 3. 中间页: 动态生成 N 个章节 (pageCount - 2 个)
        int contentPages = pageCount - 2;
        // 章节模板 (按页数选)
        String[] templates = {
            "概述", "背景介绍", "核心要点", "技术原理", "架构设计",
            "实现细节", "应用案例", "性能分析", "最佳实践", "常见问题",
            "解决方案", "扩展方向", "未来展望", "总结"
        };
        for (int i = 0; i < contentPages; i++) {
            Slide s = new Slide();
            // 章节名 (循环取模板)
            String sectionName = templates[i % templates.length];
            s.title = (i + 1) + ". " + sectionName + " - " + topic;
            s.bullets = new ArrayList<>(Arrays.asList(
                "关于 " + topic + " 的 " + sectionName,
                "关键概念与定义",
                "实际应用场景与案例",
                "常见问题与解决方案"
            ));
            s.type = "content";
            slides.add(s);
        }
        // 4. 结尾
        Slide end = new Slide();
        end.title = "谢谢观看";
        end.subtitle = topic + " | MiniMax AI";
        end.type = "closing";
        slides.add(end);
        return slides;
    }
}
