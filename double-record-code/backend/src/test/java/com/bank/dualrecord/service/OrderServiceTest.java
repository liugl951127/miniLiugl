package com.bank.dualrecord.service;

import com.bank.dualrecord.exception.BusinessException;
import com.bank.dualrecord.fabric.FabricEvidenceService;
import com.bank.dualrecord.mapper.OrderMapper;
import com.bank.dualrecord.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private FabricEvidenceService fabricEvidenceService;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateOrder_Success() {
        Order input = new Order();
        input.setCustomerId(1001L);
        input.setProductId(2001L);
        input.setProductType(1);
        input.setProductName("XX 寿险");
        input.setAmount(new BigDecimal("100000.00"));
        input.setChannel(1);

        when(orderMapper.insert(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setOrderId(1L);
            return 1;
        });

        Order result = orderService.createOrder(input);
        assertNotNull(result.getOrderNo());
        assertEquals(0, result.getState().intValue());
        assertEquals(1L, result.getOrderId());
        verify(orderMapper, times(1)).insert(input);
    }

    @Test
    void testGetById_NotFound() {
        when(orderMapper.selectById(999L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.getById(999L));
        assertEquals(404, ex.getCode());
    }

    @Test
    void testGetById_Found() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderNo("ORD-001");
        when(orderMapper.selectById(1L)).thenReturn(order);
        Order result = orderService.getById(1L);
        assertEquals(1L, result.getOrderId());
    }

    @Test
    void testCancel_CompletedOrderFails() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setState(6); // COMPLETED
        when(orderMapper.selectById(1L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> orderService.cancel(1L, "test"));
        assertEquals(400, ex.getCode());
    }

    @Test
    void testCancel_Success() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setState(0);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(orderMapper.updateById(any())).thenReturn(1);

        orderService.cancel(1L, "客户取消");
        assertEquals(-1, order.getState());
        verify(fabricEvidenceService, times(1))
            .updateState(eq("1"), eq("CANCELLED"), eq("客户取消"));
    }

    @Test
    void testSearch_Pagination() {
        Order o = new Order();
        o.setOrderId(1L);
        when(orderMapper.searchOrders(any())).thenReturn(Collections.singletonList(o));
        when(orderMapper.countOrders(any())).thenReturn(1L);

        var result = orderService.search(java.util.Map.of(), 1, 10);
        assertEquals(1, result.getItems().size());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getPage());
    }
}
