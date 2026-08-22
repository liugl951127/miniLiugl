package com.minimax.pipeline.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * Pipeline 模块 MyBatis-Plus 配置 (T1-backend-apis / P0)
 *
 * 提供:
 *   - 分页插件 (MySQL)
 *   - 自动填充: createdAt/updatedAt/createdBy
 *
 * 注: function_ext 已有自己的 FunctionMybatisPlusConfig, 这里只覆盖 pipeline.* 主包。
 *
 * @since V7.2
 */
@Configuration
public class PipelineMybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor it = new MybatisPlusInterceptor();
        it.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return it;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject m) {
                this.strictInsertFill(m, "createdAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(m, "updatedAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(m, "createdBy", Long.class, 0L);
            }

            @Override
            public void updateFill(MetaObject m) {
                this.strictUpdateFill(m, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
