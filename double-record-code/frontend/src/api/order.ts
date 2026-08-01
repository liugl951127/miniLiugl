/**
 * 订单 API 服务
 */
import { get, post } from '@/utils/request';
import type { Order, CreateOrderDTO, PageResult } from '@/types';

export const orderApi = {
  /**
   * 创建订单
   */
  create(data: CreateOrderDTO): Promise<Order> {
    return post<Order>('/v1/order/create', data);
  },

  /**
   * 查询订单详情
   */
  getById(orderId: number): Promise<Order> {
    return get<Order>(`/v1/order/${orderId}`);
  },

  /**
   * 按订单号查询
   */
  getByNo(orderNo: string): Promise<Order> {
    return get<Order>(`/v1/order/no/${orderNo}`);
  },

  /**
   * 客户订单列表
   */
  listByCustomer(customerId: number, page = 1, size = 10): Promise<PageResult<Order>> {
    return get<PageResult<Order>>(`/v1/order/customer/${customerId}`, { page, size });
  },

  /**
   * 客户经理订单列表
   */
  listBySalesUser(salesUserId: number, status?: number, page = 1, size = 10): Promise<PageResult<Order>> {
    return get<PageResult<Order>>(`/v1/order/sales-user/${salesUserId}`, { status, page, size });
  },

  /**
   * 取消订单
   */
  cancel(orderId: number, reason: string): Promise<void> {
    return post<void>(`/v1/order/${orderId}/cancel`, { reason });
  },

  /**
   * 订单状态推进(状态机流转)
   */
  advanceState(orderId: number, targetState: number, remark?: string): Promise<Order> {
    return post<Order>(`/v1/order/${orderId}/advance`, { targetState, remark });
  },

  /**
   * 订单状态回退(用于异常处理)
   */
  rollbackState(orderId: number, targetState: number, reason: string): Promise<Order> {
    return post<Order>(`/v1/order/${orderId}/rollback`, { targetState, reason });
  },
};
