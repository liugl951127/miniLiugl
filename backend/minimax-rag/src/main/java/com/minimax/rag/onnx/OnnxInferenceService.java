package com.minimax.rag.onnx;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ONNX Runtime 推理服务 (RAG 模块, V7.0)
 *
 * 功能:
 *  - 加载本地 .onnx 模型进行 embedding 推理
 *  - 自动下载模型 (首次启动, 需配置 model-url)
 *  - 无模型时降级到 MockEmbeddingClient
 *
 * 配置:
 *  - minimax.rag.onnx.enabled=true/false (默认 false)
 *  - minimax.rag.onnx.model-path=/path/to/model.onnx
 *  - minimax.rag.onnx.model-url=https://...
 *  - minimax.rag.onnx.vocab-path=/path/to/vocab.txt
 *  - minimax.rag.onnx.pooling=mean|cls|max (默认 mean)
 *  - minimax.rag.onnx.dim=embedding维度 (默认 512)
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "minimax.rag.onnx.enabled", havingValue = "true", matchIfMissing = false)
public class OnnxInferenceService {

    @Value("${minimax.rag.onnx.model-path:#{null}}")
    private String modelPath;

    @Value("${minimax.rag.onnx.model-url:#{null}}")
    private String modelUrl;

    @Value("${minimax.rag.onnx.vocab-path:#{null}}")
    private String vocabPath;

    @Value("${minimax.rag.onnx.pooling:mean}")
    private String pooling;

    @Value("${minimax.rag.onnx.dim:512}")
    private int embeddingDim;

    @Value("${minimax.rag.onnx.max-length:256}")
    private int maxLength;

    private OrtEnvironment env;
    private OrtSession session;
    private volatile boolean initialized = false;

    /** vocab: token → id */
    private final Map<String, Integer> vocab = new ConcurrentHashMap<>();

    /** reverse vocab: id → token */
    private final Map<Integer, String> idToToken = new ConcurrentHashMap<>();

    public boolean isInitialized() { return initialized; }
    public int getEmbeddingDim() { return embeddingDim; }

    @PostConstruct
    public void init() {
        try {
            env = OrtEnvironment.getEnvironment();
            loadModel();
            loadVocab();
            initialized = true;
            log.info("[OnnxInference] 初始化成功! pooling={}, dim={}", pooling, embeddingDim);
        } catch (Throwable e) {
            log.warn("[OnnxInference] 初始化失败，将使用 MockEmbeddingClient: {}", e.getMessage());
            initialized = false;
        }
    }

    private void loadModel() throws Exception {
        Path modelFile = resolveModelPath();
        if (modelFile == null || !Files.exists(modelFile)) {
            if (modelUrl != null && !modelUrl.isBlank()) {
                modelFile = downloadModel(modelUrl);
            }
        }
        if (modelFile == null || !Files.exists(modelFile)) {
            throw new FileNotFoundException(
                "ONNX 模型不存在! model-path=" + modelPath + ", model-url=" + modelUrl +
                "\n请: (1) 下载模型到本地并设置 model-path; 或 (2) 设置 model-url 自动下载" +
                "\n推荐模型: bge-small-zh-v1.5 (512维, ~130MB): https://huggingface.co/BBrum/ggml_bge-small-zh-v1.5/resolve/main/ggml-model.f16.onnx"
            );
        }
        session = env.createSession(modelFile.toString());
        log.info("[OnnxInference] 模型加载: {}", modelFile);
        log.info("[OnnxInference] 输入: {}, 输出: {}", session.getInputNames(), session.getOutputNames());
    }

    private Path resolveModelPath() {
        if (modelPath == null || modelPath.isBlank()) return null;
        Path p = Paths.get(modelPath);
        if (p.isAbsolute()) return p;
        return Paths.get(System.getProperty("user.dir"), modelPath);
    }

    private Path downloadModel(String urlStr) throws Exception {
        Path cacheDir = Paths.get(System.getProperty("java.io.tmpdir"), "onnx-models");
        Files.createDirectories(cacheDir);
        Path dest = cacheDir.resolve("rag-embedding.onnx");
        if (Files.exists(dest) && Files.size(dest) > 1024) {
            log.info("[OnnxInference] 使用缓存模型: {}", dest);
            return dest;
        }
        log.info("[OnnxInference] 下载 ONNX 模型: {}", urlStr);
        URL url = new URL(urlStr);
        // T2: HttpURLConnection 不可 AutoCloseable, 用 try-with-resources 包内层流, finally disconnect
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             OutputStream out = Files.newOutputStream(dest, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(300_000);
            long total = conn.getContentLengthLong();
            byte[] buf = new byte[32768];
            long downloaded = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                downloaded += n;
            }
        } finally {
            conn.disconnect();
        }
        log.info("[OnnxInference] 下载完成: {} ({} MB)", dest, Files.size(dest) / 1024 / 1024);
        return dest;
    }

    private void loadVocab() {
        if (vocabPath == null || vocabPath.isBlank()) {
            // 无 vocab，构建字符级 vocab
            buildCharVocab();
            return;
        }
        try {
            Path vp = Paths.get(vocabPath);
            if (!Files.exists(vp)) {
                buildCharVocab();
                return;
            }
            int id = 0;
            for (String line : Files.readAllLines(vp)) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\t");
                String token = parts[0];
                id = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : id;
                vocab.put(token, id);
                idToToken.put(id, token);
                id++;
            }
            // 确保特殊 token
            vocab.putIfAbsent("[PAD]", 0);
            vocab.putIfAbsent("[UNK]", 1);
            vocab.putIfAbsent("[CLS]", 2);
            vocab.putIfAbsent("[SEP]", 3);
            log.info("[OnnxInference] Vocab 加载: {} tokens", vocab.size());
        } catch (Exception e) {
            log.warn("[OnnxInference] Vocab 加载失败，使用字符级: {}", e.getMessage());
            buildCharVocab();
        }
    }

    private void buildCharVocab() {
        vocab.put("[PAD]", 0);
        vocab.put("[UNK]", 1);
        vocab.put("[CLS]", 2);
        vocab.put("[SEP]", 3);
        // 常用中文字符
        String common = "的一是在不了有和人这中大为上个国我以要他时来用们生到作地于出就分对成会可主发年动同工也能下过子说产种面而方后多定行学法所民得经十三之进着等部度家电力里如水高量二间因X差果信思事批量代内日X接请结第平系立林ommel风趣步三集传极号伤K技际近电业品回P务M农X名X万资六朝验吃画龙船密包麻P喜T奇绝锦纪BSCLERDAFWN郭THUAKSMLRVOWEIJQZP言X马首ELGOMDWYBPNCZXFKHQJGV连停岛港X女X适税优律测市均钱J季候导谷七K板创残局巡乃鲁返港服衣霞挖闭枚横琴键锋E帐X帖J卫液柴铲L页链锤撤饱辅仪辈D曲D";
        int id = 10;
        for (char c : common.toCharArray()) {
            String ch = String.valueOf(c);
            vocab.putIfAbsent(ch, id++);
        }
        vocab.forEach((token, tid) -> idToToken.put(tid, token));
        log.info("[OnnxInference] 字符级 Vocab 构建: {} tokens", vocab.size());
    }

    /**
     * 分词: 文本 → token ID 序列
     */
    public long[] tokenize(String text) {
        if (text == null || text.isBlank()) return new long[0];
        List<Long> ids = new ArrayList<>();
        ids.add((long) vocab.getOrDefault("[CLS]", 2));

        String cleaned = text.toLowerCase().trim();
        // 尝试词组匹配
        boolean matched = false;
        for (int len = Math.min(cleaned.length(), 8); len >= 2; len--) {
            for (int i = 0; i + len <= cleaned.length(); i++) {
                String sub = cleaned.substring(i, i + len);
                if (vocab.containsKey(sub)) {
                    ids.add((long) vocab.get(sub));
                    i += len - 1;
                    matched = true;
                    break;
                }
            }
            if (matched) break;
        }
        if (!matched) {
            // 字符级 fallback
            for (char c : cleaned.toCharArray()) {
                String ch = String.valueOf(c);
                ids.add((long) vocab.getOrDefault(ch, vocab.getOrDefault("[UNK]", 1)));
            }
        }
        ids.add((long) vocab.getOrDefault("[SEP]", 3));

        long[] result = new long[Math.min(ids.size(), maxLength)];
        for (int i = 0; i < result.length; i++) result[i] = ids.get(i);
        return result;
    }

    public long[] attentionMask(long[] inputIds) {
        long[] mask = new long[inputIds.length];
        Arrays.fill(mask, 1L);
        return mask;
    }

    /**
     * 运行 ONNX 推理, 返回 pooling 后的 embedding 向量
     */
    public float[] infer(String text) {
        if (!initialized || session == null) return null;
        long[] inputIds = tokenize(text);
        long[] mask = attentionMask(inputIds);
        return inferRaw(inputIds, mask);
    }

    public float[] inferRaw(long[] inputIds, long[] attentionMask) {
        if (!initialized || session == null) return null;
        try {
            String inputName = session.getInputNames().iterator().next();
            String outputName = session.getOutputNames().iterator().next();

            try (OnnxTensor idsTensor = OnnxTensor.createTensor(env,
                    java.nio.LongBuffer.wrap(inputIds), new long[]{1, inputIds.length});
                 OnnxTensor maskTensor = OnnxTensor.createTensor(env,
                    java.nio.LongBuffer.wrap(attentionMask), new long[]{1, attentionMask.length})) {

                // 尝试两种输入命名约定
                Map<String, OnnxTensor> inputs;
                String maskName = inputName.replace("input_ids", "attention_mask")
                        .replace("input_ids_list", "attention_mask_list");
                if (session.getInputNames().contains(maskName)) {
                    inputs = Map.of(inputName, idsTensor, maskName, maskTensor);
                } else {
                    inputs = Map.of(inputName, idsTensor);
                }

                try (OrtSession.Result result = session.run(inputs)) {
                    float[][] output = (float[][]) result.get(outputName).orElseThrow().getValue();
                    return applyPooling(output[0], inputIds.length);
                }
            }
        } catch (Throwable e) {
            log.warn("[OnnxInference] 推理失败: {}", e.getMessage());
            return null;
        }
    }

    private float[] applyPooling(float[] tokenEmbeddings, int seqLen) {
        if (seqLen <= 0 || tokenEmbeddings.length == 0) return new float[embeddingDim];
        int hidden = tokenEmbeddings.length / seqLen;
        if (hidden <= 0) hidden = embeddingDim;

        switch (pooling) {
            case "cls":
                // CLS token is first
                return normalize(Arrays.copyOf(tokenEmbeddings, Math.min(hidden, tokenEmbeddings.length)));
            case "max":
                float[] maxVec = new float[hidden];
                Arrays.fill(maxVec, Float.NEGATIVE_INFINITY);
                for (int i = 0; i < seqLen; i++) {
                    for (int j = 0; j < hidden; j++) {
                        int idx = i * hidden + j;
                        if (idx < tokenEmbeddings.length) {
                            maxVec[j] = Math.max(maxVec[j], tokenEmbeddings[idx]);
                        }
                    }
                }
                return normalize(maxVec);
            default: // mean
                float[] pooled = new float[hidden];
                for (int i = 0; i < seqLen; i++) {
                    for (int j = 0; j < hidden; j++) {
                        int idx = i * hidden + j;
                        if (idx < tokenEmbeddings.length) pooled[j] += tokenEmbeddings[idx];
                    }
                }
                for (int j = 0; j < hidden; j++) pooled[j] /= seqLen;
                return normalize(pooled);
        }
    }

    private float[] normalize(float[] v) {
        double norm = 0;
        for (float f : v) norm += f * f;
        norm = Math.sqrt(norm);
        if (norm < 1e-10) return v;
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = (float) (v[i] / norm);
        return out;
    }

    @PreDestroy
    public void close() {
        try {
            if (session != null) session.close();
            if (env != null) env.close();
        } catch (Exception ignored) {}
    }
}
