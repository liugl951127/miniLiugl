package com.minimax.ai.controller;

import com.minimax.common.result.Result;
import com.minimax.ai.tool.AiToolRegistry;
import com.minimax.ai.tool.AiToolExecutor;
import com.minimax.ai.tool.AiToolRegistry.ToolResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PPT 生成控制器 (V3.0.2)
 *
 * <p>API 列表 (统一 /api/v1/ai/ppt 前缀):
 * <ul>
 *   <li>POST /generate  生成 PPT (返回 base64 + 元信息)</li>
 *   <li>POST /auto      自动生成 (无大纲, 只给主题)</li>
 *   <li>GET  /themes    列出可用主题</li>
 *   <li>POST /parse     解析大纲预览 (不渲染, 只看解析结果)</li>
 * </ul>
 *
 * <h3>调用方式</h3>
 * <pre>
 * POST /api/v1/ai/ppt/generate
 * {
 *   "outline": "# 我的演讲\n## 第一部分\n- 要点 1",
 *   "theme": "商务蓝"
 * }
 * </pre>
 */
@Tag(name = "AI PPT 生成")
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/ppt")  // 统一 /api/v1 前缀
@RequiredArgsConstructor
public class PptGenController {

    /** 工具注册表 (PptGenTool 通过 @Component 自动注册) */
    private final AiToolRegistry registry;

    /**
     * 生成 PPT
     *
     * @param body { outline?, theme?, title?, pageCount? }
     * @return Result { base64, size, slideCount, theme, filename, slides }
     */
    @Operation(summary = "生成 PPT (返回 base64)")
    @PostMapping("/generate")
    public Result<Map<String, Object>> generate(@RequestBody Map<String, Object> body) {
        // 1. 取注册表中的 PptGenTool
        AiToolExecutor tool = registry.getExecutor("ppt.gen");
        if (tool == null) {
            return Result.fail(500, "PPT 工具未注册");
        }
        // 2. 准备输入
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("outline", body.get("outline"));
        input.put("theme", body.getOrDefault("theme", "商务蓝"));
        input.put("title", body.getOrDefault("title", "演示文稿"));
        input.put("pageCount", body.getOrDefault("pageCount", 6));

        // 3. 调用
        log.info("[ppt-ctrl] 调用 ppt.gen, input keys: {}", input.keySet());
        ToolResult result = registry.invoke("ppt.gen", input, null, null, null);
        if (!result.isSuccess()) {
            return Result.fail(500, "PPT 生成失败: " + result.getError());
        }

        // 4. 返回
        return Result.ok((Map<String, Object>) result.getOutput());
    }

    /**
     * 自动生成 (无大纲, 只给主题 + 页数)
     *
     * @param body { title, pageCount?, theme? }
     */
    @Operation(summary = "自动生成 PPT (只给主题)")
    @PostMapping("/auto")
    public Result<Map<String, Object>> auto(@RequestBody Map<String, Object> body) {
        // 1. 转发到 /generate, 但 outline=null
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("title", body.getOrDefault("title", "演示文稿"));
        input.put("theme", body.getOrDefault("theme", "商务蓝"));
        input.put("pageCount", body.getOrDefault("pageCount", 6));
        // 2. 调 tool
        ToolResult result = registry.invoke("ppt.gen", input, null, null, null);
        if (!result.isSuccess()) {
            return Result.fail(500, "PPT 自动生成失败: " + result.getError());
        }
        return Result.ok((Map<String, Object>) result.getOutput());
    }

    /**
     * 列出可用主题
     */
    @Operation(summary = "列出可用主题")
    @GetMapping("/themes")
    public Result<List<Map<String, Object>>> themes() {
        // 直接返回 4 套主题
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (com.minimax.ai.tool.builtin.ppt.PptTheme t : com.minimax.ai.tool.builtin.ppt.PptTheme.values()) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("code", t.name());                                  // 英文 code
            info.put("name", t.getName());                                // 中文名
            info.put("background", String.format("#%02X%02X%02X", t.getBackground().getRed(), t.getBackground().getGreen(), t.getBackground().getBlue()));
            info.put("title", String.format("#%02X%02X%02X", t.getTitle().getRed(), t.getTitle().getGreen(), t.getTitle().getBlue()));
            info.put("accent", String.format("#%02X%02X%02X", t.getAccent().getRed(), t.getAccent().getGreen(), t.getAccent().getBlue()));
            list.add(info);
        }
        return Result.ok(list);
    }

    /**
     * 解析大纲预览 (不渲染)
     */
    @Operation(summary = "解析大纲预览 (不生成 PPT)")
    @PostMapping("/parse")
    public Result<Map<String, Object>> parse(@RequestBody Map<String, Object> body) {
        String outline = (String) body.get("outline");
        com.minimax.ai.tool.builtin.ppt.OutlineParser parser = new com.minimax.ai.tool.builtin.ppt.OutlineParser();
        // 1. 解析
        List<com.minimax.ai.tool.builtin.ppt.OutlineParser.Slide> slides = parser.parse(outline);
        // 2. 构造预览
        List<Map<String, Object>> preview = new java.util.ArrayList<>();
        for (int i = 0; i < slides.size(); i++) {
            com.minimax.ai.tool.builtin.ppt.OutlineParser.Slide s = slides.get(i);
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("index", i + 1);
            sm.put("type", s.type);
            sm.put("title", s.title);
            sm.put("subtitle", s.subtitle);
            sm.put("bullets", s.bullets);
            preview.add(sm);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("slideCount", slides.size());
        result.put("slides", preview);
        return Result.ok(result);
    }
}
