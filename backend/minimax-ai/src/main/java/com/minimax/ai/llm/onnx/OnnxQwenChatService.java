package com.minimax.ai.llm.onnx;

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
 * ONNX Qwen2.5 对话服务 (V7.4)
 *
 * <p>基于 <a href="https://huggingface.co/onnx-community/Qwen2.5-0.5B-Instruct">Qwen2.5-0.5B-Instruct</a>
 * 量化版 (int4, 488MB), 替换项目内 MiniTransformer (256dim/4层玩具模型).</p>
 *
 * <h3>模型规格</h3>
 * <ul>
 *   <li>输入 input_ids: int64[batch, seq_len]</li>
 *   <li>输入 attention_mask: int64[batch, seq_len]</li>
 *   <li>输出 logits: float32[batch, seq_len, vocab_size=151936]</li>
 * </ul>
 *
 * <h3>对话模板 (Qwen2.5 ChatML)</h3>
 * <pre>
 * &lt;|im_start|&gt;system\n{system}&lt;|im_end|&gt;\n
 * &lt;|im_start|&gt;user\n{prompt}&lt;|im_end|&gt;\n
 * &lt;|im_start|&gt;assistant\n
 * </pre>
 *
 * <h3>限制</h3>
 * <ul>
 *   <li>CPU int4 推理, 约 5-15 tokens/秒</li>
 *   <li>无 KV cache (每次重算), 适合短输入 (≤ 1K tokens)</li>
 *   <li>不实现 beam search, 走 greedy</li>
 * </ul>
 */
@Slf4j
@Service
public class OnnxQwenChatService {

    @Value("${minimax.onnx-vision.qwen-path:./data/models/qwen2.5-0.5b-instruct/model_quantized.onnx}")
    private String modelPath;

    @Value("${minimax.onnx-vision.qwen-tokenizer:./data/models/qwen2.5-0.5b-instruct/tokenizer.json}")
    private String tokenizerPath;

    @Value("${minimax.onnx-vision.qwen-config:./data/models/qwen2.5-0.5b-instruct/config.json}")
    private String configPath;

    @Value("${minimax.onnx-vision.qwen-max-tokens:512}")
    private int maxNewTokens;

    @Value("${minimax.onnx-vision.qwen-max-context:2048}")
    private int maxContext;

    private OrtEnvironment env;
    private OrtSession session;
    private SimpleBpeTokenizer tokenizer = new SimpleBpeTokenizer();

    // Qwen2.5 特殊 token ids
    private static final int IM_START = 151644;  // <|im_start|>
    private static final int IM_END   = 151645;  // <|im_end|>
    private static final int EOS      = 151645;  // 同 im_end
    private static final int PAD      = 151643;  // <|endoftext|>
    private static final int NL       = 198;     // \n

    private int vocabSize = 151936;
    private int hiddenDim = 896;
    private int numLayers = 24;

    public boolean isReady() { return session != null && tokenizer.vocabSize() > 0; }
    public String getModelPath() { return modelPath; }
    public int getVocabSize() { return vocabSize; }

    @PostConstruct
    public void init() {
        // 1. Tokenizer
        File tf = new File(tokenizerPath);
        if (tf.exists()) {
            // Qwen2.5 tokenizer.json 用 BPE, vocab 字段或 model.vocab
            try {
                ObjectMapper om = new ObjectMapper();
                JsonNode root = om.readTree(tf);
                // 提取 vocab → 写到临时 vocab.json 供 SimpleBpeTokenizer 加载
                JsonNode vocab = root.path("model").path("vocab");
                if (vocab.isObject()) {
                    File tmpVocab = new File(tf.getParentFile(), "_vocab_tmp.json");
                    om.writeValue(tmpVocab, vocab);
                    tokenizer.loadFromVocabJson(tmpVocab.getAbsolutePath(), null);
                    tmpVocab.delete();
                }
            } catch (Exception e) {
                log.warn("[OnnxQwen] tokenizer 加载失败: {}", e.getMessage());
            }
        } else {
            log.warn("[OnnxQwen] tokenizer.json 不存在: {}", tokenizerPath);
        }
        // 2. Config
        File cf = new File(configPath);
        if (cf.exists()) {
            try {
                JsonNode cfg = new ObjectMapper().readTree(cf);
                vocabSize = cfg.path("vocab_size").asInt(151936);
                hiddenDim = cfg.path("hidden_size").asInt(896);
                numLayers = cfg.path("num_hidden_layers").asInt(24);
            } catch (Exception ignored) {}
        }
        // 3. Model
        File mf = new File(modelPath);
        if (!mf.exists()) {
            log.warn("[OnnxQwen] 模型不存在: {} — LLM 升级不可用", modelPath);
            return;
        }
        try {
            env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setIntraOpNumThreads(4);
            opts.setGraphOptimizationLevel(OrtSession.SessionOptions.OptLevel.EXTENDED_OPT);
            session = env.createSession(modelPath, opts);
            log.info("[OnnxQwen] ✅ 加载成功: {} (vocab={}, hidden={}, layers={})",
                modelPath, vocabSize, hiddenDim, numLayers);
        } catch (Exception e) {
            log.error("[OnnxQwen] 加载失败: {}", e.getMessage());
            session = null;
        }
    }

    @PreDestroy
    public void destroy() {
        try { if (session != null) session.close(); } catch (Exception ignored) {}
        try { if (env != null) env.close(); } catch (Exception ignored) {}
    }

    /**
     * 生成回复 (greedy)
     *
     * @param prompt 用户输入
     * @param system 系统 prompt (可选)
     * @return 生成的回复文本
     */
    public ChatResult chat(String prompt, String system) {
        return chat(prompt, system, maxNewTokens);
    }

    public ChatResult chat(String prompt, String system, int maxTokens) {
        if (!isReady()) {
            return new ChatResult("", "Qwen2.5 模型未就绪", 0);
        }
        if (prompt == null || prompt.isEmpty()) {
            return new ChatResult("", "prompt 不能为空", 0);
        }
        long start = System.currentTimeMillis();
        try {
            // 1. 拼装 ChatML prompt
            String chatml = buildChatML(prompt, system == null ? "你是 MiniMax 智能助手, 简洁专业地回答问题。" : system);
            log.debug("[OnnxQwen] ChatML: {}", chatml);
            // 2. Tokenize (用 SimpleBpeTokenizer, 注意 Qwen 用 BPE + 特殊 token)
            long[] inputIds = simpleEncode(chatml);
            if (inputIds.length == 0) {
                return new ChatResult("", "tokenize 失败", System.currentTimeMillis() - start);
            }
            // 3. 自回归生成
            StringBuilder output = new StringBuilder();
            int generated = 0;
            int maxLen = Math.min(inputIds.length + maxTokens, maxContext);

            while (inputIds.length < maxLen && generated < maxTokens) {
                long[] shape = {1, inputIds.length};
                long[] attn = new long[inputIds.length];
                Arrays.fill(attn, 1L);

                Map<String, OnnxTensor> inputs = new HashMap<>();
                inputs.put("input_ids", OnnxTensor.createTensor(env,
                    LongBuffer.wrap(inputIds), shape));
                inputs.put("attention_mask", OnnxTensor.createTensor(env,
                    LongBuffer.wrap(attn), shape));

                int nextToken;
                try (OrtSession.Result result = session.run(inputs)) {
                    OnnxTensor logitsT = (OnnxTensor) result.get(0);
                    float[][][] logits = (float[][][]) logitsT.getValue();
                    int lastIdx = inputIds.length - 1;
                    float[] lastLogits = logits[0][lastIdx];
                    // argmax (greedy)
                    int best = 0;
                    float bestScore = -Float.MAX_VALUE;
                    for (int v = 0; v < lastLogits.length; v++) {
                        if (lastLogits[v] > bestScore) {
                            bestScore = lastLogits[v];
                            best = v;
                        }
                    }
                    nextToken = best;
                }

                if (nextToken == EOS || nextToken == IM_END) break;
                if (nextToken == PAD) break;
                // 解码 token → string
                String tokStr = decodeToken(nextToken);
                if (tokStr != null) output.append(tokStr);
                generated++;

                // 拼接到输入
                long[] newIds = new long[inputIds.length + 1];
                System.arraycopy(inputIds, 0, newIds, 0, inputIds.length);
                newIds[inputIds.length] = nextToken;
                inputIds = newIds;
            }

            long cost = System.currentTimeMillis() - start;
            return new ChatResult(output.toString().trim(), null, cost);
        } catch (Exception e) {
            log.error("[OnnxQwen] 生成失败: {}", e.getMessage(), e);
            return new ChatResult("", "生成失败: " + e.getMessage(),
                System.currentTimeMillis() - start);
        }
    }

    /**
     * 拼装 Qwen2.5 ChatML 模板
     */
    private String buildChatML(String user, String system) {
        StringBuilder sb = new StringBuilder();
        sb.append("<|im_start|>system\n").append(system).append("<|im_end|>\n");
        sb.append("<|im_start|>user\n").append(user).append("<|im_end|>\n");
        sb.append("<|im_start|>assistant\n");
        return sb.toString();
    }

    /**
     * 简化版 BPE tokenize: 直接用 vocab hash, 不严格 BPE 合并
     * 对 ChatML 模板中的已知 token (im_start/im_end/<|...|>) 直接查表
     * 对中文文本用 char-level fallback
     */
    private long[] simpleEncode(String text) {
        List<Long> ids = new ArrayList<>();
        // 按特殊 token 拆分
        StringBuilder buf = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            // 检测 <|...|>
            if (i + 1 < text.length() && text.charAt(i) == '<' && text.charAt(i + 1) == '|') {
                int end = text.indexOf("|>", i);
                if (end > 0) {
                    // flush buf
                    encodeChunk(buf.toString(), ids);
                    buf.setLength(0);
                    String special = text.substring(i, end + 2);
                    Integer id = tokenizer.getEncoder().get(special);
                    if (id != null) {
                        ids.add(id.longValue());
                    }
                    i = end + 2;
                    continue;
                }
            }
            buf.append(text.charAt(i));
            i++;
        }
        encodeChunk(buf.toString(), ids);
        return ids.stream().mapToLong(Long::longValue).toArray();
    }

    private void encodeChunk(String chunk, List<Long> ids) {
        if (chunk.isEmpty()) return;
        // 字符级 + 词级查表
        String[] tokens = chunk.split("\\s+");
        for (String tok : tokens) {
            Integer id = tokenizer.getEncoder().get(tok);
            if (id != null) {
                ids.add(id.longValue());
            } else {
                // char-level
                for (char c : tok.toCharArray()) {
                    String s = String.valueOf(c);
                    Integer cid = tokenizer.getEncoder().get(s);
                    if (cid != null) {
                        ids.add(cid.longValue());
                    } else {
                        // UNK
                        Integer unk = tokenizer.getEncoder().get("<|endoftext|>");
                        if (unk != null) ids.add(unk.longValue());
                    }
                }
                // 词尾空格
                Integer sp = tokenizer.getEncoder().get(" ");
                if (sp != null) ids.add(sp.longValue());
            }
        }
    }

    private String decodeToken(int id) {
        // 反向查 vocab
        for (Map.Entry<String, Integer> e : tokenizer.getEncoder().entrySet()) {
            if (e.getValue() == id) {
                String s = e.getKey();
                // BPE 空格标记
                if (s.startsWith("Ġ")) return " " + s.substring(1);
                return s;
            }
        }
        return null;
    }

    public record ChatResult(String text, String error, long costMs) {
        public boolean isSuccess() { return error == null; }
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("text", text);
            m.put("error", error);
            m.put("costMs", costMs);
            m.put("ready", error == null);
            m.put("length", text == null ? 0 : text.length());
            return m;
        }
    }
}
