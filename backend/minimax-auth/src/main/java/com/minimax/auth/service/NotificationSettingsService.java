package com.minimax.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.auth.entity.NotificationSettings;
import com.minimax.auth.mapper.NotificationSettingsMapper;
import com.minimax.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 通知设置服务 (T1-backend-apis / P0)
 *
 * 2 个端点:
 *   - GET  /api/v1/notification/settings  拿当前用户的设置
 *   - PUT  /api/v1/notification/settings  保存设置
 *
 * 前端对接: views/notification/Index.vue saveSettings()
 *
 * @since V7.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSettingsService {

    private static final Set<String> ALLOWED_CHANNELS = Set.of("email", "sms", "dingtalk", "webhook", "push");
    private static final Set<String> ALLOWED_EVENTS = Set.of("login", "error", "alert", "system");

    private static final String DEFAULT_CHANNELS = "email,webhook";
    private static final String DEFAULT_EVENTS = "login,error,alert,system";
    private static final String DEFAULT_QUIET_START = "22:00";
    private static final String DEFAULT_QUIET_END = "08:00";

    private final NotificationSettingsMapper settingsMapper;

    /**
     * 取当前用户设置, 无则返回默认
     */
    public NotificationSettings get(Long userId) {
        if (userId == null) {
            return defaultSettings(null);
        }
        NotificationSettings s = settingsMapper.selectOne(
                new LambdaQueryWrapper<NotificationSettings>().eq(NotificationSettings::getUserId, userId));
        if (s == null) {
            return defaultSettings(userId);
        }
        return s;
    }

    /**
     * 保存设置 (upsert: 无则插, 有则改)
     */
    @Transactional
    public NotificationSettings save(Long userId,
                                       String channels,
                                       String events,
                                       String quietStart,
                                       String quietEnd) {
        if (userId == null) {
            throw new BizException("未登录: 缺少 X-User-Id");
        }
        validateChannels(channels);
        validateEvents(events);
        validateTime(quietStart, "quietStart");
        validateTime(quietEnd, "quietEnd");

        NotificationSettings exist = settingsMapper.selectOne(
                new LambdaQueryWrapper<NotificationSettings>().eq(NotificationSettings::getUserId, userId));
        if (exist == null) {
            exist = new NotificationSettings();
            exist.setUserId(userId);
            exist.setChannels(channels);
            exist.setEvents(events);
            exist.setQuietStart(quietStart);
            exist.setQuietEnd(quietEnd);
            settingsMapper.insert(exist);
            log.info("[NotificationSettings] created for userId={}", userId);
        } else {
            exist.setChannels(channels);
            exist.setEvents(events);
            exist.setQuietStart(quietStart);
            exist.setQuietEnd(quietEnd);
            settingsMapper.updateById(exist);
            log.info("[NotificationSettings] updated for userId={}", userId);
        }
        return exist;
    }

    private NotificationSettings defaultSettings(Long userId) {
        NotificationSettings s = new NotificationSettings();
        s.setUserId(userId);
        s.setChannels(DEFAULT_CHANNELS);
        s.setEvents(DEFAULT_EVENTS);
        s.setQuietStart(DEFAULT_QUIET_START);
        s.setQuietEnd(DEFAULT_QUIET_END);
        return s;
    }

    private void validateChannels(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new BizException("channels 不能为空");
        }
        for (String c : csv.split(",")) {
            if (!ALLOWED_CHANNELS.contains(c.trim())) {
                throw new BizException("不支持的通知渠道: " + c + " (允许: " + ALLOWED_CHANNELS + ")");
            }
        }
    }

    private void validateEvents(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new BizException("events 不能为空");
        }
        for (String e : csv.split(",")) {
            if (!ALLOWED_EVENTS.contains(e.trim())) {
                throw new BizException("不支持的通知事件: " + e + " (允许: " + ALLOWED_EVENTS + ")");
            }
        }
    }

    private void validateTime(String hhmm, String field) {
        if (hhmm == null || !hhmm.matches("^\\d{2}:\\d{2}$")) {
            throw new BizException(field + " 格式错误, 期望 HH:mm, 实际: " + hhmm);
        }
    }
}
