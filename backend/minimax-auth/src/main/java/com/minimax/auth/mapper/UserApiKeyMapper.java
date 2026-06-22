package com.minimax.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.auth.entity.UserApiKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserApiKeyMapper extends BaseMapper<UserApiKey> {

    @Select("SELECT * FROM user_api_key WHERE user_id = #{userId} AND deleted = 0 ORDER BY created_at DESC")
    List<UserApiKey> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM user_api_key WHERE key_hash = #{keyHash} AND deleted = 0 AND enabled = 1 LIMIT 1")
    UserApiKey selectByKeyHash(@Param("keyHash") String keyHash);

    @Update("UPDATE user_api_key SET use_count = use_count + 1, last_used_at = NOW() WHERE id = #{id}")
    void incrementUseCount(@Param("id") Long id);
}
