package com.minimax.ai.tool.builtin;

import com.minimax.ai.tool.builtin.ppt.OutlineParser;
import com.minimax.ai.tool.builtin.ppt.PptRenderer;
import com.minimax.ai.tool.builtin.ppt.PptTheme;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PPT 生成工具 (V3.0.2 自研)
 *
 * <p>功能: 输入大纲 (Markdown / JSON / 纯文本) + 主题, 输出 .pptx 文件 (Base64)
 *
 * <h3>输入参数</h3>
 * <ul>
 *   <li><b>outline</b> (String, 必填): 大纲文本 (Markdown/JSON/纯文本)</li>
 *   <li><b>theme</b> (String, 可选): 主题名 (商务蓝/暗夜/自然绿/暖橙, 默认商务蓝)</li>
 *   <li><b>title</b> (String, 可选): 大纲为空时, 自动生成的 PPT 主题</li>
 *   <li><b>pageCount</b> (int, 可选): 自动生成时页数 (3-20, 默认 6)</li>
 * </ul>
 *
 * <h3>输出</h3>
 * <ul>
 *   <li><b>base64</b> (String): PPTX 文件 base64 编码</li>
 *   <li><b>size</b> (int): 文件字节数</li>
 *   <li><b>slideCount</b> (int): Slide 数</li>
 *   <li><b>theme</b> (String): 实际使用主题</li>
 *   <li><b>slides</b> (List): 渲染的 slide 概要</li>
 * </ul>
 *
 * <h3>算法复杂度</h3>
 * O(N) — N 为 slide 数
 *
 * <h3>与 AI 集成</h3>
 * 用户说 "做一个 AI 主题的 PPT", 关键词引擎路由到此工具, AI 先自动生成大纲, 再调用本工具渲染
 */
@Slf4j
@Component
public class PptGenTool extends AbstractSimpleTool {

    /** 工具唯一 code (注册用) */
    @Override
    public String getCode() { return "ppt.gen"; }

    /** 工具名 */
    @Override
    public String getName() { return "PPT 生成"; }

    /** 工具描述 (供关键词引擎路由) */
    @Override
    public String getDescription() {
        return "生成 PPT/演示文稿 (.pptx), 支持 Markdown/JSON/纯文本大纲, 4 套主题";
    }

    /** 工具分类 */
    @Override
    public String getCategory() { return "office"; }

    /**
     * 主执行 (继承自 AbstractSimpleTool)
     *
     * @param input 输入参数 (outline, theme?, title?, pageCount?)
     * @return 渲染结果 (含 base64)
     */
    @Override
    protected Map<String, Object> doExecute(Map<String, Object> input) throws Exception {
        // 1. 解析参数
        String outline = (String) input.get("outline");
        String themeName = (String) input.getOrDefault("theme", "商务蓝");
        String title = (String) input.getOrDefault("title", "演示文稿");
        int pageCount = ((Number) input.getOrDefault("pageCount", 6)).intValue();

        // 2. 解析大纲
        OutlineParser parser = new OutlineParser();
        List<OutlineParser.Slide> slides;
        if (outline == null || outline.trim().isEmpty()) {
            // 2a. 无大纲 → 自动生成
            log.info("[ppt-gen] 无大纲, 自动生成主题={} 页数={}", title, pageCount);
            slides = parser.autoGenerate(title, pageCount);
        } else {
            // 2b. 解析用户大纲
            log.info("[ppt-gen] 解析用户大纲 ({} 字符), 主题={}", outline.length(), themeName);
            slides = parser.parse(outline);
        }

        // 3. 选主题
        PptTheme theme = PptTheme.fromName(themeName);
        log.info("[ppt-gen] 使用主题: {} ({})", theme.getName(), theme.name());

        // 4. 渲染 PPTX
        PptRenderer renderer = new PptRenderer();
        byte[] pptBytes = renderer.render(slides, theme);
        log.info("[ppt-gen] 渲染完成: {} 页, {} 字节", slides.size(), pptBytes.length);

        // 5. 构造返回
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("base64", Base64.getEncoder().encodeToString(pptBytes));  // PPTX base64
        result.put("size", pptBytes.length);                                    // 字节数
        result.put("slideCount", slides.size());                                // slide 数
        result.put("theme", theme.getName());                                   // 主题名 (中文)
        result.put("filename", sanitizeFilename(title) + ".pptx");              // 建议文件名

        // 6. 返回 slide 概要 (调试用)
        List<Map<String, Object>> slideSummary = new java.util.ArrayList<>();
        for (int i = 0; i < slides.size(); i++) {
            OutlineParser.Slide s = slides.get(i);
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("index", i + 1);
            sm.put("type", s.type);
            sm.put("title", s.title);
            sm.put("bulletCount", s.bullets == null ? 0 : s.bullets.size());
            slideSummary.add(sm);
        }
        result.put("slides", slideSummary);
        return result;
    }

    /**
     * 清理文件名 (去除非法字符)
     */
    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "presentation";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
