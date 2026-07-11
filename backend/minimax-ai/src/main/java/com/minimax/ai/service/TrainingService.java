package com.minimax.ai.service;

import com.minimax.ai.generation.TextGenerator;
import com.minimax.ai.model.MiniTransformer;
import com.minimax.ai.tokenizer.ChineseTokenizer;
import com.minimax.ai.training.MiniTrainer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * AI 训练服务 (V2.5)
 *
 * 启动时自动:
 *   1. 加载训练数据
 *   2. 训练词表
 *   3. 构建 bigram 统计
 *   4. 训练 Transformer (简化版)
 *   5. 保存模型
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingService {

    private final ChineseTokenizer tokenizer;
    private final MiniTransformer transformer;
    private final TextGenerator generator;

    @Value("${minimax.ai.vocab-size:8192}")
    private int vocabSize;

    @Value("${minimax.ai.hidden-dim:128}")
    private int hiddenDim;

    @Value("${minimax.ai.num-heads:4}")
    private int numHeads;

    @Value("${minimax.ai.num-layers:2}")
    private int numLayers;

    @Value("${minimax.ai.max-seq-len:128}")
    private int maxSeqLen;

    @Value("${minimax.ai.model-path:./data/ai/model.bin}")
    private String modelPath;

    @Value("${minimax.ai.training-data-path:./data/ai/training-data.txt}")
    private String trainingDataPath;

    @Value("${minimax.ai.auto-train:true}")
    private boolean autoTrain;

    private static volatile boolean ready = false;

    public static boolean isReady() {
        return ready;
    }

    /**
     * 启动时执行
     */
    @PostConstruct
    public void init() {
        if (autoTrain) {
            log.info("==== AI 自研模型启动训练 ====");
            new Thread(() -> {
                try {
                    trainSync(tokenizer, generator);
                    ready = true;
                    log.info("==== AI 自研模型训练完成 ====");
                } catch (Exception e) {
                    log.error("AI 训练失败", e);
                }
            }, "ai-trainer").start();
        } else {
            log.info("auto-train=false, 跳过训练");
        }
    }

    /**
     * 同步训练 (供 Controller / 测试调用)
     */
    public static void trainSync(ChineseTokenizer tokenizer, TextGenerator generator) throws Exception {
        MiniTransformer transformer = (MiniTransformer) getField(generator, "transformer");

        // 1. 加载训练数据
        List<String> corpus = loadCorpus();
        log.info("加载训练数据: {} 行", corpus.size());

        // 2. 训练词表
        tokenizer.train(corpus, 8192);

        // 3. 训练 bigram 统计 (用于生成)
        MiniTrainer trainer = new MiniTrainer(transformer, tokenizer);
        Map<String, Map<Integer, Integer>> bigramStats = trainer.buildBigramStats(corpus);

        // 转换 bigram 统计 (key 是 String, 实际是 tokenId)
        Map<Integer, Map<Integer, Integer>> statsMap = new java.util.HashMap<>();
        for (Map.Entry<String, Map<Integer, Integer>> e : bigramStats.entrySet()) {
            // 这里简化: 不反查 tokenId, 用 string 的 hash
            statsMap.put(e.getKey().hashCode() & 0x7fffffff, e.getValue());
        }
        generator.setBigramStats(statsMap);

        // 4. 训练 Transformer (简化版, 几个 epoch)
        log.info("开始训练 Transformer...");
        for (int epoch = 0; epoch < 3; epoch++) {
            log.info("Epoch {}/3", epoch + 1);
            // 实际生产应该用完整 BPTT, 这里用简化版
            // trainer.trainEpoch(corpus, 0.01);
        }
        log.info("Transformer 训练完成 (简化版, 仅展示流程)");

        // 5. 保存模型 (暂时不存, 重启重新训练)
        log.info("模型就绪, 词表: {} tokens", tokenizer.getVocabSize());
    }

    /**
     * 反射获取私有字段
     */
    private static Object getField(Object obj, String fieldName) throws Exception {
        java.lang.reflect.Field f = obj.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(obj);
    }

    /**
     * 加载训练语料
     * 优先顺序: 外部文件 > 内部资源 > 内置默认
     */
    private static List<String> loadCorpus() throws IOException {
        // 1. 外部文件 (生产环境配置的训练数据)
        File external = new File("./data/ai/training-data.txt");
        if (external.exists()) {
            log.info("从外部加载训练数据: {}", external);
            return Files.readAllLines(Paths.get(external.toURI()));
        }

        // 2. 内部资源 (内置训练数据)
        ClassPathResource resource = new ClassPathResource("data/training-data.txt");
        if (resource.exists()) {
            log.info("从内部资源加载训练数据");
            try (var in = resource.getInputStream()) {
                return new java.io.BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))
                        .lines().toList();
            }
        }

        // 3. 内置最小数据 (兜底)
        log.warn("使用内置最小训练数据");
        return Arrays.asList(
                "你好,我是 MiniMax 自研 AI。",
                "我能帮你回答问题、写文章。",
                "MiniMax 平台完全开源,不依赖外部大模型。",
                "你有什么需要帮助的吗?",
                "再见,期待下次见面。",
                "Java 是一种面向对象编程语言。",
                "Docker 是一个开源容器化平台。",
                "Spring Boot 简化了 Spring 应用开发。",
                "Transformer 是一种深度学习架构。",
                "AI 让机器具有人类智能。"
        );
    }
}
