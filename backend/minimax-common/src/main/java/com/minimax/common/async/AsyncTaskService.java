package com.minimax.common.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * 异步任务服务。
 *
 * - 任务用 UUID 标识
 * - 状态: pending / running / done / failed
 * - 支持自定义线程池
 * - 失败重试
 *
 * 用法:
 *   String taskId = asyncTaskService.submit("email", () -> sendEmail());
 *   Status s = asyncTaskService.status(taskId);
 */
@Slf4j
@Component
public class AsyncTaskService {

    private final ConcurrentMap<String, TaskStatus> statuses = new ConcurrentHashMap<>();
    private final ThreadPoolTaskExecutor executor;
    private final int maxRetries;

    public AsyncTaskService() {
        this(4, 16, 100, 3);
    }

    public AsyncTaskService(int core, int max, int queue, int maxRetries) {
        this.maxRetries = maxRetries;
        this.executor = new ThreadPoolTaskExecutor();
        this.executor.setCorePoolSize(core);
        this.executor.setMaxPoolSize(max);
        this.executor.setQueueCapacity(queue);
        this.executor.setThreadNamePrefix("async-task-");
        this.executor.initialize();
    }

    public String submit(String taskType, Runnable task) {
        String taskId = java.util.UUID.randomUUID().toString();
        statuses.put(taskId, new TaskStatus(taskId, taskType, "pending", 0));
        executor.execute(() -> runWithRetry(taskId, task));
        return taskId;
    }

    public <T> String submit(String taskType, Supplier<T> task, AsyncResultHandler<T> handler) {
        String taskId = java.util.UUID.randomUUID().toString();
        statuses.put(taskId, new TaskStatus(taskId, taskType, "pending", 0));
        executor.execute(() -> {
            int attempt = 0;
            Exception lastErr = null;
            while (attempt < maxRetries) {
                attempt++;
                try {
                    T result = task.get();
                    statuses.get(taskId).status = "done";
                    statuses.get(taskId).result = String.valueOf(result);
                    if (handler != null) handler.onSuccess(result);
                    return;
                } catch (Exception e) {
                    lastErr = e;
                    log.warn("task {} attempt {} failed: {}", taskId, attempt, e.getMessage());
                }
            }
            statuses.get(taskId).status = "failed";
            statuses.get(taskId).error = lastErr == null ? "unknown" : lastErr.getMessage();
            if (handler != null) handler.onFailure(lastErr);
        });
        return taskId;
    }

    public <T> CompletableFuture<T> submitFuture(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor);
    }

    @Async
    public CompletableFuture<Void> runAsync(Runnable task) {
        task.run();
        return CompletableFuture.completedFuture(null);
    }

    private void runWithRetry(String taskId, Runnable task) {
        int attempt = 0;
        Exception lastErr = null;
        while (attempt < maxRetries) {
            attempt++;
            statuses.get(taskId).status = "running";
            statuses.get(taskId).attempts = attempt;
            try {
                task.run();
                statuses.get(taskId).status = "done";
                return;
            } catch (Exception e) {
                lastErr = e;
                log.warn("task {} attempt {} failed: {}", taskId, attempt, e.getMessage());
            }
        }
        statuses.get(taskId).status = "failed";
        statuses.get(taskId).error = lastErr == null ? "unknown" : lastErr.getMessage();
    }

    public TaskStatus status(String taskId) {
        return statuses.get(taskId);
    }

    public Map<String, TaskStatus> allStatuses() {
        return new java.util.HashMap<>(statuses);
    }

    public int poolSize() {
        return executor.getPoolSize();
    }

    public int activeCount() {
        return executor.getActiveCount();
    }

    public void clear() {
        statuses.clear();
    }

    public interface AsyncResultHandler<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    public static class TaskStatus {
        public String taskId;
        public String taskType;
        public volatile String status;
        public volatile int attempts;
        public volatile String result;
        public volatile String error;

        public TaskStatus(String id, String type, String st, int at) {
            this.taskId = id;
            this.taskType = type;
            this.status = st;
            this.attempts = at;
        }
    }
}
