package com.minimax.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.admin.entity.AdminAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminAuditLogMapper extends BaseMapper<AdminAuditLog> {

    List<AdminAuditLog> selectByActor(@Param("actorId") Long actorId,
                                       @Param("limit") int limit);

    List<AdminAuditLog> selectRecent(@Param("limit") int limit);

    /** 按 action 统计 - MySQL/H2 兼容 */
    List<Map<String, Object>> countByAction(@Param("since") String since);

    /** 按 resource_type 统计 */
    List<Map<String, Object>> countByResourceType(@Param("since") String since);
}
