package com.minimax.ai.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 代码生成响应
 */
@Data
public class CodeGenResponse {
    /** 生成的项目名 */
    private String projectName;
    /** 项目类型 */
    private String projectType;
    /** 完整目录结构 */
    private String structure;
    /** 文件列表 (path -> content) */
    private Map<String, String> files;
    /** 关键文件清单 (用于快速预览) */
    private List<String> keyFiles;
    /** 启动说明 */
    private String runInstructions;
    /** 耗时 ms */
    private long durationMs;
    /** 总文件数 */
    private int totalFiles;
    /** 总行数 */
    private int totalLines;
}
