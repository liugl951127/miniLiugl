package com.bank.dualrecord.controller;

import com.bank.dualrecord.dto.ApiResponse;
import com.bank.dualrecord.dto.PageResult;
import com.bank.dualrecord.model.Order;
import com.bank.dualrecord.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 订单管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "订单管理", description = "双录业务订单 CRUD")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "创建订单")
    public ApiResponse<Order> create(@RequestBody Order order) {
        return ApiResponse.ok(orderService.createOrder(order));
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情")
    public ApiResponse<Order> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.getById(id));
    }

    @GetMapping
    @Operation(summary = "订单分页查询")
    public ApiResponse<PageResult<Order>> list(
        @RequestParam(required = false) Long customerId,
        @RequestParam(required = false) Long salesUserId,
        @RequestParam(required = false) Long branchId,
        @RequestParam(required = false) Integer state,
        @RequestParam(required = false) Integer productType,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Map<String, Object> query = Map.of(
            "customerId", customerId == null ? "" : customerId,
            "salesUserId", salesUserId == null ? "" : salesUserId,
            "branchId", branchId == null ? "" : branchId,
            "state", state == null ? "" : state,
            "productType", productType == null ? "" : productType,
            "startDate", startDate == null ? "" : startDate,
            "endDate", endDate == null ? "" : endDate
        );
        return ApiResponse.ok(orderService.search(query, page, size));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消订单")
    public ApiResponse<Void> cancel(@PathVariable Long id, @RequestParam String reason) {
        orderService.cancel(id, reason);
        return ApiResponse.ok();
    }
}
