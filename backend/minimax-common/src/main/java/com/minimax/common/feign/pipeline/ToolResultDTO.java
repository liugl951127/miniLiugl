package com.minimax.common.feign.pipeline;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工具执行结果 DTO（pipeline → agent）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolResultDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private boolean ok;
    private String result;
    private Integer durationMs;

    public static ToolResultDTO ok(String r, int d) {
        return new ToolResultDTO(true, r, d);
    }

    public static ToolResultDTO error(String r) {
        return new ToolResultDTO(false, r, 0);
    }
}
