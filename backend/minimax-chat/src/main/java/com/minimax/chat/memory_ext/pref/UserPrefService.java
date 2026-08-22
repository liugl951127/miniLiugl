package com.minimax.chat.memory_ext.pref;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPrefService {

    private final UserPrefMapper mapper;

    // T2: 加 @Transactional 包裹读+写 (避免并发条件下 2 次 selectOne 之间被覆盖)
    @Transactional
    public void set(Long userId, String key, String value, String source) {
        if (key == null || value == null) return;
        UserPref existing = mapper.selectOne(
                new LambdaQueryWrapper<UserPref>()
                        .eq(UserPref::getUserId, userId)
                        .eq(UserPref::getPrefKey, key)
                        .last("LIMIT 1"));
        if (existing != null) {
            existing.setPrefValue(value);
            existing.setSource(source != null ? source : existing.getSource());
            mapper.updateById(existing);
        } else {
            UserPref p = new UserPref();
            p.setUserId(userId);
            p.setPrefKey(key);
            p.setPrefValue(value);
            p.setSource(source != null ? source : "manual");
            p.setWeight(new java.math.BigDecimal("0.50"));
            mapper.insert(p);
        }
    }

    public String get(Long userId, String key) {
        UserPref p = mapper.selectOne(
                new LambdaQueryWrapper<UserPref>()
                        .eq(UserPref::getUserId, userId)
                        .eq(UserPref::getPrefKey, key)
                        .last("LIMIT 1"));
        return p == null ? null : p.getPrefValue();
    }

    public List<UserPref> listByUser(Long userId) {
        return mapper.selectByUser(userId);
    }

    @Transactional
    public boolean delete(Long userId, String key) {
        UserPref p = mapper.selectOne(
                new LambdaQueryWrapper<UserPref>()
                        .eq(UserPref::getUserId, userId)
                        .eq(UserPref::getPrefKey, key)
                        .last("LIMIT 1"));
        if (p == null) return false;
        mapper.deleteById(p.getId());
        return true;
    }
}
