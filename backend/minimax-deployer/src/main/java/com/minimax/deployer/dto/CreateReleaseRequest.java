package com.minimax.deployer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 创建 release 请求 DTO (V2.0)
 *
 * 包含完整的智能体定义 + 部署配置。
 */
@Data
public class CreateReleaseRequest {

    /** 所属项目 ID */
    @NotBlank
    private Long projectId;

    /** 语义化版本号 (格式: X.Y.Z) */
    @NotBlank
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", message = "版本号格式错误, 示例: 1.0.0")
    private String version;

    /** 发布标题 */
    private String title;

    /** 变更日志 (Markdown) */
    private String changelog;

    /** 智能体定义 (designer 输出) */
    private List<Map<String, Object>> agents;

    /** 连接关系 (拓扑) */
    private List<Map<String, Object>> connections;

    /** 部署配置 */
    private Map<String, Object> deployConfig;

    /** V4.0: 工作流步骤 */
    private List<Map<String, Object>> workflow;
}
