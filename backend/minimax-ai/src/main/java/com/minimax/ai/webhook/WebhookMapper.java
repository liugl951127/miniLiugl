package com.minimax.ai.webhook;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WebhookMapper extends BaseMapper<Webhook> {

    @Update("UPDATE webhook SET deliveryCount = deliveryCount + 1, " +
            "successCount = successCount + #{successDelta}, " +
            "failCount = failCount + #{failDelta}, " +
            "lastDeliveryAt = NOW(), lastStatus = #{status} " +
            "WHERE webhookId = #{webhookId}")
    int updateDeliveryStats(@Param("webhookId") String webhookId,
                            @Param("successDelta") int successDelta,
                            @Param("failDelta") int failDelta,
                            @Param("status") int status);
}
