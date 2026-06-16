package com.minimax.memory.longterm;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LongTermMemoryMapper extends BaseMapper<LongTermMemory> {

    /**
     * 拉取用户最近的 N 条记忆 (不含 embedding, 减少传输)。
     * 用于"列出我的记忆" / 简单浏览。
     */
    List<LongTermMemory> selectRecentByUser(@Param("userId") Long userId,
                                            @Param("limit") int limit);

    /**
     * 拉取用户全部 embedding (含向量 + 原文)。
     * 用于客户端做余弦相似度。
     * 单次建议 limit ≤ 5000 (10MB 以内)。
     */
    List<LongTermMemory> selectEmbeddingsByUser(@Param("userId") Long userId,
                                                @Param("limit") int limit);

    /** 增加被召回次数 + 更新 last_access_at。 */
    int touchAccess(@Param("id") Long id);
}
