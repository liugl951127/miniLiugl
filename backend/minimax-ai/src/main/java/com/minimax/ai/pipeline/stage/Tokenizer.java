package com.minimax.ai.pipeline.stage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 阶段 7+9: 分词器 + 解码器 (V2.8.5)
 *
 * <h3>职责</h3>
 * <ul>
 *   <li><b>Tokenizer</b>: 文本 → token id 序列 (喂给模型)</li>
 *   <li><b>Decoder</b>: token id 序列 → 文本 (模型输出还原)</li>
 * </ul>
 *
 * <h3>分词算法</h3>
 * BPE 简化版 (V2.8.5):
 * <ol>
 *   <li>初始化: 词表 = 基础字符 (单字 + ASCII + 标点)</li>
 *   <li>高频 bigram 合并 (训练时统计, 推理时直接查表)</li>
 *   <li>未登录字符: 映射到 UNK (id=0)</li>
 *   <li>支持特殊 token: PAD(0), BOS(1), EOS(2), UNK(3)</li>
 * </ol>
 *
 * <h3>性能优化</h3>
 * <ul>
 *   <li>使用 HashMap 索引 O(1) 查表</li>
 *   <li>预分配 token 数组避免扩容</li>
 *   <li>并行处理长文本 (V2.8.6 TODO)</li>
 * </ul>
 */
@Slf4j
@Component
public class Tokenizer {

    /** 特殊 token */
    public static final int PAD = 0;
    public static final int BOS = 1;
    public static final int EOS = 2;
    public static final int UNK = 3;

    /** token → id 映射 */
    private final Map<String, Integer> tokenToId = new HashMap<>();
    /** id → token 映射 */
    private final Map<Integer, String> idToToken = new HashMap<>();
    /** BPE 合并规则 (高频 bigram → 单 token) */
    private final Map<String, Integer> bpeMerges = new HashMap<>();
    /** 当前词表大小 */
    private volatile int vocabSize = 0;

    public Tokenizer() {
        initVocab();
    }

    /**
     * 初始化词表
     */
    private void initVocab() {
        AtomicInteger id = new AtomicInteger(4);
        // 特殊 token
        tokenToId.put("<pad>", PAD);
        tokenToId.put("<bos>", BOS);
        tokenToId.put("<eos>", EOS);
        tokenToId.put("<unk>", UNK);
        idToToken.put(PAD, "<pad>");
        idToToken.put(BOS, "<bos>");
        idToToken.put(EOS, "<eos>");
        idToToken.put(UNK, "<unk>");

        // ASCII 字符
        for (int i = 32; i < 127; i++) {
            String c = String.valueOf((char) i);
            tokenToId.put(c, id.getAndIncrement());
        }

        // 中文常用字 (一级字库 3500 简化版, 取 3000 常见)
        String commonChars = "的一是不了人我在有他这为之大来以个中上们到说时要就出会也你对开年动工方面" +
                "只把事请还看生下能而子后自前着用动方里长十第与但种文本过新比等" +
                "其三外从无明意正理气实向定问把使相体应即心反成田制表又民北现期" +
                "或被给高各并重己走同形主种地度求西东业市阶学国法口公水海山金文化";
        for (char c : commonChars.toCharArray()) {
            String s = String.valueOf(c);
            if (!tokenToId.containsKey(s)) {
                tokenToId.put(s, id.getAndIncrement());
            }
        }

        // 常见 Bigram
        String[] commonBigrams = {"我们", "他们", "你们", "什么", "怎么", "为什么", "可以", "这个", "那个",
                "公司", "系统", "数据", "分析", "生成", "处理", "查询", "更新", "删除", "下载", "上传"};
        for (String bg : commonBigrams) {
            if (!tokenToId.containsKey(bg)) {
                int tid = id.getAndIncrement();
                tokenToId.put(bg, tid);
                bpeMerges.put(bg, tid);
            }
        }
        idToToken.putAll(inverse(tokenToId));
        vocabSize = tokenToId.size();
        log.info("[tokenizer] vocab initialized: {} tokens ({} BPE merges)", vocabSize, bpeMerges.size());
    }

    /** 反转 map */
    private <K, V> Map<V, K> inverse(Map<K, V> m) {
        Map<V, K> r = new HashMap<>();
        for (Map.Entry<K, V> e : m.entrySet()) r.put(e.getValue(), e.getKey());
        return r;
    }

    /**
     * 编码: 文本 → token ids
     * 算法: 优先匹配 BPE 大词, 否则单字
     */
    public int[] encode(String text) {
        if (text == null || text.isEmpty()) return new int[]{BOS, EOS};
        // 预分配: 字符数 + 2 (BOS, EOS)
        int[] buf = new int[text.length() + 2];
        int idx = 0;
        buf[idx++] = BOS;

        int i = 0;
        while (i < text.length()) {
            // 1. 尝试 BPE bigram 匹配
            boolean matched = false;
            for (int len = 4; len >= 2; len--) {
                if (i + len > text.length()) continue;
                String candidate = text.substring(i, i + len);
                Integer tid = bpeMerges.get(candidate);
                if (tid != null) {
                    buf[idx++] = tid;
                    i += len;
                    matched = true;
                    break;
                }
            }
            if (matched) continue;

            // 2. 单字匹配
            String c = String.valueOf(text.charAt(i));
            Integer tid = tokenToId.get(c);
            buf[idx++] = tid != null ? tid : UNK;
            i++;
        }
        buf[idx++] = EOS;
        // 截断
        int[] result = new int[idx];
        System.arraycopy(buf, 0, result, 0, idx);
        return result;
    }

    /**
     * 解码: token ids → 文本
     */
    public String decode(int[] ids) {
        if (ids == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int id : ids) {
            if (id == PAD || id == BOS) continue;
            if (id == EOS) break;
            String tok = idToToken.get(id);
            if (tok != null) sb.append(tok);
        }
        return sb.toString();
    }

    /**
     * 估算 token 数 (不实际编码, 快速估算)
     * 中文: 1 字 ≈ 1 token (BPE 后更少)
     * 英文: 1 词 ≈ 1-2 token
     */
    public int estimate(String text) {
        if (text == null) return 0;
        int cjk = 0, other = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) cjk++;
            else other++;
        }
        return cjk + (other / 3);  // 英文 3 字符 ≈ 1 token
    }

    public int vocabSize() { return vocabSize; }
}
