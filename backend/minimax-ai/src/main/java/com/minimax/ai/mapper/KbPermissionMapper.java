package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.KbPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * KbPermissionMapper (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * MyBatis Mapper - KbPermissionMapper.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 KbPermissionMapper 的业务能力</li>
 *   <li>参与 AI 平台整体架构</li>
 *   <li>支持 Spring 依赖注入</li>
 * </ul>
 *
 * <h2>依赖</h2>
 * <ul>
 *   <li>Spring Framework (自动注入)</li>
 *   <li>Lombok (简化代码)</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.1
 */
@Mapper
public interface KbPermissionMapper extends BaseMapper<KbPermission> {

    @Select("SELECT * FROM kb_permission WHERE kb_id = #{kb_id}")
    List<KbPermission> findByKb(@Param("kbId") String kbId);

    @Select("SELECT * FROM kb_permission WHERE kb_id = #{kb_id} AND subject_type = #{subject_type} " +
            "AND subjectId = #{subjectId} LIMIT 1")
    KbPermission findOne(@Param("kbId") String kbId,
                         @Param("subjectType") String subjectType,
                         @Param("subjectId") Long subjectId);
}
