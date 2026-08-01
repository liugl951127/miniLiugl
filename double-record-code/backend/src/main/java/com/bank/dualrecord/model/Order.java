package com.bank.dualrecord.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@TableName("t_order")
@Schema(description = "双录订单")
public class Order implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long orderId;

    private String orderNo;
    private Long customerId;

    @TableField(exist = false)
    private String customerName;

    private Long productId;
    private Integer productType;
    private String productName;
    private BigDecimal amount;
    private String currency;
    private Integer state;

    @TableField(exist = false)
    private String stateName;

    private Integer channel;
    private Long salesUserId;
    private Long branchId;
    private String terminalId;
    private String ipAddress;
    private String location;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reserveAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireAt;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Version
    private Integer version;
}
