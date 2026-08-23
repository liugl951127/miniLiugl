package com.minimax.ai.multimodal.audio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;

/**
 * Whisper BPE Tokenizer (V7.2)
 *
 * <p>从 <code>tokenizer.json</code> (HuggingFace format) 加载 vocabulary.</p>
 *
 * <h3>特殊 token (Whisper)</h3>
 * <ul>
 *   <li>SOT: 50257 (start of transcript)</li>
 *   <li>EOT: 50256 (end of transcript)</li>
 *   <li>ENGLISH: 50259</li>
 *   <li>CHINESE: 50260</li>
 *   <li>NOTIMESTAMP_BEGIN: 50363 (无时间戳, 纯文字输出)</li>
 *   <li>TRANSCRIBE: 50358</li>
 *   <li>TRANSLATE: 50357</li>
 * </ul>
 */
@Slf4j
public class WhisperTokenizer {

    public static final int SOT         = 50257;
    public static final int EOT         = 50256;
    public static final int ENGLISH     = 50259;
    public static final int CHINESE     = 50260;
    public static final int TRANSCRIBE  = 50358;
    public static final int TRANSLATE   = 50357;
    public static final int NOTIMESTAMPS = 50363;

    private final Map<String, Integer> encoder = new LinkedHashMap<>();
    private final Map<Integer, String> decoder = new HashMap<>();
    private final Map<String, Integer> addedTokensEncoder = new HashMap<>();

    public boolean loadFromFile(String path) {
        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode root = om.readTree(new File(path));
            JsonNode model = root.path("model");
            JsonNode vocab = model.path("vocab");
            if (vocab.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = vocab.fields();
                int idx = 0;
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    encoder.put(e.getKey(), idx);
                    decoder.put(idx, e.getKey());
                    idx++;
                }
            }
            // Added tokens
            JsonNode added = root.path("added_tokens");
            if (added.isArray()) {
                for (JsonNode t : added) {
                    String content = t.path("content").asText();
                    int id = t.path("id").asInt();
                    addedTokensEncoder.put(content, id);
                }
            }
            log.info("[WhisperTokenizer] vocab 加载 {} tokens from {}", encoder.size(), path);
            return true;
        } catch (Exception e) {
            log.error("[WhisperTokenizer] 加载失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * token ids → text
     */
    public String decode(int[] tokens) {
        StringBuilder sb = new StringBuilder();
        for (int t : tokens) {
            if (t == EOT || t == SOT) continue;
            // 时间戳 token (>= 50364) 跳过
            if (t >= 50364) continue;
            String s = decoder.get(t);
            if (s != null) {
                // Whisper 使用 GPT-2 BPE, 词首 Ġ 转为空格
                if (s.startsWith("Ġ")) {
                    sb.append(' ').append(s.substring(1));
                } else {
                    sb.append(s);
                }
            }
        }
        return sb.toString().trim();
    }

    /**
     * text → token ids (BPE 编码, 简化版)
     *
     * <p>本简化版不严格按 HuggingFace BPE 合并, 改为按词查 vocab.
     * 对 Whisper 解码场景 (仅在 inference 后处理) 影响很小, 实际推理不需要此方法.</p>
     */
    public int[] encode(String text) {
        if (text == null || text.isEmpty()) return new int[0];
        List<Integer> ids = new ArrayList<>();
        for (String word : text.split("\\s+")) {
            Integer id = encoder.get(word);
            if (id != null) {
                ids.add(id);
            } else {
                // 子词 fallback
                for (int i = 1; i <= word.length(); i++) {
                    Integer sid = encoder.get(word.substring(0, i));
                    if (sid != null) ids.add(sid);
                }
            }
        }
        return ids.stream().mapToInt(Integer::intValue).toArray();
    }

    public int vocabSize() { return encoder.size(); }
    public Map<String, Integer> getEncoder() { return encoder; }
}
