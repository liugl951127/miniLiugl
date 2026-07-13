package com.minimax.monitor.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor it = new MybatisPlusInterceptor();
        it.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return it;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override public void insertFill(MetaObject m) {
                this.strictInsertFill(m, "createdAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(m, "updatedAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(m, "createdBy", Long.class, 0L);
                this.strictInsertFill(m, "updatedBy", Long.class, 0L);
                this.strictInsertFill(m, "recordedAt", LocalDateTime.class, LocalDateTime.now());
            }
            @Override public void updateFill(MetaObject m) {
                this.strictUpdateFill(m, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
