package com.minimax.ai.training;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 训练实时流 (V3.2.0 SSE)
 *
 * <p>前端订阅, 后端 record() 时推送给所有订阅者
 *
 * <h3>事件类型</h3>
 * <ul>
 *   <li>{@code metric} - 新指标 (epoch/step/loss/valLoss/accuracy)</li>
 *   <li>{@code status} - 状态变化 (PENDING/RUNNING/COMPLETED/FAILED)</li>
 *   <li>{@code checkpoint} - checkpoint 保存</li>
 * </ul>
 *
 * <h3>线程模型</h3>
 * SseEmitter 不线程安全, 用 CopyOnWriteArrayList 存储 emitters
 * record() 时遍历并 try-send (失败时移除)
 */
@Slf4j
@Service
public class TrainingStream {

    /** 事件类型: 指标 */
    public static final String EVT_METRIC = "metric";
    /** 事件类型: 状态 */
    public static final String EVT_STATUS = "status";
    /** 事件类型: checkpoint */
    public static final String EVT_CHECKPOINT = "checkpoint";

    /** taskId -> emitter 列表 (一对多, 多个前端可同时订阅) */
    private final Map<String, List<SseEmitter>> emittersByTask = new ConcurrentHashMap<>();

    /**
     * 订阅 taskId 流
     *
     * @param taskId 任务 ID
     * @return SseEmitter (前端用 EventSource 接)
     */
    public SseEmitter subscribe(String taskId) {
        // 1. 超时 5 分钟 (单次连接最长, 超时后重连)
        SseEmitter emitter = new SseEmitter(300_000L);
        // 2. 注册到列表
        emittersByTask.computeIfAbsent(taskId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        // 3. 完成/超时/错误回调: 移除 emitter
        Runnable cleanup = () -> {
            List<SseEmitter> list = emittersByTask.get(taskId);
            if (list != null) list.remove(emitter);
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError((ex) -> cleanup.run());
        // 4. 立即推一条 hello 消息 (测试连通)
        try {
            emitter.send(SseEmitter.event().name("hello").data(Map.of("taskId", taskId, "ts", System.currentTimeMillis())));
        } catch (IOException e) {
            cleanup.run();
        }
        log.info("[train-stream] 新订阅: taskId={}, 总订阅数={}", taskId, emittersByTask.get(taskId).size());
        return emitter;
    }

    /**
     * 广播指标 (record 时调)
     */
    public void broadcastMetric(String taskId, int epoch, int step, double loss, double valLoss, double accuracy) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("taskId", taskId);
        payload.put("epoch", epoch);
        payload.put("step", step);
        payload.put("loss", loss);
        payload.put("valLoss", valLoss);
        payload.put("accuracy", accuracy);
        payload.put("ts", System.currentTimeMillis());
        send(taskId, EVT_METRIC, payload);
    }

    /**
     * 广播状态
     */
    public void broadcastStatus(String taskId, String status) {
        send(taskId, EVT_STATUS, Map.of("taskId", taskId, "status", status, "ts", System.currentTimeMillis()));
    }

    /**
     * 广播 checkpoint
     */
    public void broadcastCheckpoint(String taskId, String checkpointId, String name, int epoch) {
        send(taskId, EVT_CHECKPOINT, Map.of(
                "taskId", taskId,
                "checkpointId", checkpointId,
                "name", name,
                "epoch", epoch,
                "ts", System.currentTimeMillis()
        ));
    }

    /**
     * 内部发送 (失败时移除)
     */
    private void send(String taskId, String eventName, Object payload) {
        // 1. 复制当前订阅者列表
        List<SseEmitter> list = emittersByTask.get(taskId);
        if (list == null || list.isEmpty()) return;
        // 2. 遍历发送
        for (SseEmitter e : list) {
            try {
                e.send(SseEmitter.event().name(eventName).data(payload));
            } catch (Exception ex) {
                log.debug("[train-stream] 发送失败, 移除 emitter: {}", ex.getMessage());
                list.remove(e);
            }
        }
    }

    /**
     * 当前 taskId 订阅数
     */
    public int subscriberCount(String taskId) {
        List<SseEmitter> list = emittersByTask.get(taskId);
        return list == null ? 0 : list.size();
    }

    /**
     * 关闭 taskId 所有订阅
     */
    public void closeAll(String taskId) {
        List<SseEmitter> list = emittersByTask.remove(taskId);
        if (list != null) {
            for (SseEmitter e : list) {
                try { e.complete(); } catch (Exception ignored) {}
            }
        }
    }

    /** 健康事件 (简单 DTO) */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StreamEvent {
        private String name;
        private Map<String, Object> data;
    }
}
