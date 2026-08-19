package com.minimax.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.minimax.auth.entity.UserPreference;
import com.minimax.auth.mapper.UserPreferenceMapper;
import com.minimax.auth.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户偏好 Service 实现 (V6.8.9)
 */
@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final UserPreferenceMapper preferenceMapper;

    @Override
    public UserPreference getOrCreate(Long userId) {
        UserPreference pref = preferenceMapper.selectOne(
            new LambdaQueryWrapper<UserPreference>().eq(UserPreference::getUserId, userId)
        );
        if (pref == null) {
            pref = new UserPreference();
            pref.setUserId(userId);
            pref.setTheme("light");
            pref.setLanguage("zh-CN");
            preferenceMapper.insert(pref);
        }
        return pref;
    }

    @Override
    public UserPreference updateTheme(Long userId, String theme) {
        UserPreference pref = getOrCreate(userId);
        if (!"light".equals(theme) && !"dark".equals(theme)) {
            theme = "light";
        }
        pref.setTheme(theme);
        preferenceMapper.updateById(pref);
        return pref;
    }
}
