package com.minimax.ai.distributed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * All-Reduce 梯度同步 (V3.3.3 数据并行核心)
 *
 * <p>把多个 worker 的梯度聚合成一个, 同步给所有 worker
 *
 * <h3>算法</h3>
 * <ul>
 *   <li>SUM         - 求和 (后续除以 N 算平均)</li>
 *   <li>MEAN        - 平均 (等价于 SUM/N)</li>
 *   <li>MAX         - 取最大 (适合分布式置信度集成)</li>
 *   <li>MIN         - 取最小</li>
 * </ul>
 *
 * <h3>算法对比</h3>
 * <table>
 *   <tr><th>算法</th><th>通信量</th><th>延迟</th><th>精度</th></tr>
 *   <tr><td>Naive AllReduce</td><td>O(N·W)</td><td>高</td><td>精确</td></tr>
 *   <tr><td>Ring AllReduce</td><td>O(N·W)</td><td>中</td><td>精确</td></tr>
 *   <tr><td>Tree AllReduce</td><td>O(N·W·log N)</td><td>低</td><td>近似</td></tr>
 *   <tr><td>Parameter Server</td><td>O(N·W)</td><td>高</td><td>精确</td></tr>
 * </table>
 *
 * <h3>复杂度</h3>
 *   O(N × D)  N=worker 数, D=梯度维度
 */
@Slf4j
@Component
public class AllReduce {

    public enum ReduceOp { SUM, MEAN, MAX, MIN }

    /**
     * All-Reduce
     *
     * @param gradients 各 worker 的梯度 (Map<workerId, grad[]>)
     * @param op        归约操作
     * @return 归约后的梯度 (每个 worker 拿到相同结果)
     */
    public double[] allReduce(Map<String, double[]> gradients, ReduceOp op) {
        if (gradients == null || gradients.isEmpty()) return new double[0];
        // 1. 拿第一份确认维度
        double[] first = gradients.values().iterator().next();
        int dim = first.length;
        // 2. 校验所有梯度维度一致
        for (Map.Entry<String, double[]> e : gradients.entrySet()) {
            if (e.getValue().length != dim) {
                throw new IllegalArgumentException("梯度维度不一致: worker=" + e.getKey()
                        + " 期望 " + dim + ", 实际 " + e.getValue().length);
            }
        }
        // 3. 归约
        double[] result = new double[dim];
        for (int i = 0; i < dim; i++) {
            double v = first[i];
            for (Map.Entry<String, double[]> e : gradients.entrySet()) {
                if (e.getValue() == first) continue;  // 跳过第一个 (已经是 v 初始值)
                v = reduce(v, e.getValue()[i], op);
            }
            result[i] = v;
        }
        // 4. MEAN: 求和后除以 N
        if (op == ReduceOp.MEAN) {
            int n = gradients.size();
            for (int i = 0; i < dim; i++) result[i] /= n;
        }
        log.debug("[all-reduce] op={} workers={} dim={}", op, gradients.size(), dim);
        return result;
    }

    /**
     * 二元归约
     */
    private double reduce(double a, double b, ReduceOp op) {
        return switch (op) {
            case SUM, MEAN -> a + b;
            case MAX -> Math.max(a, b);
            case MIN -> Math.min(a, b);
        };
    }

    /**
     * Ring All-Reduce (简化版)
     *
     * <p>原理: 把数据切成 N 段, 每 worker 负责一段, N-1 步后所有 worker 拥有所有段
     * <p>通信量: O(N × D), 延迟: O(N), 与 worker 数线性扩展
     *
     * <p>这里简化为: 把所有梯度都汇总到一个 worker, 然后广播 (中央式)
     * <p>真实 Ring 实现需点对点通信, 略
     */
    public double[] ringAllReduce(double[][] workerGradients, ReduceOp op) {
        if (workerGradients == null || workerGradients.length == 0) return new double[0];
        int nWorkers = workerGradients.length;
        int dim = workerGradients[0].length;
        // 1. 归约到 worker 0
        double[] reduced = new double[dim];
        System.arraycopy(workerGradients[0], 0, reduced, 0, dim);
        for (int w = 1; w < nWorkers; w++) {
            for (int i = 0; i < dim; i++) {
                reduced[i] = reduce(reduced[i], workerGradients[w][i], op);
            }
        }
        // 2. 平均
        if (op == ReduceOp.MEAN) {
            for (int i = 0; i < dim; i++) reduced[i] /= nWorkers;
        }
        return reduced;
    }

    /**
     * 计算 norm (L2)
     */
    public double norm(double[] grad) {
        double sum = 0;
        for (double v : grad) sum += v * v;
        return Math.sqrt(sum);
    }
}
