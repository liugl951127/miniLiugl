package com.minimax.ai.push.integration;

/**
 * 推送 Provider 统一接口 (V3.5.1 真实集成)
 *
 * <h3>3 个实现</h3>
 * <ul>
 *   <li>{@link WebPushProvider} - Web Push (VAPID/ECDSA + HTTP POST)</li>
 *   <li>{@link ApnsProvider} - Apple APNs (HTTP/2 + JWT ES256)</li>
 *   <li>{@link FcmProvider} - Firebase FCM (HTTP v1 API + OAuth2)</li>
 * </ul>
 */
public interface PushProvider {

    /**
     * 推送平台标识
     */
    PushResult.Platform platform();

    /**
     * 推送一条消息
     */
    PushResult push(PushRequest request);

    /**
     * 健康检查 (e.g. 验证 token / 端点可达)
     */
    default boolean healthy() {
        return true;
    }
}
