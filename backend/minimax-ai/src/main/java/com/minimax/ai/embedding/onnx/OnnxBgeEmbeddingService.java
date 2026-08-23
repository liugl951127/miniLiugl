package com.minimax.ai.embedding.onnx;

import ai.onnxruntime.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minimax.ai.multimodal.onnx.SimpleBpeTokenizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.*;

/**
 * ONNX BGE 中文 Embedding 服务 (V7.4)
 *
 * <p>替换项目内 {@code SimpleEmbedding} (TF-IDF 风格, 384维), 升级为
 * <a href="https://huggingface.co/BAAI/bge-small-zh-v1.5">BAAI/bge-small-zh-v1.5</a>
 * 真实 sentence-transformer, 512 维, 中文检索 SOTA.</p>
 *
 * <h3>模型规格</h3>
 * <ul>
 *   <li>输入 input_ids: int64[batch, seq] (BERT tokenizer, max 512)</li>
 *   <li>输入 attention_mask: int64[batch, seq]</li>
 *   <li>输入 token_type_ids: int64[batch, seq] (全 0)</li>
 *   <li>输出 last_hidden_state: float32[batch, seq, 512]</li>
 *   <li>Pool: [CLS] 向量 (BGE 官方做法), L2 归一化</li>
 * </ul>
 *
 * <h3>降级</h3>
 * <p>模型未加载 → 返回零向量 + 标记 ready=false, 上层 RAG 链路自动用 SimpleEmbedding 兜底.</p>
 */
@Slf4j
@Service
public class OnnxBgeEmbeddingService {

    @Value("${minimax.onnx-vision.bge-path:./data/models/bge-small-zh-v15/model.onnx}")
    private String modelPath;

    @Value("${minimax.onnx-vision.bge-vocab:./data/models/bge-small-zh-v15/vocab.txt}")
    private String vocabPath;

    @Value("${minimax.onnx-vision.bge-max-len:512}")
    private int maxLen;

    private OrtEnvironment env;
    private OrtSession session;
    private BertTokenizer tokenizer;

    public boolean isReady() { return session != null && tokenizer != null; }
    public String getModelPath() { return modelPath; }
    public int getEmbeddingDim() { return 512; }

    @PostConstruct
    public void init() {
        // 1. Tokenizer
        File vf = new File(vocabPath);
        if (vf.exists()) {
            tokenizer = new BertTokenizer();
            if (!tokenizer.loadFromVocab(vocabPath)) {
                log.warn("[OnnxBge] vocab 加载失败");
                tokenizer = null;
            }
        } else {
            log.warn("[OnnxBge] vocab.txt 不存在: {}", vocabPath);
        }
        // 2. Model
        File mf = new File(modelPath);
        if (!mf.exists()) {
            log.warn("[OnnxBge] 模型不存在: {} — Embedding 升级不可用", modelPath);
            return;
        }
        try {
            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setIntraOpNumThreads(4);
            session = env.createSession(modelPath, opts);
            log.info("[OnnxBge] ✅ 加载成功: {} (max_len={})", modelPath, maxLen);
        } catch (Exception e) {
            log.error("[OnnxBge] 加载失败: {}", e.getMessage());
            session = null;
        }
    }

    @PreDestroy
    public void destroy() {
        try { if (session != null) session.close(); } catch (Exception ignored) {}
        try { if (env != null) env.close(); } catch (Exception ignored) {}
    }

    /**
     * 计算文本 embedding
     */
    public float[] encode(String text) {
        return encodeBatch(new String[]{text})[0];
    }

    /**
     * 批量 embedding
     *
     * @return 二维数组 [N][512], L2 归一化
     */
    public float[][] encodeBatch(String[] texts) {
        if (!isReady() || texts == null || texts.length == 0) {
            return new float[0][];
        }
        try {
            int n = texts.length;
            int seqLen = Math.min(maxLen, 512);
            long[][] inputIds = new long[n][seqLen];
            long[][] attentionMask = new long[n][seqLen];
            long[][] tokenTypeIds = new long[n][seqLen];

            for (int i = 0; i < n; i++) {
                long[] tokens = tokenizer.tokenize(texts[i] == null ? "" : texts[i], seqLen);
                System.arraycopy(tokens, 0, inputIds[i], 0, seqLen);
                for (int j = 0; j < seqLen; j++) {
                    attentionMask[i][j] = tokens[j] == 0 ? 0 : 1;
                    tokenTypeIds[i][j] = 0;
                }
            }

            // 展平
            long[] flatIds = new long[n * seqLen];
            long[] flatMask = new long[n * seqLen];
            long[] flatType = new long[n * seqLen];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < seqLen; j++) {
                    flatIds[i * seqLen + j] = inputIds[i][j];
                    flatMask[i * seqLen + j] = attentionMask[i][j];
                    flatType[i * seqLen + j] = tokenTypeIds[i][j];
                }
            }
            long[] shape = {n, seqLen};
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", OnnxTensor.createTensor(env, LongBuffer.wrap(flatIds), shape));
            inputs.put("attention_mask", OnnxTensor.createTensor(env, LongBuffer.wrap(flatMask), shape));
            inputs.put("token_type_ids", OnnxTensor.createTensor(env, LongBuffer.wrap(flatType), shape));

            try (OrtSession.Result result = session.run(inputs)) {
                OnnxTensor out = (OnnxTensor) result.get(0);
                float[][][] hidden = (float[][][]) out.getValue();
                // CLS pooling (取 [CLS] = position 0)
                float[][] embs = new float[n][512];
                for (int i = 0; i < n; i++) {
                    System.arraycopy(hidden[i][0], 0, embs[i], 0, 512);
                    embs[i] = l2Normalize(embs[i]);
                }
                return embs;
            }
        } catch (Exception e) {
            log.error("[OnnxBge] 推理失败: {}", e.getMessage());
            return new float[0][];
        }
    }

    private float[] l2Normalize(float[] v) {
        double sum = 0;
        for (float f : v) sum += f * f;
        if (sum == 0) return v;
        sum = Math.sqrt(sum);
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = (float) (v[i] / sum);
        return out;
    }

    /**
     * 内置 BERT WordPiece Tokenizer (V7.4)
     *
     * <p>从 vocab.txt 加载词表 (BGE 格式, 一行一个 token).</p>
     */
    public static class BertTokenizer {
        private final Map<String, Integer> vocab = new LinkedHashMap<>();
        private static final String[] NEVER_SPLIT = {
            "[UNK]", "[SEP]", "[PAD]", "[CLS]", "[MASK]"
        };

        public boolean loadFromVocab(String path) {
            try {
                List<String> lines = java.nio.file.Files.readAllLines(new File(path).toPath());
                int idx = 0;
                for (String line : lines) {
                    String tok = line.trim();
                    if (!tok.isEmpty()) vocab.put(tok, idx++);
                }
                log.info("[BertTokenizer] vocab {} tokens from {}", vocab.size(), path);
                return !vocab.isEmpty();
            } catch (Exception e) {
                log.error("[BertTokenizer] 加载失败: {}", e.getMessage());
                return false;
            }
        }

        public long[] tokenize(String text, int maxLen) {
            long[] out = new long[maxLen];
            // [CLS]
            out[0] = lookup("[CLS]");
            int pos = 1;

            // 简化 Chinese tokenize: char-level (vocab 包含单字)
            // 英文: BasicTokenizer 拆分 (空格分 + 标点)
            String[] tokens = basicTokenize(text);
            for (String tok : tokens) {
                if (pos >= maxLen - 1) break;
                Integer id = vocab.get(tok);
                if (id != null) {
                    out[pos++] = id;
                } else {
                    // WordPiece: 找最长匹配 + ## 后缀
                    int start = 0;
                    while (start < tok.length() && pos < maxLen - 1) {
                        int end = tok.length();
                        String cur = null;
                        while (end > start) {
                            String sub = start == 0 ? tok.substring(start, end)
                                                    : "##" + tok.substring(start, end);
                            if (vocab.containsKey(sub)) {
                                cur = sub; break;
                            }
                            end--;
                        }
                        if (cur == null) {
                            out[pos++] = lookup("[UNK]");
                            break;
                        }
                        out[pos++] = vocab.get(cur);
                        start = end;
                    }
                }
            }
            // [SEP]
            if (pos < maxLen) out[pos++] = lookup("[SEP]");
            // pad 0
            for (; pos < maxLen; pos++) out[pos] = 0;
            return out;
        }

        private String[] basicTokenize(String text) {
            if (text == null) return new String[0];
            // 清除多余空白
            text = text.replaceAll("\\s+", " ").trim();
            if (text.isEmpty()) return new String[0];
            // 按空格 + 标点切分
            List<String> out = new ArrayList<>();
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (Character.isWhitespace(c)) {
                    if (buf.length() > 0) { out.add(buf.toString()); buf.setLength(0); }
                } else if (isPunctuation(c) || isCjk(c)) {
                    if (buf.length() > 0) { out.add(buf.toString()); buf.setLength(0); }
                    out.add(String.valueOf(c));
                } else {
                    buf.append(c);
                }
            }
            if (buf.length() > 0) out.add(buf.toString());
            return out.toArray(new String[0]);
        }

        private boolean isPunctuation(char c) {
            return !Character.isLetterOrDigit(c) && !Character.isWhitespace(c);
        }

        private boolean isCjk(char c) {
            return (c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3400 && c <= 0x4DBF);
        }

        private int lookup(String tok) {
            Integer id = vocab.get(tok);
            return id == null ? 0 : id;
        }

        public int vocabSize() { return vocab.size(); }
    }
}
