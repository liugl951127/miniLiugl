package com.minimax.auth.service;

import com.minimax.auth.entity.UserPreference;

/**
 * 用户偏好 Service 接口 (V6.8.9)
 */
public interface UserPreferenceService {

    /**
     * 获取用户偏好（不存在则创建默认）
     */
    UserPreference getOrCreate(Long userId);

    /**
     * 更新主题
     */
    UserPreference updateTheme(Long userId, String theme);
}
