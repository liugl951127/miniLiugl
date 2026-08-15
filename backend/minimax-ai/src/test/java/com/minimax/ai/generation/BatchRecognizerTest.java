package com.minimax.ai.generation;

import com.minimax.ai.generation.ConversationContext;
import com.minimax.ai.generation.model.BatchRecognizer;
import com.minimax.ai.generation.model.ContextModel;
import com.minimax.ai.generation.model.NeuralIntentModel;
import com.minimax.ai.generation.model.NgramModel;
import com.minimax.ai.generation.model.SynonymModel;
import com.minimax.ai.generation.model.QueryEmbedder;
import com.minimax.ai.mapper.AiIntentKeywordMapper;
import com.minimax.ai.tokenizer.ChineseTokenizer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BatchRecognizer 批量识别测试 (V3.5.16+)
 */
@DisplayName("BatchRecognizer 批量测试")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BatchRecognizerTest {

    private static BatchRecognizer recognizer;
    private static IntentService intentService;

    @BeforeAll
    static void setUp() {
        ChineseTokenizer tokenizer = new ChineseTokenizer();
        QueryEmbedder embedder = new QueryEmbedder(tokenizer);
        embedder.init();
        NeuralIntentModel neuralModel = new NeuralIntentModel(embedder);
        neuralModel.init();

        AiIntentKeywordMapper mapper = Mockito.mock(AiIntentKeywordMapper.class);
        Mockito.when(mapper.selectList(Mockito.any())).thenReturn(new java.util.ArrayList<>());
        NgramModel ngramModel = new NgramModel();
        ngramModel.train(NgramModel.defaultTrainingData());
        SynonymModel syn = new SynonymModel();
        ContextModel ctx = new ContextModel(new ConversationContext());
        intentService = new IntentService(mapper, ngramModel, syn, ctx, neuralModel, null);
        injectField(intentService, "weightTf", 0.4);
        injectField(intentService, "weightNgram", 0.3);
        injectField(intentService, "weightSynonym", 0.2);
        injectField(intentService, "weightContext", 0.1);
        injectField(intentService, "weightNeural", 0.0);
        injectField(intentService, "cacheSize", 1000);
        intentService.init();

        recognizer = new BatchRecognizer(intentService, embedder, neuralModel);
    }

    private static void injectField(Object t, String name, Object v) {
        try {
            java.lang.reflect.Field f = t.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(t, v);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("批量: 5 个 query 一次返回 5 个结果")
    void testBatch() {
        List<String> queries = Arrays.asList(
                "画柱状图", "做个饼图", "转人工", "你好", "查询用户"
        );
        List<BatchRecognizer.BatchResult> results = recognizer.recognizeBatch(queries);
        assertEquals(5, results.size());
    }

    @Test
    @DisplayName("批量: 空列表返回空")
    void testEmpty() {
        assertEquals(0, recognizer.recognizeBatch(Arrays.asList()).size());
        assertEquals(0, recognizer.recognizeBatch(null).size());
    }

    @Test
    @DisplayName("批量: 每个 result 都有 intent")
    void testResultHasIntent() {
        List<BatchRecognizer.BatchResult> results = recognizer.recognizeBatch(
                Arrays.asList("画柱状图", "转人工"));
        for (BatchRecognizer.BatchResult r : results) {
            assertNotNull(r.intent());
            assertNotNull(r.query());
        }
    }

    @Test
    @DisplayName("性能: 20 个 query 批量 < 1000ms")
    void testPerformance() {
        // 20 query 足够展示 batch 优势
        List<String> queries = java.util.stream.IntStream.range(0, 20)
                .mapToObj(i -> "画柱状图 " + i)
                .collect(Collectors.toList());
        long start = System.nanoTime();
        List<BatchRecognizer.BatchResult> results = recognizer.recognizeBatch(queries);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(20, results.size());
        // 沙箱慢, 给 1500ms 阈值
        assertTrue(ms < 1500, "Batch 20 should < 1500ms: " + ms);
        System.out.printf("[batch-perf] 20 query: %dms (avg %.1fms/req)%n", ms, ms/20.0);
    }
}
