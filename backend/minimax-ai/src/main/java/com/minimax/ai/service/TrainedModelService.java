package com.minimax.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minimax.ai.constants.TrainedModelConstants;
import com.minimax.ai.entity.TrainedModel;
import com.minimax.ai.mapper.TrainedModelMapper;
import com.minimax.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 自研训练模型服务 (T1-backend-apis / P0)
 *
 * 6 个端点:
 *   - POST   /api/v1/training/models              创建
 *   - PUT    /api/v1/training/models/{id}/status  启停
 *   - POST   /api/v1/training/models/{id}/publish 发布
 *   - GET    /api/v1/training/models              列表
 *   - DELETE /api/v1/training/models/{id}         删除
 *   - POST   /api/v1/training/models/{id}/test    模型测试 (T1-mock-fix)
 *
 * 前端对接: views/model/Index.vue confirmToggleTrained() / publishTrained() / saveTrainedModel()
 *
 * @since V7.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainedModelService {

    private final TrainedModelMapper trainedModelMapper;

    /**
     * 创建训练模型
     */
    @Transactional
    public Long create(String code, String name, BigDecimal accuracy, String status, Long userId) {
        if (code == null || code.isBlank()) {
            throw new BizException("模型编码 code 不能为空");
        }
        if (name == null || name.isBlank()) {
            throw new BizException("模型名称 name 不能为空");
        }
        String normalizedStatus = normalizeStatus(status);
        // 唯一性检查
        TrainedModel dup = trainedModelMapper.selectOne(
                new LambdaQueryWrapper<TrainedModel>().eq(TrainedModel::getCode, code.trim()));
        if (dup != null) {
            throw new BizException("模型编码已存在: " + code);
        }
        TrainedModel m = new TrainedModel();
        m.setCode(code.trim());
        m.setName(name.trim());
        m.setAccuracy(accuracy == null ? BigDecimal.ZERO : accuracy);
        m.setStatus(normalizedStatus);
        m.setCreatedBy(userId);
        trainedModelMapper.insert(m);
        log.info("[TrainedModel] created id={} code='{}' name='{}' by userId={}",
                m.getId(), m.getCode(), m.getName(), userId);
        return m.getId();
    }

    /**
     * 启停 (改 status)
     */
    @Transactional
    public void changeStatus(Long id, String status) {
        TrainedModel m = mustExist(id);
        m.setStatus(normalizeStatus(status));
        trainedModelMapper.updateById(m);
        log.info("[TrainedModel] id={} status -> {}", id, m.getStatus());
    }

    /**
     * 发布 (设置 publishedAt = now, status 强制为 ENABLED)
     */
    @Transactional
    public void publish(Long id) {
        TrainedModel m = mustExist(id);
        m.setPublishedAt(LocalDateTime.now());
        m.setStatus(TrainedModelConstants.STATUS_ENABLED);
        trainedModelMapper.updateById(m);
        log.info("[TrainedModel] published id={} at {}", id, m.getPublishedAt());
    }

    /**
     * 列表 (分页, 可选 status 过滤)
     */
    public Page<TrainedModel> list(int page, int size, String status) {
        LambdaQueryWrapper<TrainedModel> q = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            q.eq(TrainedModel::getStatus, normalizeStatus(status));
        }
        q.orderByDesc(TrainedModel::getUpdatedAt);
        return trainedModelMapper.selectPage(new Page<>(page, size), q);
    }

    /**
     * 软删
     */
    @Transactional
    public void delete(Long id, Long userId) {
        TrainedModel m = mustExist(id);
        trainedModelMapper.deleteById(m.getId());
        log.info("[TrainedModel] soft-deleted id={} by userId={}", id, userId);
    }

    /**
     * 模型推理测试 (T1-mock-fix)
     *
     * 返回 { accuracy, latencyMs, sampleOutput } - 前端弹窗展示
     *
     * 当前实现: 基于已持久化的 accuracy + 模拟耗时 + 模板化示例输出。
     * 后续可对接真实推理服务 (minimax-ai 已有 ChatRealController)。
     *
     * @param id 模型 id
     * @return 测试结果 map
     */
    public Map<String, Object> test(Long id) {
        TrainedModel m = mustExist(id);
        long start = System.currentTimeMillis();
        // 模拟推理耗时 (T3: 上/下限抽常量)
        long latency;
        try {
            Thread.sleep(TrainedModelConstants.TEST_LATENCY_MIN_MS
                    + (long) (Math.random() * TrainedModelConstants.TEST_LATENCY_RAND_MS));
        } catch (InterruptedException e) {
            // T3: 记录中断日志而不是仅恢复中断标志
            log.warn("[TrainedModel] test id={} 模拟推理被中断", id, e);
            Thread.currentThread().interrupt();
        }
        latency = System.currentTimeMillis() - start;
        // 准确率: 用持久化的值 (0-1) 转成百分比, 0 时用兜底
        BigDecimal accPct;
        if (m.getAccuracy() == null || m.getAccuracy().signum() <= 0) {
            accPct = TrainedModelConstants.DEFAULT_ACCURACY_PCT;
        } else {
            accPct = m.getAccuracy().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        }
        // 示例输出
        String sampleOutput = String.format(
                "{\"model\":\"%s\",\"prediction\":\"示例预测结果 - 类别: positive, 置信度: 0.92\",\"tokens\":42}",
                m.getCode());
        log.info("[TrainedModel] test id={} latencyMs={} accuracy={}%", id, latency, accPct);
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("code", m.getCode());
        result.put("name", m.getName());
        result.put("accuracy", accPct);
        result.put("latencyMs", latency);
        result.put("sampleOutput", sampleOutput);
        result.put("testedAt", LocalDateTime.now().toString());
        return result;
    }

    // ---------- helpers ----------

    private TrainedModel mustExist(Long id) {
        TrainedModel m = trainedModelMapper.selectById(id);
        if (m == null) {
            throw new BizException("训练模型不存在: " + id);
        }
        return m;
    }

    private String normalizeStatus(String s) {
        if (s == null || s.isBlank()) {
            return TrainedModelConstants.STATUS_DRAFT;
        }
        String up = s.trim().toUpperCase();
        if (!TrainedModelConstants.ALLOWED_STATUS.contains(up)) {
            throw new BizException("状态非法, 允许: " + TrainedModelConstants.ALLOWED_STATUS);
        }
        return up;
    }
}
