package com.minimax.model.quota;

import com.minimax.model.mapper.ModelQuotaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class QuotaService {

    private final ModelQuotaMapper quotaMapper;

    /**
     * 记录一次使用。如果超限，返回 false。
     */
    public boolean record(Long userId, Long modelId, long tokens) {
        String today = LocalDate.now().toString();
        quotaMapper.incrementUsage(userId, modelId, today, tokens, 1);
        // 简化：每次写都返回 true（不强制拦截）
        // 实际可在此处 SELECT 当前用量判断是否超 limit
        return true;
    }
}
