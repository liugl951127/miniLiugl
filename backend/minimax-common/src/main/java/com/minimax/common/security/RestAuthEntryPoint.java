package com.minimax.common.security;

import com.minimax.common.result.Result;
import com.minimax.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未登录 / token 失效时的统一 JSON 响应 (V5.5 gateway 适配).
 * Implements AuthenticationEntryPoint (业务模块 servlet 模式需要).
 * Gateway webflux 模式: 用 @ConditionalOnWebApplication(SERVLET) 跳过 Bean 注册.
 * 由于 webflux 模式下 spring 不实例化 bean, class 仍被引用但没被 new 出来.
 */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse resp, AuthenticationException e)
            throws IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(
                Result.fail(ResultCode.UNAUTHORIZED.getCode(),
                        ResultCode.UNAUTHORIZED.getMessage()).toJsonString()
        );
    }
}