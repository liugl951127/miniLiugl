package com.minimax.ai.push.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 推送请求 (V3.5.1 真实集成)
 *
 * <p>3 平台统一结构, 各 Provider 内部转换.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushRequest {
    /** 目标 token (web push endpoint / APNs device token / FCM registration token) */
    private String target;
    /** 通知标题 */
    private String title;
    /** 通知内容 */
    private String body;
    /** 图标 URL (可选) */
    private String icon;
    /** 点击跳转 URL (可选) */
    private String clickAction;
    /** 自定义数据 (key-value) */
    private Map<String, String> data;
    /** 优先级 (HIGH / NORMAL) */
    private Priority priority;
    /** 消息生存时间 (秒) */
    private Integer ttlSeconds;
    /** 主题 (APNs required) */
    private String topic;

    public enum Priority { HIGH, NORMAL }
}
