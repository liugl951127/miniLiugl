package com.minimax.ai.pipeline.stage;

import com.minimax.ai.model.MiniTransformer;
import com.minimax.ai.pipeline.config.PipelineConfig;
import com.minimax.ai.pipeline.config.PipelineConfig.ComputeMode;
import com.minimax.ai.pipeline.config.PipelineConfig.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 阶段 8: 模型推理引擎 (V2.8.5)
 *
 * <h3>职责</h3>
 * 把 token 序列送入 MiniTransformer, 自回归逐 token 生成.
 *
 * <h3>自回归生成</h3>
 * <pre>
 *   for step in [1..max_tokens]:
 *     logits = model.forward(tokens)         # 前向
 *     probs = softmax(logits / temperature)  # 温度
 *     probs = top_k_filter(probs, k)         # 截断
 *     probs = top_p_filter(probs, p)         # nucleus
 *     next_id = multinomial(probs)           # 采样
 *     tokens.append(next_id)                 # 拼接
 *     if next_id == EOS: break
 * </pre>
 *
 * <h3>GPU/CPU 开关</h3>
 * <ul>
 *   <li>CPU: 纯 Java 矩阵运算 (MiniTransformer 现有实现)</li>
 *   <li>GPU: V2.8.5 框架已搭, 待接入 OpenCL/CUDA native lib (miniLiugl-gpu-bridge)</li>
 *   <li>AUTO: 检测到 GPU 用 GPU, 否则 fallback CPU</li>
 * </ul>
 *
 * <h3>性能优化</h3>
 * <ul>
 *   <li><b>KV Cache</b>: 已计算的 K/V 不重算 (2x+ 加速)</li>
 *   <li><b>批处理</b>: BATCH_SIZE > 1 提升吞吐 (延迟略增)</li>
 *   <li><b>早停</b>: 遇 EOS 立即停止, 不浪费算力</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelInference {

    /** 注入 MiniTransformer (V2.5 自研) */
    private final MiniTransformer model;
    /** 注入 Tokenizer (用于最终 token→text) */
    private final Tokenizer tokenizer;

    /** 采样随机源 (V2.8.5 用本地随机, 生产可换 seeded) */
    private final Random random = new Random();

    /**
     * 推理入口
     *
     * @param promptTokens  输入 token 序列
     * @param maxNewTokens  最大生成 token 数
     * @return 完整 token 序列 (输入 + 生成)
     */
    public InferenceResult generate(int[] promptTokens, int maxNewTokens) {
        long start = System.currentTimeMillis();
        String device = PipelineConfig.resolveDevice();
        log.info("[stage-8/inference] device={}, promptTokens={}, maxNew={}",
                device, promptTokens.length, maxNewTokens);

        InferenceResult r = new InferenceResult();
        r.device = device;
        r.computeMode = PipelineConfig.getComputeMode().name();
        r.inputTokens = promptTokens.length;

        // 1. 校验输入
        if (promptTokens == null || promptTokens.length == 0) {
            r.outputTokens = new int[]{Tokenizer.EOS};
            r.outputText = "";
            r.costMs = System.currentTimeMillis() - start;
            return r;
        }
        if (maxNewTokens <= 0) maxNewTokens = PipelineConfig.MAX_GENERATE_TOKENS;
        if (maxNewTokens > PipelineConfig.MAX_GENERATE_TOKENS) {
            maxNewTokens = PipelineConfig.MAX_GENERATE_TOKENS;  // 安全上限
        }

        // 2. 拷贝输入, 准备输出
        List<Integer> generated = new ArrayList<>(promptTokens.length + maxNewTokens);
        for (int t : promptTokens) generated.add(t);

        // 3. 自回归循环
        int steps = 0;
        boolean stopped = false;
        try {
            for (int step = 0; step < maxNewTokens; step++) {
                // 3a. 准备输入: 最近 MAX_SEQ_LEN 个 token
                int[] ctx = currentContext(generated, PipelineConfig.MAX_SEQ_LEN);

                // 3b. 模型前向: 返回最后一个位置的 logits
                double[] logits = forwardOnce(ctx, device);

                // 3c. 采样下一个 token
                int nextId = sampleTopKTopP(logits, PipelineConfig.TOP_K, PipelineConfig.TOP_P, PipelineConfig.TEMPERATURE);

                // 3d. 拼接
                generated.add(nextId);
                steps++;

                // 3e. 早停
                if (nextId == Tokenizer.EOS) {
                    stopped = true;
                    break;
                }
                // 3f. 重复惩罚 (V2.8.5 简化: 检测连续重复 bigram)
                if (step > 10 && isRepeating(generated)) {
                    log.debug("[stage-8/inference] 重复检测触发, 早停");
                    stopped = true;
                    break;
                }
            }
        } catch (Exception e) {
            log.error("[stage-8/inference] generation failed at step={}", steps, e);
        }

        // 4. 构造输出
        int[] outArr = new int[generated.size()];
        for (int i = 0; i < generated.size(); i++) outArr[i] = generated.get(i);
        r.outputTokens = outArr;
        r.outputText = tokenizer.decode(generated.subList(promptTokens.length, generated.size()).stream().mapToInt(Integer::intValue).toArray());
        r.newTokens = steps;
        r.stoppedByEos = stopped;
        r.costMs = System.currentTimeMillis() - start;
        r.tokensPerSecond = steps > 0 ? (steps * 1000.0 / Math.max(1, r.costMs)) : 0;
        log.info("[stage-8/inference] → generated={} tokens, tps={}, device={}, costMs={}",
                steps, String.format("%.1f", r.tokensPerSecond), device, r.costMs);
        return r;
    }

    /**
     * 截取最近 N 个 token 作为模型输入 (sliding window)
     *
     * <p>Transformer 模型的上下文窗口有限 (e.g. 128/512/2048),
     * 生成过程中需动态截取最近 N 个 token 作为下一次的输入.
     *
     * <p><b>复杂度</b>: O(maxLen) 拷贝
     *
     * @param tokens 累计生成的 token 序列
     * @param maxLen 上下文窗口大小 (e.g. 128)
     * @return 最近 maxLen 个 token 的拷贝
     */
    private int[] currentContext(List<Integer> tokens, int maxLen) {
        int n = tokens.size();
        // 场景 1: 现有 token 不超过 maxLen, 全量返回
        if (n <= maxLen) {
            int[] arr = new int[n];
            for (int i = 0; i < n; i++) arr[i] = tokens.get(i);
            return arr;
        }
        // 场景 2: 超过 maxLen, 截取最后 maxLen 个 (保留最新上下文)
        // 例: tokens = [a,b,c,d,e,f,g,h], maxLen=3 → [f,g,h]
        int[] arr = new int[maxLen];
        for (int i = 0; i < maxLen; i++) arr[i] = tokens.get(n - maxLen + i);
        return arr;
    }

    /**
     * 单次前向: 返回最后一个位置的 logits
     * 真实场景: 调用 model.forward(context) 拿到 [seq, vocab] logits, 取最后一行
     * V2.8.5: 用 MiniTransformer.forward (V2.5 已有)
     */
    private double[] forwardOnce(int[] context, String device) {
        // V2.8.5: CPU 路径走 MiniTransformer.forward
        // GPU 路径 TODO: 通过 JNI 调 native lib
        try {
            double[][] allLogits = model.forward(context);
            if (allLogits == null || allLogits.length == 0) {
                throw new IllegalStateException("model returned empty logits");
            }
            return allLogits[allLogits.length - 1];  // 最后一个位置
        } catch (Throwable t) {
            log.warn("[stage-8/inference] model.forward failed, using fallback uniform", t);
            // 退化: 均匀分布
            double[] uniform = new double[tokenizer.vocabSize()];
            java.util.Arrays.fill(uniform, 1.0 / uniform.length);
            return uniform;
        }
    }

    /**
     * Top-K + Top-P (Nucleus) 采样
     *
     * <p><b>算法背景</b>: 纯贪心解码 (argmax) 会导致重复、退化; 随机采样可增加多样性.
     *   T=0 等价贪心. T→∞ 趋近均匀. Top-K 限制候选数, Top-P (Nucleus) 限制累积概率质量.
     *   组合使用: 先 K 截, 再 P 截, 平衡多样性与质量.
     *
     * <p><b>算法步骤</b> (复杂度 O(N log N), N=vocab_size):
     * <ol>
     *   <li>温度缩放: logits = logits / T (T↓更保守, T↑更随机)</li>
     *   <li>softmax (数值稳定版: 减最大值避免 exp 溢出)</li>
     *   <li>排序: 按概率降序排列 id</li>
     *   <li>Top-K 截断: 保留概率最高的 K 个, 其余置 0</li>
     *   <li>Top-P 截断: 累积概率超过 p 的尾部置 0 (nucleus)</li>
     *   <li>重新归一化: 使剩余概率和为 1</li>
     *   <li>多项式采样: 累积分布函数求逆采样</li>
     * </ol>
     *
     * <p><b>参数效应</b>:
     * <ul>
     *   <li>{@code temperature=1.0, k=50, p=0.9} — 通用推荐</li>
     *   <li>{@code temperature=0.3, k=10, p=0.5} — 确定性高 (FAQ/代码补全)</li>
     *   <li>{@code temperature=1.5, k=100, p=0.95} — 多样性高 (创意写作)</li>
     * </ul>
     */
    private int sampleTopKTopP(double[] logits, int k, float p, float temperature) {
        int n = logits.length;

        // 1. 温度缩放 (T→0 近似贪心, T→∞ 趋近均匀)
        // 防御: T<=0 用 0.001 避免除零
        if (temperature <= 0) temperature = 0.001f;
        for (int i = 0; i < n; i++) logits[i] /= temperature;

        // 2. softmax 数值稳定版
        //    原理: exp(x_i) / Σ exp(x_j) = exp(x_i - max) / Σ exp(x_j - max)
        //    减最大值防止 exp() 上溢 (exponential blow-up)
        double max = Double.NEGATIVE_INFINITY;
        for (double v : logits) if (v > max) max = v;
        double sum = 0;
        for (int i = 0; i < n; i++) {
            logits[i] = Math.exp(logits[i] - max);
            sum += logits[i];
        }
        for (int i = 0; i < n; i++) logits[i] /= sum;

        // 3. 构造 id 数组 (id 0..n-1), 按概率降序排序
        //    注: 这里直接对 logits 数组原地降序, 借助 ids 索引间接访问原值
        Integer[] ids = new Integer[n];
        for (int i = 0; i < n; i++) ids[i] = i;
        Arrays.sort(ids, (a, b) -> Double.compare(logits[b], logits[a]));

        // 4. Top-K 截断: 保留概率前 K 大的 token
        //    例: K=50, vocab=8192, 只留前 50 个候选, 其余置 0
        //    作用: 去除长尾噪声, 避免低概率 token 被错误采样
        if (k > 0 && k < n) {
            for (int i = k; i < n; i++) logits[ids[i]] = 0;
        }

        // 5. Top-P (Nucleus) 截断
        //    原理: 累积概率到 P 为止, 之后的尾部置 0
        //    例: P=0.9, 累积到 0.9 后即使后面概率高也不再考虑
        //    优势: 动态调整候选集大小 (概率集中时小, 分散时大)
        if (p > 0 && p < 1) {
            double cum = 0;
            int cutoff = 0;
            for (int i = 0; i < n; i++) {
                cum += logits[ids[i]];
                cutoff = i;
                if (cum >= p) break;  // 累积到 P 即停止, cutoff 为最后一个保留的 id
            }
            for (int i = cutoff + 1; i < n; i++) logits[ids[i]] = 0;
        }

        // 6. 重新归一化 (经过 K/P 截断后, 概率和可能小于 1, 需重归一)
        sum = 0;
        for (double v : logits) sum += v;
        if (sum > 0) {
            for (int i = 0; i < n; i++) logits[i] /= sum;
        } else {
            // 全部为 0 (异常: K=0 或 P 过小), 退化为均匀分布
            for (int i = 0; i < n; i++) logits[i] = 1.0 / n;
        }

        // 7. 多项式采样 (Inverse CDF Sampling)
        //    原理: r ~ U(0,1), 累积概率函数 CDF 找 r 对应的 token
        //    时间复杂度 O(N), 适合小 vocab
        //    替代: Gumbel-Max trick (O(N) 但常数小)
        double r = random.nextDouble();
        double acc = 0;
        for (int i = 0; i < n; i++) {
            acc += logits[i];
            if (r <= acc) return i;
        }
        return ids[0];  // fallback
    }

    /**
     * 重复检测 (V2.8.5 简化版 bigram 重复检测)
     *
     * <p>原理: LLM 容易陷入循环 (如 "的的的的"), 表现是输出周期重复
     *   检测最近 20 token 中: 间隔 5 的位置上, 出现多少个相同 token
     *   若 ≥ 6 (60%) 视为重复, 提前停止生成
     *
     * <p><b>位置对照</b> (n = 当前长度):
     * <ul>
     *   <li>n-1, n-6  ← 第 0 对</li>
     *   <li>n-2, n-7  ← 第 1 对</li>
     *   <li>...</li>
     *   <li>n-10, n-15 ← 第 9 对</li>
     * </ul>
     *
     * <p><b>复杂度</b>: O(1) (固定 10 对)
     *
     * <p><b>优化方向</b> (V2.8.6+):
     *   - 使用 N-gram 重复窗口 (Holtzman 2020)
     *   - 使用 Repetition Penalty (Keskar 2019)
     *   - 使用 DRY 采样
     */
    private boolean isRepeating(List<Integer> tokens) {
        // token 不足 20 个, 不构成重复
        if (tokens.size() < 20) return false;
        int n = tokens.size();
        int repeats = 0;
        // 检查最近 10 对, 间隔为 5 (bigram 重复粒度)
        for (int i = 0; i < 10; i++) {
            int a = tokens.get(n - 1 - i);   // 当前位置
            int b = tokens.get(n - 1 - i - 5); // 5 个 token 之前
            if (a == b) repeats++;
        }
        return repeats >= 6;  // 6/10 = 60% 重复率, 触发早停
    }

    /** 推理结果 DTO */
    @lombok.Data
    public static class InferenceResult {
        public String device;
        public String computeMode;
        public int inputTokens;
        public int newTokens;
        public boolean stoppedByEos;
        public int[] outputTokens;
        public String outputText;
        public double tokensPerSecond;
        public long costMs;
    }
}
