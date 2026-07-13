package com.minimax.ai.distribute.spark;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Task 执行结果 (V3.5.4)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResult {
    private boolean success;
    private Object result;
    private long durationMs;
    private String workerId;
    private String errorMessage;

    public static TaskResult success(Object result, long durationMs) {
        return TaskResult.builder()
                .success(true)
                .result(result)
                .durationMs(durationMs)
                .build();
    }

    public static TaskResult failed(String error) {
        return TaskResult.builder()
                .success(false)
                .errorMessage(error)
                .build();
    }
}
