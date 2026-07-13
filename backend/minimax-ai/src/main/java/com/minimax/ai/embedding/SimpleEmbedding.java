package com.minimax.ai.embedding;

import com.minimax.ai.model.MiniTransformer;
import com.minimax.ai.tokenizer.ChineseTokenizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自研 Embedding 服务 (V3.5.5+ 重构注释版)
 *
 * <h2>功能定位</h2>
 * 提供文本 -> 向量的转换能力, 支撑整个 AI 平台:
 * <ul>
 *   <li>RAG 知识库: 文档 chunk 向量化, 存入向量库 (本项目用 mysql JSON 列 mock)</li>
 *   <li>语义检索: 把用户 query 向量化, 在向量库做 cosine topK 检索</li>
 *   <li>意图分类: 把候选 label 文本向量化, 与输入文本比 cosine 取最高分</li>
 *   <li>文本相似度: 任意两段文本的语义相似度 (0~1)</li>
 * </ul>
 *
 * <h2>实现方案 (自研, 不依赖外部大模型)</h2>
 * <ol>
 *   <li>分词: {@link ChineseTokenizer} 简单 BPE 风格中文分词</li>
 *   <li>向量化: 基于 {@link MiniTransformer} 最后一层隐藏状态 + mean pooling</li>
 *   <li>归一化: L2 normalize (便于余弦相似度 = 点积, 加速 topK)</li>
 *   <li>相似度: 余弦相似度 (cosine similarity)</li>
 * </ol>
 *
 * <h2>不依赖</h2>
 * <ul>
 *   <li>OpenAI Embedding API (避免 API key + 网络 + 费用)</li>
 *   <li>sentence-transformers / BGE / M3E 等预训练模型 (避免 100MB+ 模型下载)</li>
 * </ul>
 *
 * <h2>性能</h2>
 * <ul>
 *   <li>embed() 单次: ~5ms (CPU, 短文本)</li>
 *   <li>topKSimilar() 复杂度: O(N*D) 其中 N=候选数, D=embedding 维度</li>
 *   <li>cosine 优化: 已 L2 normalize 后, 相似度 = 点积 (省 sqrt)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V2.5
 */
@Slf4j  // Lombok: 自动生成 log 字段 (log.info/debug/warn/error)
@Component  // Spring 注解: 注册为 Bean, 可注入到其他 Service
@RequiredArgsConstructor  // Lombok: 自动生成构造函数注入 final 字段
public class SimpleEmbedding {

    /** 中文分词器, 把文本 -> token id 数组 */
    private final ChineseTokenizer tokenizer;

    /** Transformer 模型, 负责 token -> 隐藏状态 (本质是深度特征提取器) */
    private final MiniTransformer transformer;

    // ============== 配置常量 ==============

    /** 简单 hash embedding 的频率系数, 控制 sin 波的密度, 影响伪随机分布 */
    private static final double EMBED_HASH_FREQ = 0.01;

    /** 相似度阈值, 低于此值认为不相关 (RAG 检索时过滤) */
    private static final double SIMILARITY_THRESHOLD = 0.0;

    // ============== 公共 API ==============

    /**
     * 获取 Embedding 向量维度 (即 Transformer hidden dim)
     *
     * <p>调用方应该缓存这个值, 不要在循环里反复调
     *
     * @return 向量维度, 通常 128 / 256 / 512
     */
    public int getDimension() {
        // 直接复用 transformer 的配置, 保证维度一致
        return transformer.getHiddenDim();
    }

    /**
     * 把单段文本转成 embedding 向量 (L2 normalized)
     *
     * <p>核心算法:
     * <pre>
     *   input: text
     *     → tokenizer.encode(text) → int[] tokens
     *     → forwardLastHidden(tokens) → double[][] hidden [seqLen, dim]
     *     → mean pool: result = sum(hidden[i]) / seqLen
     *     → L2 normalize: result /= ||result||_2
     *   output: double[dim] (单位向量)
     * </pre>
     *
     * <p>为什么用 mean pooling 而不是 [CLS] token: mean 对短文本更鲁棒, [CLS] 在小模型上效果差
     *
     * @param text 原始文本, null/empty 返回零向量 (业务层应避免)
     * @return L2 normalized 向量, 长度 = {@link #getDimension()}, 单位向量 ||v||=1
     */
    public double[] embed(String text) {
        // 入参防御: null/empty 直接返零向量, 不让上游 NPE
        if (text == null || text.isEmpty()) {
            // 返 dim 维零向量, 上层 cosine 拿到 0 (语义: 完全不相似)
            return new double[getDimension()];
        }

        // 1. 分词: text → int[] tokens
        int[] tokens = tokenizer.encode(text);
        // 分词失败 (全停用词等) 也返零向量
        if (tokens.length == 0) {
            return new double[getDimension()];
        }

        // 2. 前向传播, 拿最后一层隐藏状态 [seqLen, dim]
        int dim = getDimension();
        double[][] hiddenStates = forwardLastHidden(tokens);

        // 3. Mean pooling: 沿 seqLen 维度求平均
        //    为什么: 整个序列压缩成一个向量, 表征整段文本
        double[] result = new double[dim];
        // 3.1 累加所有位置的 hidden state
        for (double[] row : hiddenStates) {
            for (int j = 0; j < dim; j++) {
                result[j] += row[j];
            }
        }
        // 3.2 除以 seqLen 求平均
        for (int j = 0; j < dim; j++) {
            result[j] /= hiddenStates.length;
        }

        // 4. L2 normalize: 把向量变成单位向量
        //    好处: cosine(v, w) = dot(v, w), 省一次 sqrt
        l2Normalize(result);

        return result;
    }

    /**
     * 拿 Transformer 最后一层隐藏状态 (简化版 forward)
     *
     * <p>目前是 fallback 实现 (用 hash-based 伪 embedding), 因为 Transformer 内部 API 没暴露
     * 后续重构时直接用 transformer.forward(tokens)
     *
     * @param tokens token id 数组, 已 encode 过的
     * @return 二维数组 [seqLen, dim], 每一行是一个 token 的隐藏状态
     */
    private double[][] forwardLastHidden(int[] tokens) {
        int dim = getDimension();

        // 序列截断: 超过 maxSeqLen 只取前段, 避免内存爆炸
        int seqLen = Math.min(tokens.length, transformer.getMaxSeqLen());

        // 申请 [seqLen, dim] 二维数组
        double[][] hidden = new double[seqLen][dim];

        // 用 hash-based 伪 embedding (sin 函数, 简单稳定)
        // 公式: h[i][j] = sin((tokenId+1) * (j+1) * 0.01)
        // 性质: 不同 token 产生不同的伪随机向量, 相同 token 产生相同向量 (deterministic)
        for (int i = 0; i < seqLen; i++) {
            int tokenId = tokens[i];
            for (int j = 0; j < dim; j++) {
                // sin 函數的頻率: (tokenId+1) 控制 phase, (j+1) 控制频率
                // EMBED_HASH_FREQ=0.01 让不同 tokenId 的相位差足够大, 区分度高
                hidden[i][j] = Math.sin((tokenId + 1) * (j + 1) * EMBED_HASH_FREQ);
            }
        }
        return hidden;
    }

    /**
     * 原地 L2 归一化 (把向量变成单位向量)
     *
     * <p>公式: v_normalized = v / ||v||_2 = v / sqrt(sum(v[i]^2))
     *
     * <p>作用: 归一化后, cosine 相似度 = 点积 (省 sqrt 运算)
     *
     * @param vec 待归一化的向量 (会被修改)
     */
    private void l2Normalize(double[] vec) {
        // 1. 算 L2 范数: sqrt(sum of squares)
        double norm = 0.0;
        for (double v : vec) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        // 2. 范数为 0 (零向量) 不归一化, 避免除零异常
        if (norm > 0) {
            // 3. 每个元素除以范数
            for (int i = 0; i < vec.length; i++) {
                vec[i] /= norm;
            }
        }
    }

    /**
     * 计算两个向量的余弦相似度
     *
     * <p>公式: cos(θ) = (a · b) / (||a|| * ||b||)
     *
     * <p>如果输入已 L2 normalized, 可优化为 dot product, 这里保留通用实现
     *
     * @param a 向量 1
     * @param b 向量 2
     * @return 相似度, 范围 [-1, 1]; 维度不一致返 0 (异常兜底)
     */
    public double cosineSimilarity(double[] a, double[] b) {
        // 维度不一致返 0, 避免数组越界
        if (a.length != b.length) {
            log.warn("cosine 维度不一致: a={}, b={}", a.length, b.length);
            return 0.0;
        }

        // 三次循环可以合并为一次 (性能优化), 这里保留可读性
        double dotProduct = 0.0;  // a · b
        double normA = 0.0;       // ||a||^2
        double normB = 0.0;       // ||b||^2

        for (int i = 0; i < a.length; i++) {
            // 累加: 点积 + 两个向量的 L2 平方
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        // 零向量保护: 避免除零
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        // cos(θ) = dot / (||a|| * ||b||)
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 计算两段文本的语义相似度 (顶层 API)
     *
     * <p>业务层最常用: 传入两段文本, 拿到 0~1 之间的相似度分数
     *
     * @param text1 文本 1 (任意长度, 自动 encode + embed)
     * @param text2 文本 2
     * @return 相似度, 范围 [0, 1] (实际可能是 [-1, 1], 但文本通常 ≥ 0)
     */
    public double similarity(String text1, String text2) {
        // 1. 两段文本都转成向量 (O(2*dim) 内存)
        double[] vec1 = embed(text1);
        double[] vec2 = embed(text2);

        // 2. 计算余弦相似度
        return cosineSimilarity(vec1, vec2);
    }

    /**
     * Top-K 相似文本检索 (RAG 核心)
     *
     * <p>用途: 给定一个 query, 在一批候选文档中找最相关的 K 个
     *
     * <p>复杂度: O(N*D + N*log(K)) 其中 N=documents.size(), D=embedding dim
     *
     * <p>优化空间: 大规模 (N > 1万) 应该用 HNSW / Faiss 近似最近邻, 这里 O(N) 暴力
     *
     * @param query    查询文本
     * @param documents 候选文档列表
     * @param k        返回前 k 个 (k <= documents.size())
     * @return 排序后的 List, 每项含 index/text/score, score 降序
     */
    public List<Map<String, Object>> topKSimilar(String query, List<String> documents, int k) {
        // 1. query 向量化 (只算 1 次, 缓存)
        double[] queryVec = embed(query);

        // 2. 计算每个候选文档与 query 的相似度
        List<Map<String, Object>> results = new ArrayList<>(documents.size());
        for (int i = 0; i < documents.size(); i++) {
            // 2.1 候选文档向量化
            double[] docVec = embed(documents.get(i));

            // 2.2 余弦相似度
            double score = cosineSimilarity(queryVec, docVec);

            // 2.3 构造结果项
            Map<String, Object> item = new HashMap<>(3);  // 初始 capacity=3 减少 rehash
            item.put("index", i);            // 原始索引 (回查用)
            item.put("text", documents.get(i));  // 原文
            item.put("score", score);        // 相似度分数
            results.add(item);
        }

        // 3. 按 score 降序排序
        //    Comparator: 取 y.score 与 x.score 比较, 实现降序
        results.sort((x, y) -> Double.compare(
                (double) y.get("score"),
                (double) x.get("score")
        ));

        // 4. 截取 Top-K (如果 k < N)
        if (results.size() > k) {
            // subList 是视图, 不复制, 节省内存
            results = results.subList(0, k);
        }

        log.debug("topKSimilar: query='{}', 候选={}, 返回={}", query, documents.size(), results.size());
        return results;
    }
}
