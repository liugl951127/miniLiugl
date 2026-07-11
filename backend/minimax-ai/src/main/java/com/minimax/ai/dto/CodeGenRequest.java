package com.minimax.ai.dto;

import lombok.Data;

/**
 * 代码生成请求
 */
@Data
public class CodeGenRequest {
    /** 项目类型: spring-boot / vue / react / python-flask / node-express / html */
    private String projectType;
    /** 项目名称 (英文, 用于包名) */
    private String projectName;
    /** 项目描述 (中文, AI 用来生成业务代码) */
    private String description;
    /** 功能列表 (逗号分隔) */
    private String features;
    /** 数据库: mysql / postgresql / h2 / none */
    private String database;
    /** 是否包含单元测试 */
    private Boolean includeTests;
    /** 包名 (com.example.xxx) */
    private String packageName;
}
