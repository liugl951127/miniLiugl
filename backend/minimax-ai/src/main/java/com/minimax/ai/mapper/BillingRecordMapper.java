package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.BillingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BillingRecordMapper extends BaseMapper<BillingRecord> {

    @Select("SELECT * FROM billing_record WHERE user_id = #{user_id} ORDER BY created_at DESC LIMIT #{limit}")
    List<BillingRecord> findByUser(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT COALESCE(SUM(amount_cents), 0) FROM billing_record WHERE user_id = #{user_id} AND status = 'SUCCESS'")
    Long sumByUser(@Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(amount_cents), 0) FROM billing_record WHERE user_id = #{user_id} AND record_type = 'USAGE' AND status = 'SUCCESS'")
    Long sumUsageByUser(@Param("userId") Long userId);
}
