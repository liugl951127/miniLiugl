package com.minimax.common.web;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.minimax.common.result.Result;
import com.minimax.common.result.ResultCode;

import java.util.List;
import java.util.function.Function;

/**
 * 通用 Controller 基类 (V5.4 冗余重构).
 *
 * 提供 5 个标准 REST 接口的默认实现:
 *   - GET    /page       分页查询
 *   - GET    /{id}       详情
 *   - POST   /           新增
 *   - PUT    /{id}       更新
 *   - DELETE /{id}       删除
 *
 * 子类只需实现 4 个抽象方法, 节省 80% CRUD 模板代码.
 *
 * 用法:
 * <pre>
 * {@code
 * @RestController
 * @RequestMapping("/api/v1/user")
 * public class UserController extends BaseController<User, UserService> {
 *
 *     public UserController(UserService service) {
 *         super(service);
 *     }
 *
 *     @Override
 *     protected User doCreate(User entity) { return entity; }
 *
 *     @Override
 *     protected User doUpdate(User entity) { return entity; }
 * }
 * }
 * </pre>
 *
 * @param <T> 实体类型
 * @param <S> Service 类型 (需继承 BaseService)
 */
public abstract class BaseController<T, S extends BaseService<?, T>> {

    protected final S service;

    protected BaseController(S service) {
        this.service = service;
    }

    /**
     * 分页查询
     * GET /page?page=1&size=10
     */
    public Result<Page<T>> page(int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 200) size = 200;
        return Result.ok(service.page(new Page<>(page, size), defaultQueryWrapper()));
    }

    /**
     * 详情
     * GET /{id}
     */
    public Result<T> get(Long id) {
        if (id == null) return Result.fail(ResultCode.BAD_REQUEST);
        T entity = service.getById(id);
        if (entity == null) return Result.fail(ResultCode.NOT_FOUND);
        return Result.ok(entity);
    }

    /**
     * 新增
     * POST /
     */
    public Result<T> create(T entity) {
        T created = doCreate(entity);
        service.save(created);
        return Result.ok(created);
    }

    /**
     * 更新
     * PUT /{id}
     */
    public Result<T> update(Long id, T entity) {
        T updated = doUpdate(entity);
        service.updateById(updated);
        return Result.ok(updated);
    }

    /**
     * 删除
     * DELETE /{id}
     */
    public Result<Void> delete(Long id) {
        if (id == null) return Result.fail(ResultCode.BAD_REQUEST);
        service.removeById(id);
        return Result.ok();
    }

    /**
     * 子类可重写: 创建前处理 (设置默认值等)
     */
    protected T doCreate(T entity) {
        return entity;
    }

    /**
     * 子类可重写: 更新前处理 (强制设置 id 等)
     */
    protected T doUpdate(T entity) {
        return entity;
    }

    /**
     * 子类可重写: 默认查询条件 (过滤 deleted=0, tenantId 等)
     */
    protected QueryWrapper<T> defaultQueryWrapper() {
        return new QueryWrapper<>();
    }
}