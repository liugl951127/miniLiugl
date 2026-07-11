package com.minimax.ai;

import com.minimax.ai.embedding.SimpleEmbedding;
import com.minimax.ai.generation.ConversationContext;
import com.minimax.ai.generation.IntentService;
import com.minimax.ai.generation.KeywordEngine;
import com.minimax.ai.generation.TypoTolerance;
import com.minimax.ai.model.MiniTransformer;
import com.minimax.ai.pipeline.PipelineExecutor;
import com.minimax.ai.pipeline.PipelineExecutor.PipelineRequest;
import com.minimax.ai.pipeline.PipelineExecutor.PipelineResult;
import com.minimax.ai.pipeline.config.PipelineConfig;
import com.minimax.ai.pipeline.config.PipelineConfig.ComputeMode;
import com.minimax.ai.pipeline.stage.*;
import com.minimax.ai.tool.AiToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * V2.8.5 Pipeline 端到端测试
 * 验证 13 阶段流水线的真实数据流转
 */
class V285PipelineTest {

    private PipelineExecutor executor;

    @BeforeEach
    void setup() {
        // 手动构造, 不依赖 Spring 容器
        Tokenizer tokenizer = new Tokenizer();
        MiniTransformer transformer = new MiniTransformer(8192, 128, 4, 2, 128);
        com.minimax.ai.tokenizer.ChineseTokenizer chineseTokenizer = new com.minimax.ai.tokenizer.ChineseTokenizer();
        SimpleEmbedding embedding = new SimpleEmbedding(chineseTokenizer, transformer);
        com.minimax.ai.multimodal.ImageAnalyzer imageAnalyzer = new com.minimax.ai.multimodal.ImageAnalyzer(embedding, Mockito.mock(com.minimax.ai.mapper.MultimediaFileMapper.class));
        com.minimax.ai.multimodal.AudioAnalyzer audioAnalyzer = new com.minimax.ai.multimodal.AudioAnalyzer();
        com.minimax.ai.document.DocumentParser documentParser = new com.minimax.ai.document.DocumentParser();
        MultimodalParser mmParser = new MultimodalParser(imageAnalyzer, audioAnalyzer, documentParser);
        ContextAssembler contextAssembler = new ContextAssembler(new ConversationContext());
        RiskControl risk = new RiskControl();
        ConversationContext ctx = new ConversationContext();
        KeywordEngine engine = new KeywordEngine(Mockito.mock(AiToolRegistry.class));
        AiToolRegistry registry = Mockito.mock(AiToolRegistry.class);
        RagToolAgentEnhancer rag = new RagToolAgentEnhancer(engine, registry, embedding, ctx);
        ModelInference inference = new ModelInference(transformer, tokenizer);
        FormatProcessor format = new FormatProcessor();
        LogStore logStore = new LogStore(null);  // log store 不可用, 不抛错
        IntentService intentService = Mockito.mock(IntentService.class);
        Mockito.when(intentService.recognize(Mockito.anyString())).thenReturn(KeywordEngine.Intent.CHAT);

        executor = new PipelineExecutor(
                new GatewayDispatcher(), mmParser, contextAssembler, risk,
                rag, tokenizer, inference, format, logStore, ctx, intentService);
    }

    @Test
    void testSimpleTextPipeline() {
        PipelineRequest req = new PipelineRequest();
        req.sessionId = "test-1";
        req.text = "你好, 帮我画个柱状图";
        req.clientIp = "127.0.0.1";

        PipelineResult r = executor.execute(req);
        assertNotNull(r);
        assertTrue(r.success, "Pipeline should succeed: " + r.errorMessage);
        assertNotNull(r.stageCosts, "stage costs should be recorded");
        // 13 个阶段都执行 (包括 RETURN)
        assertTrue(r.stageCosts.containsKey("USER_INPUT"));
        assertTrue(r.stageCosts.containsKey("GATEWAY_DISPATCH"));
        assertTrue(r.stageCosts.containsKey("MULTIMODAL_PARSE"));
        assertTrue(r.stageCosts.containsKey("CONTEXT_ASSEMBLE"));
        assertTrue(r.stageCosts.containsKey("PRE_RISK"));
        assertTrue(r.stageCosts.containsKey("RAG_TOOL_AGENT"));
        assertTrue(r.stageCosts.containsKey("TOKENIZE"));
        assertTrue(r.stageCosts.containsKey("MODEL_GENERATE"));
        assertTrue(r.stageCosts.containsKey("TOKEN_DECODE"));
        assertTrue(r.stageCosts.containsKey("POST_RISK"));
        assertTrue(r.stageCosts.containsKey("FORMAT"));
        assertTrue(r.stageCosts.containsKey("RETURN"));
        assertNotNull(r.finalText);
        assertTrue(r.finalText.length() > 0);
        assertTrue(r.totalCostMs > 0);
    }

    @Test
    void testBlockedByPreRisk() {
        PipelineRequest req = new PipelineRequest();
        req.sessionId = "test-blocked";
        req.text = "我想看色情视频";
        PipelineResult r = executor.execute(req);
        assertTrue(r.blocked, "Should be blocked");
        assertNotNull(r.blockReason);
        assertTrue(r.blockReason.contains("前置风控"));
    }

    @Test
    void testPrivacyDetected() {
        PipelineRequest req = new PipelineRequest();
        req.sessionId = "test-privacy";
        req.text = "我的手机号是 13800138000, 帮我存一下";
        PipelineResult r = executor.execute(req);
        // 隐私不应阻断 (只标记)
        assertNotNull(r.preRisk);
        assertTrue(r.preRisk.privacyHits.contains("MOBILE_CN"));
    }

    @Test
    void testComputeModeDefault() {
        // 默认应该是 AUTO
        assertEquals(ComputeMode.AUTO, PipelineConfig.getComputeMode());
        assertNotNull(PipelineConfig.resolveDevice());
    }

    @Test
    void testComputeModeSwitch() {
        // 切换到 CPU
        PipelineConfig.setComputeMode(ComputeMode.CPU);
        assertEquals("CPU", PipelineConfig.resolveDevice());
        // 切换到 GPU (但环境无 GPU, 应该报错或 fallback)
        try {
            PipelineConfig.setComputeMode(ComputeMode.GPU);
            String device = PipelineConfig.resolveDevice();
            // 如果没有 GPU, resolveDevice 会抛错, 我们要么拿到 GPU 设备, 要么被 catch
            assertTrue(device.startsWith("CPU") || device.startsWith("GPU"));
        } catch (IllegalStateException e) {
            // GPU 不可用
            assertTrue(e.getMessage().contains("GPU not available"));
        }
        // 恢复 AUTO
        PipelineConfig.setComputeMode(ComputeMode.AUTO);
    }

    @Test
    void testConfigSnapshot() {
        PipelineConfig.Data data = PipelineConfig.snapshot();
        assertNotNull(data);
        assertEquals(8192, data.vocabSize);
        assertEquals(128, data.hiddenDim);
        assertTrue(data.temperature > 0);
        assertTrue(data.maxGenerateTokens > 0);
    }

    @Test
    void testTokenizerRoundTrip() {
        Tokenizer t = new Tokenizer();
        String[] texts = {"你好世界", "Hello World", "画个柱状图", "MiniMax AI"};
        for (String text : texts) {
            int[] ids = t.encode(text);
            assertTrue(ids.length > 0);
            String decoded = t.decode(ids);
            assertNotNull(decoded);
            // BPE 后部分细节可能丢, 但应包含主要 token
        }
        // 估算
        assertEquals(4, t.estimate("你好世界"));
    }

    @Test
    void testRiskControlLevels() {
        RiskControl risk = new RiskControl();
        // Safe
        RiskControl.RiskResult safe = risk.preCheck("你好, 今天天气真好");
        assertEquals(PipelineConfig.RiskLevel.SAFE, safe.level);
        assertFalse(safe.blocked);
        // High (色情)
        RiskControl.RiskResult blocked = risk.preCheck("我想看色情");
        assertTrue(blocked.level.level >= PipelineConfig.RiskLevel.MEDIUM.level);
        // Privacy
        RiskControl.RiskResult priv = risk.preCheck("手机 13800138000");
        assertTrue(priv.privacyHits.size() > 0);
    }

    @Test
    void testFormatProcessor() {
        FormatProcessor f = new FormatProcessor();
        ModelInference.InferenceResult ir = new ModelInference.InferenceResult();
        ir.outputText = "你好世界  \n\n\n\n\n  多空行测试  \n  1. 第一项  \n  2. 第二项";
        FormatProcessor.FormatResult r = f.format(ir);
        assertNotNull(r);
        assertTrue(r.charCount > 0);
        assertFalse(r.formattedText.contains("\n\n\n"), "Multi blank lines should be merged");
        assertFalse(r.formattedText.contains("  \n"), "Trailing space should be trimmed");
    }
}
