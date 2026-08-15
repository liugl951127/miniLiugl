package com.minimax.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.model.entity.ModelQuota;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ModelQuotaMapper extends BaseMapper<ModelQuota> {

    /** 原子增加用量，返回更新后行（用于限流判断）。 */
    int incrementUsage(@Param("userId") Long userId,
                       @Param("modelId") Long modelId,
                       @Param("quotaDate") String quotaDate,
                       @Param("tokens") long tokens,
                       @Param("requests") int requests);
}
