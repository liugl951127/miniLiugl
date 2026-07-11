package com.minimax.ai.controller;

import com.minimax.ai.dto.EmbedRequest;
import com.minimax.ai.dto.GenerateRequest;
import com.minimax.ai.embedding.SimpleEmbedding;
import com.minimax.ai.generation.TextGenerator;
import com.minimax.ai.tokenizer.ChineseTokenizer;
import com.minimax.ai.vo.GenerateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

/**
 * MiniMax 自研 AI 接口 (V2.5)
 *
 * 不依赖任何外部大模型, 完全自研
 *
 * 接口列表:
 *   POST /api/ai/generate         文本生成 (非流式)
 *   POST /api/ai/generate/stream  文本生成 (SSE 流式)
 *   POST /api/ai/embed            文本向量化
 *   POST /api/ai/similarity       文本相似度
 *   POST /api/ai/tokenize         分词
 *   GET  /api/ai/info             AI 服务信息
 *   GET  /api/ai/health           健康检查
 *   POST /api/ai/train            触发训练
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "自研 AI", description = "MiniMax 自研 AI (Transformer 简化版, 不依赖外部大模型)")
public class AiController {

    private final TextGenerator generator;
    private final SimpleEmbedding embedding;
    private final ChineseTokenizer tokenizer;

    @Value("${minimax.ai.vocab-size:8192}")
    private int vocabSize;

    @Value("${minimax.ai.hidden-dim:128}")
    private int hiddenDim;

    /**
     * 文本生成 (非流式)
     */
    @PostMapping("/generate")
    @Operation(summary = "文本生成 (自研模型)")
    public GenerateResponse generate(@RequestBody GenerateRequest request) {
        long start = System.currentTimeMillis();
        String prompt = request.getPrompt() == null ? "" : request.getPrompt();
        int maxLen = request.getMaxLength() == null ? 50 : Math.min(request.getMaxLength(), 200);
        double temp = request.getTemperature() == null ? 0.8 :
                Math.max(0.1, Math.min(request.getTemperature(), 2.0));

        log.info("生成请求: prompt='{}' maxLen={} temp={}", prompt, maxLen, temp);

        String text = generator.generate(prompt, maxLen, temp);
        long duration = System.currentTimeMillis() - start;

        // token 数估算
        int tokens = tokenizer.encode(text).length;

        return new GenerateResponse(
                prompt, text, tokens, duration,
                "MiniMax-Transformer-Small", true
        );
    }

    /**
     * 文本生成 (SSE 流式)
     */
    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "文本生成 (SSE 流式)")
    public SseEmitter generateStream(@RequestBody GenerateRequest request) {
        SseEmitter emitter = new SseEmitter(60_000L);
        String prompt = request.getPrompt() == null ? "" : request.getPrompt();
        int maxLen = request.getMaxLength() == null ? 50 : Math.min(request.getMaxLength(), 200);
        double temp = request.getTemperature() == null ? 0.8 : request.getTemperature();

        // 异步流式生成
        new Thread(() -> {
            try {
                // 简化: 每 200ms 推一个 token
                String result = generator.generate(prompt, maxLen, temp);
                int[] tokens = tokenizer.encode(result);
                for (int i = 0; i < tokens.length; i++) {
                    String piece = tokenizer.decode(new int[]{tokens[i]});
                    emitter.send(SseEmitter.event()
                            .id(String.valueOf(i))
                            .data(piece)
                            .name("token"));
                    Thread.sleep(50);
                }
                emitter.send(SseEmitter.event()
                        .data("[DONE]")
                        .name("end"));
                emitter.complete();
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    /**
     * 文本向量化
     */
    @PostMapping("/embed")
    @Operation(summary = "文本向量化 (用于 RAG / 相似度)")
    public Map<String, Object> embed(@RequestBody EmbedRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("dim", embedding.getDimension());

        if (request.getText() != null) {
            double[] vec = embedding.embed(request.getText());
            result.put("vector", vec);
            result.put("count", 1);
        } else if (request.getTexts() != null) {
            List<double[]> vecs = new ArrayList<>();
            for (String t : request.getTexts()) {
                vecs.add(embedding.embed(t));
            }
            result.put("vectors", vecs);
            result.put("count", vecs.size());
        } else {
            result.put("error", "text or texts required");
        }
        return result;
    }

    /**
     * 文本相似度
     */
    @PostMapping("/similarity")
    @Operation(summary = "两段文本相似度")
    public Map<String, Object> similarity(@RequestBody Map<String, String> request) {
        String t1 = request.get("text1");
        String t2 = request.get("text2");
        double score = embedding.similarity(t1, t2);
        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("text1", t1);
        result.put("text2", t2);
        result.put("method", "cosine-similarity");
        return result;
    }

    /**
     * 分词
     */
    @PostMapping("/tokenize")
    @Operation(summary = "中文分词")
    public Map<String, Object> tokenize(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        List<String> tokens = tokenizer.preTokenize(text);
        int[] ids = tokenizer.encode(text);

        Map<String, Object> result = new HashMap<>();
        result.put("text", text);
        result.put("tokens", tokens);
        result.put("ids", ids);
        result.put("vocabSize", tokenizer.getVocabSize());
        return result;
    }

    /**
     * AI 服务信息
     */
    @GetMapping("/info")
    @Operation(summary = "AI 服务信息")
    public Map<String, Object> info() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", "MiniMax 自研 AI");
        result.put("version", "V2.5");
        result.put("type", "Transformer-Small");
        result.put("vocabSize", vocabSize);
        result.put("hiddenDim", hiddenDim);
        result.put("architecture", "Embedding + N×[Self-Attention + FFN] + LM-Head");
        result.put("parameters", "1-2M (CPU 友好)");
        result.put("selfDeveloped", true);
        result.put("dependencies", "无 (纯 Java 实现)");
        result.put("endpoints", Arrays.asList(
                "/api/ai/generate",
                "/api/ai/generate/stream",
                "/api/ai/embed",
                "/api/ai/similarity",
                "/api/ai/tokenize",
                "/api/ai/info",
                "/api/ai/health",
                "/api/ai/train"
        ));
        return result;
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        result.put("vocabReady", tokenizer.getVocabSize() > 5);
        result.put("embeddingDim", embedding.getDimension());
        return result;
    }

    /**
     * 触发训练
     */
    @PostMapping("/train")
    @Operation(summary = "触发模型训练")
    public Map<String, Object> train() {
        // 训练逻辑放后台
        new Thread(() -> {
            try {
                com.minimax.ai.service.TrainingService.trainSync(tokenizer, generator);
            } catch (Exception e) {
                log.error("训练失败", e);
            }
        }).start();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "training started");
        result.put("background", true);
        return result;
    }
}
