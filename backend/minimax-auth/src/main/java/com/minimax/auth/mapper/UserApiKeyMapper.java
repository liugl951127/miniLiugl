package com.minimax.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.auth.entity.UserApiKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserApiKeyMapper extends BaseMapper<UserApiKey> {

    /** 通过 userId 列出 API Key (XML mapper) */
    List<UserApiKey> selectByUserId(@Param("userId") Long userId);

    /** 通过 keyHash 查单个 API Key (XML mapper) */
    UserApiKey selectByKeyHash(@Param("keyHash") String keyHash);

    /** 自增 use_count (XML mapper) */
    void incrementUseCount(@Param("id") Long id);

    /** 全量聚合统计 */
    @Select("SELECT COUNT(*) AS total_keys, SUM(use_count) AS total_calls FROM user_api_key WHERE deleted = 0")
    java.util.Map<String, Object> selectStats();

    /** 按 enabled 统计 */
    @Select("SELECT enabled, COUNT(*) AS cnt FROM user_api_key WHERE deleted = 0 GROUP BY enabled")
    java.util.List<java.util.Map<String, Object>> selectCountByStatus();
}
