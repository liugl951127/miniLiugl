package com.minimax.ai.distributed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * All-Reduce 梯度同步 (V3.5.5+ 完整注释版)
 *
 * <h2>背景: 为什么需要 AllReduce?</h2>
 * 数据并行训练时, 每个 worker 都有完整模型副本, 在不同 mini-batch 上算梯度.
 * 训练前需要把这些梯度聚合成"全局梯度", 让所有 worker 用同样的梯度更新模型.
 * 这个"聚合 + 广播"的操作就是 All-Reduce.
 *
 * <pre>
 * Worker-0:  grad_0 = [0.1, 0.2, 0.3]  ─┐
 * Worker-1:  grad_1 = [0.2, 0.3, 0.4]  ─┤── allReduce(MEAN) ──→ [0.15, 0.25, 0.35]
 * Worker-2:  grad_2 = [0.15, 0.25, 0.4] ─┘                     (广播给所有 worker)
 * </pre>
 *
 * <h2>支持 4 种归约操作</h2>
 * <ul>
 *   <li>{@link ReduceOp#SUM}   - 求和 (后续除以 N 算平均)</li>
 *   <li>{@link ReduceOp#MEAN}  - 平均 (等价于 SUM/N, 数据并行标准做法)</li>
 *   <li>{@link ReduceOp#MAX}   - 取最大 (适合分布式置信度集成)</li>
 *   <li>{@link ReduceOp#MIN}   - 取最小</li>
 * </ul>
 *
 * <h2>算法对比</h2>
 * <table border="1">
 *   <tr><th>算法</th><th>通信量</th><th>延迟</th><th>适用规模</th></tr>
 *   <tr><td>Naive (中央式)</td><td>O(N·W)</td><td>高</td><td>小 (&lt; 10 worker)</td></tr>
 *   <tr><td>Ring AllReduce</td><td>O(N·W/N) = O(W)</td><td>中</td><td>中 (10-100)</td></tr>
 *   <tr><td>Tree AllReduce</td><td>O(W·log N)</td><td>低</td><td>大 (100+)</td></tr>
 *   <tr><td>Parameter Server</td><td>O(N·W)</td><td>高</td><td>异步训练</td></tr>
 * </table>
 * (N=worker 数, W=梯度元素数)
 *
 * <h2>本实现</h2>
 * 使用"中央式"简化版, 所有 worker 把梯度发给 master, master 归约后广播.
 * 适合沙箱 / 小规模 (N < 10), 工业级应该用 NCCL / Horovod / BytePS.
 *
 * @author MiniMax
 * @since V3.3.3
 */
@Slf4j  // Lombok: 自动生成 log 字段
@Component  // Spring: 注册为 Bean
public class AllReduce {

    /**
     * 归约操作枚举
     */
    public enum ReduceOp {
        /** 求和 */
        SUM,
        /** 求平均 (SUM/N) */
        MEAN,
        /** 取最大 */
        MAX,
        /** 取最小 */
        MIN
    }

    // ============== 公共 API ==============

    /**
     * All-Reduce 入口 (Map 形式)
     *
     * <p>流程:
     * <ol>
     *   <li>校验所有 worker 的梯度维度一致</li>
     *   <li>逐元素归约 (SUM/MEAN/MAX/MIN)</li>
     *   <li>MEAN 额外除以 N</li>
     *   <li>返归约结果 (所有 worker 拿相同结果, 模拟"广播")</li>
     * </ol>
     *
     * @param gradients 各 worker 的梯度, Map<workerId, grad[]>
     *                  key: worker 标识 (任意字符串)
     *                  value: 该 worker 的梯度向量
     * @param op        归约操作, 见 {@link ReduceOp}
     * @return 归约后的梯度, 所有 worker 拿到相同结果
     *         维度 = 输入梯度维度
     * @throws IllegalArgumentException 如果不同 worker 的梯度维度不一致
     */
    public double[] allReduce(Map<String, double[]> gradients, ReduceOp op) {
        // 入参防御: 空入参返空数组
        if (gradients == null || gradients.isEmpty()) {
            return new double[0];
        }

        // 1. 拿第一份确认维度
        //    iterator() 不保证顺序, 但只需要 length, 顺序无关
        double[] first = gradients.values().iterator().next();
        int dim = first.length;

        // 2. 维度校验: 所有 worker 的梯度必须同维度
        //    否则聚合无意义
        for (Map.Entry<String, double[]> entry : gradients.entrySet()) {
            if (entry.getValue().length != dim) {
                throw new IllegalArgumentException(
                        "梯度维度不一致: worker=" + entry.getKey()
                                + " 期望 " + dim
                                + ", 实际 " + entry.getValue().length
                );
            }
        }

        // 3. 归约: 对每个维度 i, 把所有 worker 在 i 位置的值聚合
        double[] result = new double[dim];
        for (int i = 0; i < dim; i++) {
            // 3.1 用第一个 worker 的值作为初值
            double v = first[i];
            // 3.2 遍历其他 worker 做归约
            for (Map.Entry<String, double[]> entry : gradients.entrySet()) {
                // 跳过第一个 (已经是 v 初始值)
                if (entry.getValue() == first) {
                    continue;
                }
                // 用归约函数把 v 和 entry.value[i] 聚合
                v = reduce(v, entry.getValue()[i], op);
            }
            // 3.3 存到 result
            result[i] = v;
        }

        // 4. MEAN 特殊处理: 求和后除以 N
        //    其他操作 (SUM/MAX/MIN) 已经是终态
        if (op == ReduceOp.MEAN) {
            int n = gradients.size();
            for (int i = 0; i < dim; i++) {
                result[i] /= n;
            }
        }

        log.debug("[all-reduce] op={} workers={} dim={}", op, gradients.size(), dim);
        return result;
    }

    /**
     * 二元归约函数 (内部用)
     *
     * <p>Java 17 switch 表达式, 比传统 switch-case 更简洁
     *
     * @param a  累加值 (running aggregate)
     * @param b  新值
     * @param op 归约操作
     * @return 归约结果
     */
    private double reduce(double a, double b, ReduceOp op) {
        // switch 表达式: 直接返值, 不需要 break
        return switch (op) {
            // SUM 和 MEAN 后续处理: 这里都做加法, MEAN 在外面再除以 N
            case SUM, MEAN -> a + b;
            case MAX -> Math.max(a, b);
            case MIN -> Math.min(a, b);
        };
    }

    /**
     * Ring All-Reduce (简化版, 中央式实现)
     *
     * <p>真正的 Ring All-Reduce:
     * <ul>
     *   <li>把数据切成 N 段, 每个 worker 负责一段</li>
     *   <li>N-1 步 reduce-scatter: 每 worker 把自己段传给下一个</li>
     *   <li>N-1 步 all-gather: 把归约好的段广播给所有 worker</li>
     *   <li>通信量 O(W), 与 worker 数无关 (理想的扩展性)</li>
     * </ul>
     *
     * <p>本实现简化: 中央式 (master 收所有 + 广播), 通信量 O(N·W)
     * 适用于沙箱 (N < 10), 工业级用 NCCL/Ring 库
     *
     * @param workerGradients 所有 worker 的梯度, 二维数组 [nWorkers][dim]
     * @param op              归约操作
     * @return 归约后的梯度 (一维数组, 长度 dim)
     */
    public double[] ringAllReduce(double[][] workerGradients, ReduceOp op) {
        // 入参防御
        if (workerGradients == null || workerGradients.length == 0) {
            return new double[0];
        }

        // 维度信息
        int nWorkers = workerGradients.length;
        int dim = workerGradients[0].length;

        // 1. 归约到 worker 0 (作为 master 临时承担)
        //    拷贝 worker-0 的梯度作为初值
        double[] reduced = new double[dim];
        System.arraycopy(workerGradients[0], 0, reduced, 0, dim);

        // 2. 累加 worker-1 ~ worker-N 的梯度
        for (int w = 1; w < nWorkers; w++) {
            for (int i = 0; i < dim; i++) {
                // 用 reduce 函数累加
                reduced[i] = reduce(reduced[i], workerGradients[w][i], op);
            }
        }

        // 3. MEAN: 累加和除以 N
        if (op == ReduceOp.MEAN) {
            for (int i = 0; i < dim; i++) {
                reduced[i] /= nWorkers;
            }
        }

        // 真实 Ring 还需要 all-gather 步骤, 把 reduced 广播给所有 worker
        // 这里简化: 调用方拿到 reduced 后自行分发
        return reduced;
    }

    /**
     * 计算向量的 L2 范数 (用于梯度裁剪 / 健康检查)
     *
     * <p>公式: ||x||_2 = sqrt(sum(x[i]^2))
     *
     * <p>用途:
     * <ul>
     *   <li>梯度裁剪: 如果 ||grad|| > max_norm, 缩放避免梯度爆炸</li>
     *   <li>健康检查: ||grad|| → 0 表示模型已收敛</li>
     *   <li>监控: ||grad|| 异常大可能数据有问题</li>
     * </ul>
     *
     * @param grad 梯度向量
     * @return L2 范数, 非负
     */
    public double norm(double[] grad) {
        // 1. sum of squares (L2 norm 的平方)
        double sumOfSquares = 0.0;
        for (double v : grad) {
            sumOfSquares += v * v;
        }
        // 2. sqrt 得 L2 norm
        return Math.sqrt(sumOfSquares);
    }
}
