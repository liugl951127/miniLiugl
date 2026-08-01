package com.bank.dualrecord.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bank.dualrecord.dto.PageResult;
import com.bank.dualrecord.exception.BusinessException;
import com.bank.dualrecord.fabric.FabricEvidenceService;
import com.bank.dualrecord.mapper.OrderMapper;
import com.bank.dualrecord.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 订单 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService implements IService<Order> {

    private final OrderMapper orderMapper;
    private final FabricEvidenceService fabricEvidenceService;

    @Override
    public OrderMapper getBaseMapper() {
        return orderMapper;
    }

    /**
     * 创建订单
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Order order) {
        if (order.getOrderNo() == null) {
            order.setOrderNo(generateOrderNo());
        }
        if (order.getState() == null) {
            order.setState(0);
        }
        orderMapper.insert(order);
        log.info("订单创建: orderId={}, orderNo={}", order.getOrderId(), order.getOrderNo());
        return order;
    }

    /**
     * 查询(带关联)
     */
    public Order getById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        return order;
    }

    /**
     * 条件分页
     */
    public PageResult<Order> search(Map<String, Object> query, int page, int size) {
        int offset = (page - 1) * size;
        Map<String, Object> params = Map.of(
            "query", query,
            "offset", offset,
            "size", size
        );
        List<Order> items = orderMapper.searchOrders(params);
        long total = orderMapper.countOrders(params);
        long totalPages = (total + size - 1) / size;
        return new PageResult<>(items, total, page, size, totalPages);
    }

    /**
     * 取消订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long orderId, String reason) {
        Order order = getById(orderId);
        if (order.getState() == 6) {
            throw new BusinessException(400, "订单已完成,无法取消");
        }
        order.setState(-1);
        orderMapper.updateById(order);

        // 同步到链上
        try {
            fabricEvidenceService.updateState(String.valueOf(orderId), "CANCELLED", reason);
        } catch (Exception e) {
            log.warn("链上状态同步失败(订单业务已取消): {}", e.getMessage());
        }
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis();
    }
}
