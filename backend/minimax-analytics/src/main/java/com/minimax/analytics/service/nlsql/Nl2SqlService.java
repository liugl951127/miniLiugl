package com.minimax.analytics.service.nlsql;

import com.minimax.analytics.dto.Nl2SqlRequest;
import com.minimax.analytics.entity.Nl2SqlHistory;
import com.minimax.analytics.vo.Nl2SqlResult;
import com.minimax.analytics.vo.QueryResult;

import java.util.List;

/**
 * NL2SQL 服务接口 (V5.31)
 */
public interface Nl2SqlService {

    /** 自然语言 → SQL (默认: 不自动执行) */
    Nl2SqlResult ask(Long userId, Nl2SqlRequest request);

    /** LLM 解释给定 SQL */
    String explain(Long userId, Long dataSourceId, String sql);

    /** 用户反馈修改后 SQL (训练样本) */
    void feedback(Long userId, Long historyId, String correctedSql, Integer rating);

    /** 历史记录 (分页) */
    List<Nl2SqlHistory> history(Long userId, int page, int size);
}
