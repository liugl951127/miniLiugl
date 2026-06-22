package com.minimax.monitor.alert;

import com.minimax.monitor.entity.AlertEvent;

/**
 * 告警通知器接口 (V5.33 Day 18).
 *
 * AlertEngine 在告警触发时调用所有已注册的 notifier。
 * 支持扩展: 邮件 / 钉钉 / Slack / 飞书 / WebHook 等。
 */
public interface AlertNotifier {

    /**
     * 发送告警通知。
     *
     * @param event 告警事件
     * @param channelConfig 渠道配置 JSON (如 {"email":"a@b.com"})
     * @return true 发送成功，false 失败（不阻断其他渠道）
     */
    boolean send(AlertEvent event, String channelConfig);

    /** 渠道类型标识，如 "EMAIL" / "DINGTALK" */
    String channelType();
}
