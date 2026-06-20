package com.minimax.common.security;

import com.minimax.common.result.Result;
import com.minimax.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 403 Access Denied handler.
 *
 * 业务模块 (servlet 模式) 用 Spring Security, 需要 implements AccessDeniedHandler.
 * Gateway (webflux 模式) 不实例化 (webflux 模式下 spring security webflux 用不同 FilterChain).
 *
 * V5.6: 保持接口实现, 让业务模块编译通过.
 */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp, AccessDeniedException e)
            throws IOException {
        resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(
                Result.fail(ResultCode.FORBIDDEN.getCode(),
                        ResultCode.FORBIDDEN.getMessage()).toJsonString()
        );
    }
}