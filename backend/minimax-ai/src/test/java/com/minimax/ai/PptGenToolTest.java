package com.minimax.ai;

import com.minimax.ai.tool.builtin.PptGenTool;
import com.minimax.ai.tool.builtin.ppt.OutlineParser;
import com.minimax.ai.tool.builtin.ppt.PptRenderer;
import com.minimax.ai.tool.builtin.ppt.PptTheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PPT 生成工具测试 (V3.0.2 自研)
 *
 * <p>覆盖:
 *   1. PptTheme 主题查找
 *   2. OutlineParser 三种格式
 *   3. OutlineParser 自动生成
 *   4. PptRenderer 渲染 4 套主题
 *   5. PptGenTool 端到端
 *   6. PptGenTool 异常情况
 */
class PptGenToolTest {

    /** 工具实例 */
    private final PptGenTool tool = new PptGenTool();

    /**
     * 测试 1: 主题按名查找
     */
    @Test
    @DisplayName("1. PptTheme.fromName 中文/英文/不区分大小写")
    void testThemeFromName() {
        assertEquals(PptTheme.BUSINESS, PptTheme.fromName("商务蓝"));
        assertEquals(PptTheme.BUSINESS, PptTheme.fromName("business"));
        assertEquals(PptTheme.BUSINESS, PptTheme.fromName("BUSINESS"));
        assertEquals(PptTheme.DARK, PptTheme.fromName("暗夜"));
        assertEquals(PptTheme.DARK, PptTheme.fromName("DARK"));
        assertEquals(PptTheme.NATURE, PptTheme.fromName("自然绿"));
        assertEquals(PptTheme.WARM, PptTheme.fromName("暖橙"));
        // 兜底: 不存在的名 → BUSINESS
        assertEquals(PptTheme.BUSINESS, PptTheme.fromName("不存在的颜色"));
        // 兜底: null
        assertEquals(PptTheme.BUSINESS, PptTheme.fromName(null));
    }

    /**
     * 测试 2: Markdown 风格大纲解析
     */
    @Test
    @DisplayName("2. OutlineParser 解析 Markdown 大纲")
    void testMarkdownParse() {
        String md = "# 我的演讲\n## 第一部分\n- 要点 1\n- 要点 2\n## 第二部分\n- 要点 3";
        OutlineParser parser = new OutlineParser();
        List<OutlineParser.Slide> slides = parser.parse(md);
        assertEquals(3, slides.size(), "应解析出 3 个 slide (cover + 2 content)");
        assertEquals("我的演讲", slides.get(0).title);
        assertEquals("cover", slides.get(0).type);
        assertEquals("第一部分", slides.get(1).title);
        assertEquals(2, slides.get(1).bullets.size());
        assertEquals("要点 1", slides.get(1).bullets.get(0));
    }

    /**
     * 测试 3: JSON 风格大纲解析
     */
    @Test
    @DisplayName("3. OutlineParser 解析 JSON 大纲")
    void testJsonParse() {
        String json = "[{\"title\":\"封面\",\"subtitle\":\"副标题\"}," +
                "{\"title\":\"内容\",\"bullets\":[\"A\",\"B\"],\"type\":\"content\"}]";
        OutlineParser parser = new OutlineParser();
        List<OutlineParser.Slide> slides = parser.parse(json);
        assertEquals(2, slides.size());
        assertEquals("封面", slides.get(0).title);
        assertEquals("副标题", slides.get(0).subtitle);
        assertEquals(2, slides.get(1).bullets.size());
        assertEquals("A", slides.get(1).bullets.get(0));
    }

    /**
     * 测试 4: 纯文本风格
     */
    @Test
    @DisplayName("4. OutlineParser 解析纯文本大纲")
    void testPlainParse() {
        String text = "封面: 我的演讲\n要点 1\n要点 2\n下一章: 详情";
        OutlineParser parser = new OutlineParser();
        List<OutlineParser.Slide> slides = parser.parse(text);
        assertEquals(2, slides.size());
        assertEquals("封面", slides.get(0).title);
        assertEquals("我的演讲", slides.get(0).subtitle);
        assertEquals(2, slides.get(0).bullets.size());
    }

    /**
     * 测试 5: 自动生成大纲
     */
    @Test
    @DisplayName("5. OutlineParser.autoGenerate 自动生成 N 页")
    void testAutoGenerate() {
        OutlineParser parser = new OutlineParser();
        List<OutlineParser.Slide> slides = parser.autoGenerate("AI 入门", 6);
        assertEquals(6, slides.size(), "6 页");
        assertEquals("cover", slides.get(0).type);
        assertEquals("AI 入门", slides.get(0).title);
        assertEquals("closing", slides.get(slides.size() - 1).type);
    }

    /**
     * 测试 6: 自动生成边界
     */
    @Test
    @DisplayName("6. OutlineParser.autoGenerate 边界 (<3 → 3, >20 → 20)")
    void testAutoGenerateBounds() {
        OutlineParser parser = new OutlineParser();
        assertEquals(3, parser.autoGenerate("X", 1).size(), "<3 应夹到 3");
        assertEquals(3, parser.autoGenerate("X", 0).size());
        assertEquals(20, parser.autoGenerate("X", 100).size(), ">20 应夹到 20");
    }

    /**
     * 测试 7: 渲染 4 套主题都能输出 PPTX
     */
    @Test
    @DisplayName("7. PptRenderer 渲染 4 套主题 (输出 .pptx)")
    void testRenderAllThemes() throws Exception {
        PptRenderer renderer = new PptRenderer();
        OutlineParser parser = new OutlineParser();
        List<OutlineParser.Slide> slides = parser.autoGenerate("测试", 4);
        for (PptTheme theme : PptTheme.values()) {
            byte[] data = renderer.render(slides, theme);
            assertNotNull(data);
            assertTrue(data.length > 0, theme.getName() + " 应有内容");
            // PPTX 文件签名: PK (ZIP 格式)
            assertEquals('P', data[0] & 0xFF, theme.getName() + " 应是 ZIP 格式");
            assertEquals('K', data[1] & 0xFF);
        }
    }

    /**
     * 测试 8: PptGenTool 端到端 (无大纲, 自动生成)
     */
    @Test
    @DisplayName("8. PptGenTool 自动生成 (无大纲)")
    void testPptGenAuto() throws Exception {
        Map<String, Object> input = new HashMap<>();
        input.put("title", "我的演讲");
        input.put("pageCount", 5);
        input.put("theme", "商务蓝");
        @SuppressWarnings("unchecked") Map<String, Object> result = (Map<String, Object>) tool.execute(null, input);
        assertNotNull(result);
        assertEquals(true, result.get("success"));
        assertNotNull(result.get("base64"));
        assertTrue(result.get("size") instanceof Integer);
        assertEquals(5, result.get("slideCount"));
        assertEquals("商务蓝", result.get("theme"));
        assertEquals("我的演讲.pptx", result.get("filename"));
        // 验证 base64 可解码
        byte[] decoded = Base64.getDecoder().decode((String) result.get("base64"));
        assertTrue(decoded.length > 100, "PPTX 应有内容");
    }

    /**
     * 测试 9: PptGenTool 带 Markdown 大纲
     */
    @Test
    @DisplayName("9. PptGenTool 解析 Markdown 大纲生成")
    void testPptGenWithMarkdown() throws Exception {
        Map<String, Object> input = new HashMap<>();
        input.put("outline", "# 标题\n## 章节 1\n- 要点 A\n- 要点 B");
        input.put("theme", "暗夜");
        @SuppressWarnings("unchecked") Map<String, Object> result = (Map<String, Object>) tool.execute(null, input);
        assertTrue((Boolean) result.get("success"));
        assertEquals(2, result.get("slideCount"));  // cover + content
        assertEquals("暗夜", result.get("theme"));
    }

    /**
     * 测试 10: 主题切换
     */
    @Test
    @DisplayName("10. PptGenTool 4 套主题分别输出")
    void testPptGenThemes() throws Exception {
        for (PptTheme theme : PptTheme.values()) {
            Map<String, Object> input = new HashMap<>();
            input.put("title", "测试 " + theme.getName());
            input.put("pageCount", 3);
            input.put("theme", theme.name());
            @SuppressWarnings("unchecked") Map<String, Object> result = (Map<String, Object>) tool.execute(null, input);
            assertTrue((Boolean) result.get("success"), theme.getName() + " 失败");
            assertEquals(theme.getName(), result.get("theme"));
        }
    }

    /**
     * 测试 11: 文件名清理 (去除非法字符)
     */
    @Test
    @DisplayName("11. PptGenTool 文件名清理")
    void testFilenameSanitize() throws Exception {
        Map<String, Object> input = new HashMap<>();
        input.put("title", "非法/字符\\测试:文件名");
        @SuppressWarnings("unchecked") Map<String, Object> result = (Map<String, Object>) tool.execute(null, input);
        String filename = (String) result.get("filename");
        // 应不包含 \ / : * ? " < > |
        assertFalse(filename.contains("/"));
        assertFalse(filename.contains("\\"));
        assertFalse(filename.contains(":"));
    }

    /**
     * 测试 12: 工具元信息
     */
    @Test
    @DisplayName("12. 工具元信息 (code/name/category)")
    void testToolMetadata() {
        assertEquals("ppt.gen", tool.getCode());
        assertEquals("PPT 生成", tool.getName());
        assertEquals("office", tool.getCategory());
        assertNotNull(tool.getDescription());
    }
}
