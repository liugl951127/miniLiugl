package com.minimax.ai;

import com.minimax.ai.datasource.DynamicDataSource;
import com.minimax.ai.tool.builtin.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V2.8.3 新增 AI 工具测试
 */
class V283ToolsTest {

    @Test
    void testTextSummaryTool() throws Exception {
        TextSummaryTool tool = new TextSummaryTool();
        assertEquals("text.analyze", tool.getCode());
        Map<String, Object> input = new HashMap<>();
        input.put("text", "MiniMax 是企业级 AI 平台。性能优秀, 体验好, 我们都喜欢它。功能丰富, 文档完整, 值得推荐!");
        input.put("task", "all");
        input.put("topK", 5);
        Object r = tool.execute(null, input);
        assertNotNull(r);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) r;
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("summary"));
        assertNotNull(result.get("sentiment"));
        assertNotNull(result.get("entities"));
        assertNotNull(result.get("keywords"));
    }

    @Test
    void testTextSummaryEntityRecognition() throws Exception {
        TextSummaryTool tool = new TextSummaryTool();
        Map<String, Object> input = new HashMap<>();
        input.put("text", "邮箱 admin@minimax.com, 手机 13800138000, 访问 https://minimax.com");
        input.put("task", "entities");
        @SuppressWarnings("unchecked")
        Map<String, Object> r = (Map<String, Object>) tool.execute(null, input);
        @SuppressWarnings("unchecked")
        Map<String, List<String>> entities = (Map<String, List<String>>) r.get("entities");
        assertTrue(entities.get("EMAIL").contains("admin@minimax.com"));
        assertTrue(entities.get("MOBILE").contains("13800138000"));
        assertTrue(entities.get("URL").stream().anyMatch(u -> u.contains("minimax.com")));
    }

    @Test
    void testSentimentAnalysis() throws Exception {
        TextSummaryTool tool = new TextSummaryTool();
        Map<String, Object> input = new HashMap<>();
        input.put("text", "这个产品太棒了! 完美! 我非常喜欢!");
        input.put("task", "sentiment");
        @SuppressWarnings("unchecked")
        Map<String, Object> r = (Map<String, Object>) tool.execute(null, input);
        @SuppressWarnings("unchecked")
        Map<String, Object> sentiment = (Map<String, Object>) r.get("sentiment");
        assertEquals("POSITIVE", sentiment.get("label"));
        assertTrue(((Number) sentiment.get("positiveCount")).intValue() >= 2);
    }

    @Test
    void testDateTimeNow() throws Exception {
        DateTimeTool tool = new DateTimeTool();
        Map<String, Object> input = new HashMap<>();
        input.put("op", "now");
        input.put("timezone", "Asia/Shanghai");
        @SuppressWarnings("unchecked")
        Map<String, Object> r = (Map<String, Object>) tool.execute(null, input);
        assertNotNull(r.get("epochMillis"));
        assertNotNull(r.get("formatted"));
    }

    @Test
    void testDateTimeAdd() throws Exception {
        DateTimeTool tool = new DateTimeTool();
        Map<String, Object> input = new HashMap<>();
        input.put("op", "add");
        input.put("epochMillis", System.currentTimeMillis());
        input.put("days", 7);
        @SuppressWarnings("unchecked")
        Map<String, Object> r = (Map<String, Object>) tool.execute(null, input);
        assertNotNull(r.get("resultEpochMillis"));
        long result = ((Number) r.get("resultEpochMillis")).longValue();
        long orig = ((Number) input.get("epochMillis")).longValue();
        assertEquals(orig + 7L * 86400 * 1000, result);
    }

    @Test
    void testDateTimeDiff() throws Exception {
        DateTimeTool tool = new DateTimeTool();
        Map<String, Object> input = new HashMap<>();
        input.put("op", "diff");
        input.put("epochMillisA", 1000L);
        input.put("epochMillisB", 1000L + 3661 * 1000L);  // 1h 1m 1s
        @SuppressWarnings("unchecked")
        Map<String, Object> r = (Map<String, Object>) tool.execute(null, input);
        assertEquals(3661, ((Number) r.get("diffSeconds")).intValue());
        String human = (String) r.get("diffHuman");
        assertTrue(human.contains("时") && human.contains("分"));
    }

    @Test
    void testFileConverterFormat() throws Exception {
        FileConverterTool tool = new FileConverterTool();
        Map<String, Object> input = new HashMap<>();
        input.put("op", "format");
        input.put("text", "{\"name\":\"张三\",\"age\":25,\"city\":\"北京\"}");
        @SuppressWarnings("unchecked")
        Map<String, Object> r = (Map<String, Object>) tool.execute(null, input);
        String formatted = (String) r.get("output");
        assertTrue(formatted.contains("\n"));
        assertTrue(formatted.contains("\"name\""));
    }

    @Test
    void testFileConverterYaml2Json() throws Exception {
        FileConverterTool tool = new FileConverterTool();
        Map<String, Object> input = new HashMap<>();
        input.put("op", "yaml2json");
        input.put("text", "name: 张三\nage: 25\nactive: true");
        @SuppressWarnings("unchecked")
        Map<String, Object> r = (Map<String, Object>) tool.execute(null, input);
        String json = (String) r.get("output");
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("25"));
    }

    @Test
    void testFileConverterBase64() throws Exception {
        FileConverterTool tool = new FileConverterTool();
        Map<String, Object> input = new HashMap<>();
        input.put("op", "text2base64");
        input.put("text", "Hello World");
        @SuppressWarnings("unchecked")
        Map<String, Object> r = (Map<String, Object>) tool.execute(null, input);
        String b64 = (String) r.get("output");
        assertEquals("SGVsbG8gV29ybGQ=", b64);

        // 反向
        input.put("op", "base642text");
        input.put("text", b64);
        @SuppressWarnings("unchecked")
        Map<String, Object> r2 = (Map<String, Object>) tool.execute(null, input);
        assertEquals("Hello World", r2.get("output"));
    }

    @Test
    void testPredictionLinear() throws Exception {
        PredictionTool tool = new PredictionTool(Mockito.mock(DynamicDataSource.class));
        Map<String, Object> input = new HashMap<>();
        input.put("method", "linear");
        input.put("values", List.of(10.0, 20.0, 30.0, 40.0, 50.0));
        input.put("periods", 3);
        @SuppressWarnings("unchecked")
        Map<String, Object> r = (Map<String, Object>) tool.execute(null, input);
        System.out.println("DEBUG linear result: " + r);
        assertNotNull(r.get("forecast"));
        assertNotNull(r.get("rSquared"), "rSquared is null");
        // 完美线性: R²=1
        assertEquals(1.0, ((Number) r.get("rSquared")).doubleValue(), 0.01);
        // 斜率 10
        assertEquals(10.0, ((Number) r.get("slope")).doubleValue(), 0.1);
    }

    @Test
    void testPredictionLinearNoOp() {
        // placeholder
    }

    @Test
    void testPredictionMovingAverage() throws Exception {
        PredictionTool tool = new PredictionTool(Mockito.mock(DynamicDataSource.class));
        Map<String, Object> input = new HashMap<>();
        input.put("method", "ma3");
        input.put("values", List.of(10.0, 20.0, 30.0));
        input.put("periods", 2);
        @SuppressWarnings("unchecked")
        Map<String, Object> r = (Map<String, Object>) tool.execute(null, input);
        Object forecast = r.get("forecast");
        assertNotNull(forecast);
        // ma3 返回 List<Double>，size = periods
        assertTrue(forecast instanceof List);
        assertEquals(2, ((List<?>) forecast).size());
    }

    @Test
    void testCorrelationPearson() {
        CorrelationTool tool = new CorrelationTool(Mockito.mock(DynamicDataSource.class));
        // 完美正相关
        double[] x = {1, 2, 3, 4, 5};
        double[] y = {2, 4, 6, 8, 10};
        assertEquals(1.0, tool.pearson(x, y), 0.0001);
        // 完美负相关
        double[] z = {10, 8, 6, 4, 2};
        assertEquals(-1.0, tool.pearson(x, z), 0.0001);
    }

    @Test
    void testCorrelationSpearman() {
        CorrelationTool tool = new CorrelationTool(Mockito.mock(DynamicDataSource.class));
        double[] x = {1, 2, 3, 4, 5};
        double[] y = {1, 4, 9, 16, 25};  // 平方关系: Pearson 中等, Spearman 完美
        double p = tool.pearson(x, y);
        double s = tool.spearman(x, y);
        assertEquals(1.0, s, 0.01);
        assertTrue(p < s);
    }
}
