package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.BillingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * BillingRecordMapper (V6.1 详细注释版)
 *
 * <h2>职责</h2>
 * MyBatis Mapper - BillingRecordMapper.java
 *
 * <h2>核心功能</h2>
 * <ul>
 *   <li>提供 BillingRecordMapper 的业务能力</li>
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
public interface BillingRecordMapper extends BaseMapper<BillingRecord> {

    @Select("SELECT * FROM billing_record WHERE user_id = #{user_id} ORDER BY created_at DESC LIMIT #{limit}")
    List<BillingRecord> findByUser(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT COALESCE(SUM(amount_cents), 0) FROM billing_record WHERE user_id = #{user_id} AND status = 'SUCCESS'")
    Long sumByUser(@Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(amount_cents), 0) FROM billing_record WHERE user_id = #{user_id} AND record_type = 'USAGE' AND status = 'SUCCESS'")
    Long sumUsageByUser(@Param("userId") Long userId);
}
