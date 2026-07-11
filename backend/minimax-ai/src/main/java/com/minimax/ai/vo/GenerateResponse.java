package com.minimax.ai.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 文本生成响应
 */
@Data
@AllArgsConstructor
public class GenerateResponse {
    /** 输入提示 */
    private String prompt;
    /** 生成结果 */
    private String text;
    /** 生成 token 数 */
    private int tokens;
    /** 推理耗时 (ms) */
    private long durationMs;
    /** 模型信息 */
    private String model;
    /** 是否来自自研模型 (V2.5 true) */
    private boolean selfDeveloped;
}
