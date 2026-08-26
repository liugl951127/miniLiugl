package com.minimax.system.controller;

import com.minimax.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 字典控制器 (V8.0.3.1) — 修致命设计错
 *
 * 之前 V8.0.3 放在 minimax-common, 但:
 *  - common 是 library (packaging=jar), 不是 Spring Boot 应用
 *  - 即使 gateway 扫 com.minimax, common 里 controller 永远不会被启动
 *  - gateway 路由表是 /api/v1/system/**, 前端调 /dict/* 会 404
 *
 * 修: 移到 minimax-system, 前端改成 /system/dict/*
 * 走 gateway 现有路由 /api/v1/system/**, 会命中 system 服务
 */
@Tag(name = "系统-字典")
@RestController
@RequestMapping("/api/v1/system/dict")
public class DictController {

    @GetMapping("/k8s-clusters")
    @Operation(summary = "K8s 集群字典")
    public Result<List<Map<String, String>>> k8sClusters() {
        return Result.ok(List.of(
            Map.of("value", "prod-cluster-01", "label", "prod-cluster-01", "region", "cn-beijing"),
            Map.of("value", "prod-cluster-02", "label", "prod-cluster-02 (新加坡)", "region", "ap-southeast"),
            Map.of("value", "staging-cluster", "label", "staging-cluster", "region", "cn-shanghai")
        ));
    }

    @GetMapping("/agent-roles")
    @Operation(summary = "Agent 角色字典")
    public Result<List<Map<String, String>>> agentRoles() {
        return Result.ok(List.of(
            Map.of("value", "客服", "label", "客服", "color", "linear-gradient(135deg, #6366f1, #8b5cf6)"),
            Map.of("value", "顾问", "label", "顾问", "color", "linear-gradient(135deg, #f59e0b, #ef4444)"),
            Map.of("value", "质检", "label", "质检", "color", "linear-gradient(135deg, #ec4899, #f43f5e)"),
            Map.of("value", "调度", "label", "调度", "color", "linear-gradient(135deg, #10b981, #06b6d4)"),
            Map.of("value", "专业领域", "label", "专业领域", "color", "linear-gradient(135deg, #8b5cf6, #ec4899)")
        ));
    }

    @GetMapping("/alert-channels")
    @Operation(summary = "告警通知渠道字典")
    public Result<List<Map<String, String>>> alertChannels() {
        return Result.ok(List.of(
            Map.of("value", "dingtalk", "label", "钉钉", "icon", "📌"),
            Map.of("value", "feishu", "label", "飞书", "icon", "🚀"),
            Map.of("value", "wechat_work", "label", "企业微信", "icon", "💬"),
            Map.of("value", "email", "label", "邮件", "icon", "✉️"),
            Map.of("value", "webhook", "label", "Webhook", "icon", "🔗"),
            Map.of("value", "sms", "label", "短信", "icon", "📱")
        ));
    }

    @GetMapping("/industries")
    @Operation(summary = "行业类型字典")
    public Result<List<Map<String, String>>> industries() {
        return Result.ok(List.of(
            Map.of("value", "通用", "label", "通用"),
            Map.of("value", "法律", "label", "法律"),
            Map.of("value", "医疗", "label", "医疗"),
            Map.of("value", "金融", "label", "金融"),
            Map.of("value", "代码", "label", "代码"),
            Map.of("value", "教育", "label", "教育"),
            Map.of("value", "电商", "label", "电商")
        ));
    }

    @GetMapping("/kb-strategies")
    @Operation(summary = "知识库检索/分块策略字典")
    public Result<List<Map<String, String>>> kbStrategies() {
        return Result.ok(List.of(
            Map.of("value", "default", "label", "【默认】简洁检索", "category", "retrieval"),
            Map.of("value", "detailed", "label", "【详细】带上下文", "category", "retrieval"),
            Map.of("value", "academic", "label", "【学术】引用文献", "category", "retrieval"),
            Map.of("value", "multi", "label", "【对比】多角度检索", "category", "retrieval"),
            Map.of("value", "auto", "label", "自动（默认）", "category", "chunking"),
            Map.of("value", "fixed", "label", "固定大小", "category", "chunking"),
            Map.of("value", "semantic", "label", "语义切分", "category", "chunking")
        ));
    }

    @GetMapping("/models")
    @Operation(summary = "可用模型字典")
    public Result<List<Map<String, String>>> models() {
        return Result.ok(List.of(
            Map.of("value", "gpt-4o", "label", "GPT-4o", "provider", "openai"),
            Map.of("value", "claude-3.5-sonnet", "label", "Claude-3.5", "provider", "anthropic"),
            Map.of("value", "deepseek-chat", "label", "DeepSeek", "provider", "deepseek"),
            Map.of("value", "qwen2.5-72b-instruct", "label", "Qwen2.5-72B", "provider", "alibaba"),
            Map.of("value", "qwen2.5-0.5b-instruct", "label", "Qwen2.5-0.5B (本地)", "provider", "minimax-ai")
        ));
    }
}
