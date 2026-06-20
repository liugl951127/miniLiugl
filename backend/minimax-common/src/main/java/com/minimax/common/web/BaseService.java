package com.minimax.common.web;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 通用 Service 基类 (V5.4 冗余重构).
 *
 * 继承 MyBatis-Plus ServiceImpl, 自动获得:
 *   - save / saveBatch
 *   - removeById / removeByIds
 *   - updateById / updateBatchById
 *   - getById / listByIds
 *   - list / page
 *   - count
 *
 * 子类只需:
 * <pre>
 * {@code
 * @Service
 * public class UserService extends BaseService<UserMapper, User> {
 *     // 业务方法
 *     public User findByUsername(String username) { ... }
 * }
 * }
 * </pre>
 *
 * @param <M> Mapper 类型 (必须继承 BaseMapper<T>)
 * @param <T> 实体类型
 */
public abstract class BaseService<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> {
    // 后续可加通用方法: 软删除校验, 审计字段填充, 租户隔离等
}