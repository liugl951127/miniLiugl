package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.BillingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BillingRecordMapper extends BaseMapper<BillingRecord> {

    @Select("SELECT * FROM billing_record WHERE userId = #{userId} ORDER BY createdAt DESC LIMIT #{limit}")
    List<BillingRecord> findByUser(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT COALESCE(SUM(amountCents), 0) FROM billing_record WHERE userId = #{userId} AND status = 'SUCCESS'")
    Long sumByUser(@Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(amountCents), 0) FROM billing_record WHERE userId = #{userId} AND recordType = 'USAGE' AND status = 'SUCCESS'")
    Long sumUsageByUser(@Param("userId") Long userId);
}
