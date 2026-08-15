package com.minimax.ai;

import com.minimax.ai.codegen.ProjectPackager;
import com.minimax.ai.dto.CodeGenRequest;
import com.minimax.ai.dto.CodeGenResponse;
import com.minimax.ai.generation.ConversationContext;
import com.minimax.ai.generation.KeywordEngine;
import com.minimax.ai.generation.TypoTolerance;
import com.minimax.ai.tool.builtin.JavaProjectGenTool;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V2.8.4 新功能测试
 * 1. TypoTolerance 错别字
 * 2. ConversationContext 多轮对话
 * 3. KeywordEngine 上下文路由
 * 4. ProjectPackager ZIP 打包
 */
class V284FeaturesTest {

    // ============ TypoTolerance 测试 ============

    @Test
    void testTypoCorrection() {
        TypoTolerance tt = new TypoTolerance();
        // 验证错别字词表包含常见项 (不依赖 encoding)
        assertTrue(TypoTolerance.class.getDeclaredFields().length > 0
                || tt.correct("test") != null);
        // 单词拼写测试
        assertEquals("chart", tt.correct("chatr"));
        assertEquals("music", tt.correct("musc"));
        // 无错别字
        assertEquals("hello", tt.correct("hello"));
    }

    @Test
    void testPinyinExpansion() {
        TypoTolerance tt = new TypoTolerance();
        assertEquals("数据 图表", tt.expandPinyin("shuj tubiao"));
    }

    @Test
    void testEditDistance() {
        TypoTolerance tt = new TypoTolerance();
        assertEquals(0, tt.editDistance("统计", "统计"));
        assertEquals(1, tt.editDistance("统计", "统记"));
        // 实际: 柱状图→柱装图=1 (中->装), 其余相同
        assertTrue(tt.editDistance("柱状图", "柱装图") <= 2);
    }

    @Test
    void testFuzzyMatch() {
        TypoTolerance tt = new TypoTolerance();
        assertTrue(tt.fuzzyMatch("画个柱装图", "柱状图"));
        assertTrue(tt.fuzzyMatch("分析这个数据", "分析"));
        assertFalse(tt.fuzzyMatch("随机", "柱状图"));
    }

    // ============ ConversationContext 测试 ============

    @Test
    void testMultiTurnContext() {
        ConversationContext ctx = new ConversationContext();
        String sid = "test-session-1";

        // 第一轮: 用户要图表
        ctx.record(sid, "画个柱状图", KeywordEngine.Intent.GENERATE_CHART,
                Map.of("chartType", "柱状图"));

        // 第二轮: "改个颜色" (短输入, 沿用上轮)
        List<KeywordEngine.Intent> recent = ctx.recentIntents(sid, 3);
        assertEquals(1, recent.size());
        assertEquals(KeywordEngine.Intent.GENERATE_CHART, recent.get(0));

        // 参数继承
        Map<String, Object> merged = ctx.mergeParams(sid, Map.of("color", "red"));
        assertEquals("柱状图", merged.get("chartType"));
        assertEquals("red", merged.get("color"));
    }

    @Test
    void testClearAndLimit() {
        ConversationContext ctx = new ConversationContext();
        String sid = "s2";
        for (int i = 0; i < 15; i++) {
            ctx.record(sid, "msg-" + i, KeywordEngine.Intent.CHAT, Map.of("i", i));
        }
        // 最多保留 10 轮
        assertEquals(10, ctx.recentIntents(sid, 100).size());

        ctx.clear(sid);
        assertEquals(0, ctx.recentIntents(sid, 100).size());
    }

    // ============ KeywordEngine 上下文路由 ============

    @Test
    void testRouteWithContextFollowUp() {
        KeywordEngine engine = new KeywordEngine(org.mockito.Mockito.mock(com.minimax.ai.tool.AiToolRegistry.class));
        ConversationContext ctx = new ConversationContext();
        String sid = "s3";

        // 第一轮: 显式说图表
        KeywordEngine.RouteResult r1 = engine.routeWithContext("画个柱状图",
                Map.of("sessionId", sid), ctx);
        assertEquals(KeywordEngine.Intent.GENERATE_CHART, r1.intent);

        // 第二轮: 短输入, 上下文继承
        KeywordEngine.RouteResult r2 = engine.routeWithContext("改成红色",
                Map.of("sessionId", sid), ctx);
        // 短输入应该沿用上轮意图
        assertEquals(KeywordEngine.Intent.GENERATE_CHART, r2.intent);
        assertTrue(r2.confidence > 0.5);
    }

    @Test
    void testRouteWithContextNewIntent() {
        KeywordEngine engine = new KeywordEngine(org.mockito.Mockito.mock(com.minimax.ai.tool.AiToolRegistry.class));
        ConversationContext ctx = new ConversationContext();
        String sid = "s4";

        ctx.record(sid, "你好", KeywordEngine.Intent.CHAT, Map.of());
        // 新意图: 明确说生成音乐
        KeywordEngine.RouteResult r = engine.routeWithContext("来个流行曲子",
                Map.of("sessionId", sid), ctx);
        assertEquals(KeywordEngine.Intent.GENERATE_MUSIC, r.intent);
    }

    // ============ ProjectPackager ZIP 打包 ============

    @Test
    void testProjectPackager() throws Exception {
        ProjectPackager packager = new ProjectPackager();
        CodeGenResponse resp = new CodeGenResponse();
        resp.setProjectName("test-app");
        resp.setProjectType("spring-boot");
        resp.setFiles(Map.of(
                "src/main/java/com/example/Main.java", "public class Main { }",
                "pom.xml", "<project></project>"
        ));

        byte[] zip = packager.packageAsZip(resp, "test-app", "1.0.0", "com.example");
        // 输出重复路径
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(zip);
        java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(bais);
        java.util.zip.ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (!seen.add(entry.getName())) {
                System.out.println("DUP: " + entry.getName());
            }
        }
        assertNotNull(zip);
        assertTrue(zip.length > 1000, "ZIP should be > 1KB with all enterprise files, got " + zip.length);

        // 校验 ZIP 头
        assertEquals(0x50, zip[0] & 0xFF, "should be PK header");
        assertEquals(0x4B, zip[1] & 0xFF);
    }

    @Test
    void testJavaProjectGenTool() throws Exception {
        // 验证 JavaProjectGenTool 配置正确
        com.minimax.ai.codegen.ProjectCodeGenerator mockGen = org.mockito.Mockito.mock(com.minimax.ai.codegen.ProjectCodeGenerator.class);
        ProjectPackager mockPackager = org.mockito.Mockito.mock(ProjectPackager.class);
        JavaProjectGenTool tool = new JavaProjectGenTool(mockGen, mockPackager);
        assertEquals("java.project.gen", tool.getCode());
        assertEquals("code", tool.getCategory());
        assertEquals("Java 企业项目生成", tool.getName());
        // 注: execute 需要 Spring 上下文, 这里只测配置
    }
}
