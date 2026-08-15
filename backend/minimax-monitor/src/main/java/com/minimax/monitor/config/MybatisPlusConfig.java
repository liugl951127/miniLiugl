package com.minimax.monitor.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 全局配置 (V3.5.31+).
 *
 * 关键点:
 *   1. type-aliases-package 限定到 .entity 子包, 避免 alert.AlertNotifier 等 interface 被误扫
 *   2. mapper-locations 显式只读 mapper/ 目录下的 XML
 *   3. 分页插件 (V3.5.31+ 统一拦截)
 *
 * 解决: AlertNotifier.channelType 之前的 MyBatis BindingException
 */
@Configuration
@MapperScan(
    basePackages = "com.minimax.monitor.mapper",
    sqlSessionTemplateRef = "sqlSessionTemplate"
)
public class MybatisPlusConfig {

    /**
     * 分页插件 (V3.5.31+ 统一拦截).
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
