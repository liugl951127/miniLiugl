package com.minimax.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.ai.entity.PushSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * PushSubscription Mapper (V3.3.1)
 */
@Mapper
public interface PushSubscriptionMapper extends BaseMapper<PushSubscription> {

    /** 按 userId 查 */
    @Select("SELECT * FROM push_subscription WHERE userId = #{userId} AND status = 'ACTIVE'")
    List<PushSubscription> findByUser(@Param("userId") Long userId);

    /** 按 platform 查 */
    @Select("SELECT * FROM push_subscription WHERE platform = #{platform} AND status = 'ACTIVE'")
    List<PushSubscription> findByPlatform(@Param("platform") String platform);

    /** 所有 ACTIVE */
    @Select("SELECT * FROM push_subscription WHERE status = 'ACTIVE'")
    List<PushSubscription> findAllActive();

    /** 按 endpoint 查 (去重) */
    @Select("SELECT * FROM push_subscription WHERE endpoint = #{endpoint} LIMIT 1")
    PushSubscription findByEndpoint(@Param("endpoint") String endpoint);

    /** 标过期 */
    @Update("UPDATE push_subscription SET status = 'EXPIRED', updatedAt = NOW() WHERE id = #{id}")
    int markExpired(@Param("id") Long id);

    /** 取消订阅 */
    @Update("UPDATE push_subscription SET status = 'UNSUBSCRIBED', updatedAt = NOW() WHERE subscriptionId = #{subscriptionId}")
    int unsubscribe(@Param("subscriptionId") String subscriptionId);

    /** 计数 (按 platform) */
    @Select("SELECT platform, COUNT(*) AS cnt FROM push_subscription WHERE status = 'ACTIVE' GROUP BY platform")
    List<java.util.Map<String, Object>> countByPlatform();
}
