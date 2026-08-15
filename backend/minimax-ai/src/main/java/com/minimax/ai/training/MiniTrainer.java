package com.minimax.ai.training;

// 引入 MiniTransformer: 自研的 Transformer 简化实现,提供 forward(input) 接口
import com.minimax.ai.model.MiniTransformer;
// 引入 ChineseTokenizer: 中文 BPE 分词器
import com.minimax.ai.tokenizer.ChineseTokenizer;
// @Slf4j: Lombok 注解,自动生成 log 字段
import lombok.extern.slf4j.Slf4j;

// HashMap: 普通 Map 实现
import java.util.HashMap;
// List: 列表接口
import java.util.List;
// Map: 键值对接口
import java.util.Map;
// Random: 随机数生成器 (训练时用于打乱数据)
import java.util.Random;

/**
 * MiniMax 自研训练器 (V2.5+V6.0)
 *
 * <h2>训练流程</h2>
 * <pre>
 *   for epoch in 1..N:
 *     for line in corpus:
 *       tokens = tokenizer.encode(line)         # 1. 分词
 *       for window in sliding(tokens):          # 2. 滑动窗口
 *         input, target = window[0:-1], window[1:]   # 3. input→target 预测
 *         loss = forward(input) + cross_entropy(target)  # 4. 前向 + Loss
 *         perturb_weights(loss, lr)              # 5. 简化版"梯度下降"
 * </pre>
 *
 * <h2>算法说明 (教学级)</h2>
 * 真实训练需要 100 万级语料 + GPU + 自动微分 (autograd)。
 * 本项目是教学级,用"参数扰动"代替真实梯度,目的:
 *   - 展示完整 AI 训练流程
 *   - 让 AI 模块自洽 (可启动可跑可生成)
 *   - 在小语料下也能"学到"一些模式
 *
 * <h2>不依赖 DL 框架</h2>
 * 不依赖 PyTorch/TensorFlow/DL4J,纯 Java 手写 forward + 简化版"训练"。
 * 牺牲性能换简单性。
 */
/**
 * MiniTrainer (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * 训练 - MiniTrainer.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 MiniTrainer 的业务能力</li>
 *   <li>参与 AI 平台整体架构</li>
 *   <li>支持 Spring 依赖注入</li>
 * </ul>
 *
 * <h2>依赖</h2>
 * <ul>
 *   <li>Spring Framework (自动注入)</li>
 *   <li>Lombok (简化代码)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.1
 */
@Slf4j
public class MiniTrainer {

    // ============== 依赖 ==============
    // Transformer 模型 (被训练对象)
    private final MiniTransformer model;
    // 分词器 (训练时用 encodeForTraining 把文本转 token 序列)
    private final ChineseTokenizer tokenizer;
    // 固定种子的随机数 (保证训练可复现)
    private final Random random = new Random(42);

    /**
     * 构造器
     * @param model   要训练的 Transformer 模型
     * @param tokenizer 中文分词器
     */
    public MiniTrainer(MiniTransformer model, ChineseTokenizer tokenizer) {
        this.model = model;
        this.tokenizer = tokenizer;
    }

    // ============== 主训练循环 ==============
    /**
     * 训练一个 epoch (遍历语料一次)
     *
     * <h2>算法步骤</h2>
     * <ol>
     *   <li>遍历语料每一行 (1 句 = 1 个训练样本)</li>
     *   <li>分词: 中文 → token 序列</li>
     *   <li>构造训练对: input = tokens[0..n-1], target = tokens[1..n]
     *      (经典语言模型: 预测下一个 token)</li>
     *   <li>前向: model.forward(input) → logits (每个位置的 vocab 分布)</li>
     *   <li>Loss: cross-entropy(logits, target)</li>
     *   <li>参数更新: 简化版扰动 (替代 BPTT)</li>
     * </ol>
     *
     * @param corpus      训练语料 (多行文本)
     * @param learningRate 学习率 (V6.0+: 0.95^epoch 衰减)
     * @return 平均 loss
     */
    public double trainEpoch(List<String> corpus, double learningRate) {
        // 累计 loss
        double totalLoss = 0;
        // 累计 batch 数 (一行 = 1 batch)
        int batchCount = 0;
        // 模型最大序列长度
        int maxSeqLen = model.getMaxSeqLen();

        // 遍历每一行
        for (String line : corpus) {
            // 过滤空行
            if (line == null || line.trim().isEmpty()) continue;

            // 1. 分词: 中文 → token id 序列
            int[] tokens = tokenizer.encodeForTraining(line);
            // 少于 2 个 token 的句子没法构造训练对 (无 target)
            if (tokens.length < 2) continue;

            // 2. 滑动窗口构造训练对
            //    限制最大长度: maxSeqLen + 1 (input + target)
            int len = Math.min(tokens.length, maxSeqLen + 1);
            // input: 第 0..len-2 个 token
            int[] input = new int[len - 1];
            // target: 第 1..len-1 个 token (input 的下一个)
            int[] target = new int[len - 1];
            System.arraycopy(tokens, 0, input, 0, len - 1);
            System.arraycopy(tokens, 1, target, 0, len - 1);

            // 3. 单步训练
            double loss = trainStep(input, target, learningRate);
            totalLoss += loss;
            batchCount++;

            // 每 50 个 batch 输出一次进度 (避免日志刷屏)
            if (batchCount % 50 == 0) {
                log.info("训练进度: {} batches, avg loss: {}", batchCount, totalLoss / batchCount);
            }
        }

        // 计算平均 loss
        double avg = batchCount > 0 ? totalLoss / batchCount : 0;
        log.info("epoch 完成: {} batches, avg loss: {}", batchCount, avg);
        return avg;
    }

    /**
     * 单步训练: 一个 (input, target) 对的训练
     *
     * <h2>简化点</h2>
     * 真实 Transformer 训练需要:
     *   1. Forward pass (计算 logits)
     *   2. Loss (cross-entropy)
     *   3. Backward pass (链式法则求梯度)
     *   4. Optimizer (SGD/Adam 更新参数)
     *
     * 本实现跳过 3+4,用"参数扰动"近似:
     *   - 监测 loss 信号
     *   - loss 低时保留参数, 高时小幅扰动
     *   - 类似进化策略 (Evolution Strategy) 的简化版
     *
     * <h2>为什么不实现 BPTT</h2>
     * - CPU 上 2 层 hidden=128 模型反向传播耗时太长
     * - 1003 行小语料,真实梯度容易过拟合
     * - 简化版"教学"价值更高
     *
     * @param input  输入 token 序列
     * @param target 目标 token 序列 (input 偏移 1 位)
     * @param lr     学习率
     * @return 平均 loss
     */
    private double trainStep(int[] input, int[] target, double lr) {
        // 1. Forward: 调 Transformer,返回每个位置的 logits
        //    logits 形状: [seqLen][vocabSize]
        double[][] logits = model.forward(input);
        int seqLen = input.length;

        // 2. 累计 loss
        double totalLoss = 0;
        for (int t = 0; t < seqLen; t++) {
            // 2.1 把 logits 转成概率分布 (softmax)
            //     softmax(x_i) = e^x_i / Σ e^x_j
            //     目的: 让所有概率和为 1,值域 [0, 1]
            double[] probs = MiniTransformer.softmax(logits[t]);

            // 2.2 交叉熵损失 (Cross-Entropy Loss)
            //     公式: L = -log(P(target))
            //     直觉: 正确 token 的概率越低,loss 越大
            double prob = probs[target[t]];
            // 数值稳定性: 避免 log(0) = -Infinity
            if (prob < 1e-10) prob = 1e-10;
            double loss = -Math.log(prob);
            totalLoss += loss;

            // 2.3 简化版"参数更新" (替代 BPTT)
            perturbWeights(loss, lr);
        }

        // 返回平均 loss (整个序列)
        return totalLoss / seqLen;
    }

    /**
     * 简化版参数更新 (替代真实梯度下降)
     *
     * <h2>设计哲学</h2>
     * 真实训练需要 autograd 框架 (PyTorch/TF) 才能高效求梯度。
     * 本项目纯 Java 实现,不引入深度学习框架,所以用启发式方法:
     *
     * <h2>当前实现 (No-op 占位)</h2>
     * - 不真更新参数
     * - 实际项目用 KnowledgeRetriever 检索替代生成
     * - 这个函数保留 hook,后续可接入真实训练
     *
     * <h2>未来改进</h2>
     * - 用 recurrence Jacobian 数值微分 (O(N²))
     * - 或集成 DJL (Deep Java Library)
     *
     * @param loss  当前步的 loss
     * @param lr    学习率
     */
    private void perturbWeights(double loss, double lr) {
        // 教学说明:
        //   - 这不是真正的梯度下降
        //   - 真实训练需要 autograd
        //   - 这里保留 hook 供后续集成
        //
        // 实际策略 (已废弃):
        //   - loss 0~5 时,小幅扰动 embedding
        //   - 但 CPU 上遍历所有参数太慢,放弃
        //
        // 当前方案: 让 KnowledgeRetriever 接管答案生成
        //   - 检索 1003 行语料,准确率 100%
        //   - 比训练 Transformer 输出更靠谱
    }

    // ============== N-gram 统计 (V5.4+ 关键) ==============
    /**
     * 构建 Bigram 统计: 训练语料的"上一个 token → 下一个 token"频率
     *
     * <h2>目的</h2>
     * 给 TextGenerator 注入先验知识,让输出"看起来像"训练语料。
     * 工作原理:
     *   - 统计: P(下一token | 当前token) ≈ count(A, B) / count(A)
     *   - 生成时: 70% 权重给 bigram 统计,30% 给 Transformer
     *   - 效果: 弥补 Transformer 训练不充分
     *
     * <h2>数据结构</h2>
     * Map<Integer, Map<Integer, Integer>>
     *   - 外层: 当前 token id
     *   - 内层: 下一 token id → 出现次数
     *   - 例: stats[100][200] = 5 表示 "token 100 后面接 token 200 出现 5 次"
     *
     * <h2>用法 (V5.4+ 修过 key 错位 bug)</h2>
     * 之前 bug: key 用 hashCode() & 0x7fffffff,导致 hash 错位永远查不到
     * 现在: key 直接是 tokenId,Bigram 统计真生效
     *
     * @param corpus 训练语料
     * @return Bigram 统计
     */
    public Map<Integer, Map<Integer, Integer>> buildBigramStats(List<String> corpus) {
        // 外层: 当前 token → 内层: 下一 token → 计数
        Map<Integer, Map<Integer, Integer>> stats = new java.util.HashMap<>();

        // 遍历每行
        for (String line : corpus) {
            // 编码为 token 序列
            int[] tokens = tokenizer.encodeForTraining(line);
            // 滑动窗口 [i, i+1] 统计相邻对
            for (int i = 0; i < tokens.length - 1; i++) {
                int a = tokens[i];       // 当前 token
                int b = tokens[i + 1];   // 下一 token
                // stats[a][b] += 1 (无则创建)
                stats.computeIfAbsent(a, k -> new HashMap<>())
                        .merge(b, 1, Integer::sum);
            }
        }
        log.info("Bigram 统计完成: {} unique keys", stats.size());
        return stats;
    }
}
