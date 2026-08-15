package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.PushMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * PushMessage Mapper (V3.3.1)
 */
@Mapper
public interface PushMessageMapper extends BaseMapper<PushMessage> {

    /** 按 userId 查 (需要 join) — 简化: 查全部最近 N 条 */
    @Select("SELECT * FROM push_message ORDER BY created_at DESC LIMIT #{limit}")
    List<PushMessage> findRecent(@Param("limit") int limit);

    /** 按 status 查 */
    @Select("SELECT * FROM push_message WHERE status = #{status} ORDER BY created_at DESC")
    List<PushMessage> findByStatus(@Param("status") String status);
}
