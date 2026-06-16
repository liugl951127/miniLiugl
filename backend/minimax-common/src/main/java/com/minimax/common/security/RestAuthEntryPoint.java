package com.minimax.common.security;

import com.minimax.common.result.Result;
import com.minimax.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未登录 / token 失效时的统一 JSON 响应。
 */
@Component
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
