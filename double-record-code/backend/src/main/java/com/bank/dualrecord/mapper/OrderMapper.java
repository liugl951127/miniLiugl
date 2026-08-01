package com.bank.dualrecord.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bank.dualrecord.model.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 动态条件查询(分页)
     */
    @Select({
        "<script>",
        "SELECT o.*, c.name AS customer_name FROM t_order o LEFT JOIN t_customer c ON o.customer_id = c.customer_id",
        "<where>",
        "<if test='query.customerId != null and query.customerId != \"\"'> AND o.customer_id = #{query.customerId} </if>",
        "<if test='query.salesUserId != null and query.salesUserId != \"\"'> AND o.sales_user_id = #{query.salesUserId} </if>",
        "<if test='query.branchId != null and query.branchId != \"\"'> AND o.branch_id = #{query.branchId} </if>",
        "<if test='query.state != null and query.state != \"\"'> AND o.state = #{query.state} </if>",
        "<if test='query.productType != null and query.productType != \"\"'> AND o.product_type = #{query.productType} </if>",
        "<if test='query.startDate != null and query.startDate != \"\"'> AND o.created_at >= #{query.startDate} </if>",
        "<if test='query.endDate != null and query.endDate != \"\"'> AND o.created_at < #{query.endDate} </if>",
        "AND o.deleted_at IS NULL",
        "</where>",
        "ORDER BY o.created_at DESC",
        "LIMIT #{size} OFFSET #{offset}",
        "</script>"
    })
    List<Order> searchOrders(Map<String, Object> params);

    @Select({
        "<script>",
        "SELECT COUNT(*) FROM t_order o",
        "<where>",
        "<if test='query.customerId != null and query.customerId != \"\"'> AND o.customer_id = #{query.customerId} </if>",
        "<if test='query.salesUserId != null and query.salesUserId != \"\"'> AND o.sales_user_id = #{query.salesUserId} </if>",
        "<if test='query.branchId != null and query.branchId != \"\"'> AND o.branch_id = #{query.branchId} </if>",
        "<if test='query.state != null and query.state != \"\"'> AND o.state = #{query.state} </if>",
        "<if test='query.productType != null and query.productType != \"\"'> AND o.product_type = #{query.productType} </if>",
        "<if test='query.startDate != null and query.startDate != \"\"'> AND o.created_at >= #{query.startDate} </if>",
        "<if test='query.endDate != null and query.endDate != \"\"'> AND o.created_at < #{query.endDate} </if>",
        "AND o.deleted_at IS NULL",
        "</where>",
        "</script>"
    })
    long countOrders(Map<String, Object> params);
}
