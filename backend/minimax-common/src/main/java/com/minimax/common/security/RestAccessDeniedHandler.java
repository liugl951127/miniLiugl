package com.minimax.common.security;

import com.minimax.common.result.Result;
import com.minimax.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 403 Access Denied handler (V5.5 gateway 适配)
 * V5.5 之前实现 spring-security-web AccessDeniedHandler 接口
 * gateway 用 webflux (没有 spring-security-web), 改成 POJO, 通过 @ConditionalOnWebApplication 在 webflux 下跳过
 */
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RestAccessDeniedHandler {

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