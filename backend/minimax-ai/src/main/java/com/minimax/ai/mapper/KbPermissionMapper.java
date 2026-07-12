package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.KbPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KbPermissionMapper extends BaseMapper<KbPermission> {

    @Select("SELECT * FROM kb_permission WHERE kbId = #{kbId}")
    List<KbPermission> findByKb(@Param("kbId") String kbId);

    @Select("SELECT * FROM kb_permission WHERE kbId = #{kbId} AND subjectType = #{subjectType} " +
            "AND subjectId = #{subjectId} LIMIT 1")
    KbPermission findOne(@Param("kbId") String kbId,
                         @Param("subjectType") String subjectType,
                         @Param("subjectId") Long subjectId);
}
