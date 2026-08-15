package com.minimax.common.audit;

import com.minimax.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * AOP 审计切面。
 *
 * <p>拦截所有标注了 {@link Audited} 的方法，自动记录操作到 DB。</p>
 *
 * <p>特性:</p>
 * <ul>
 *   <li>异步入库（不阻塞业务）</li>
 *   <li>支持 SpEL 表达式提取 resourceId</li>
 *   <li>自动从 ThreadLocal 获取当前用户/IP/UA</li>
 *   <li>记录耗时和结果</li>
 * </ul>
 *
 * @author MiniMax
 * @since V6.8.2
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(AuditRecorder.class)
public class AuditAspect {

    private final AuditRecorder auditRecorder;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer paramDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(audited) || @within(audited)")
    public Object doAudit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        long start = System.currentTimeMillis();
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        Audited ann = method.getAnnotation(Audited.class);
        if (ann == null) ann = audited; // 类级别

        // 1. 提取 resourceId
        String resourceId = resolveResourceId(ann, method, pjp.getArgs());

        // 2. 提取 detail（参数快照）
        Map<String, Object> detail = ann.logRequestBody() ? buildDetail(sig, pjp.getArgs()) : null;

        // 3. 执行目标方法
        Object result = null;
        String resultStr = "ok";
        boolean success = true;
        try {
            result = pjp.proceed();
            if (result instanceof Result<?> r) {
                success = r.getCode() != null && r.getCode() == 0;
                resultStr = success ? "ok" : ("error:" + r.getCode());
            }
        } catch (Throwable ex) {
            success = false;
            resultStr = "exception:" + ex.getClass().getSimpleName();
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - start;
            // 异步写库
            asyncRecord(ann.action(), ann.resourceType(), resourceId, detail, resultStr, duration);
        }
        return result;
    }

    private String resolveResourceId(Audited ann, Method method, Object[] args) {
        String expr = ann.resourceId();
        if (expr == null || expr.isEmpty()) {
            // 默认: 取第一个 Long/Integer 参数
            for (Object arg : args) {
                if (arg instanceof Long l) return String.valueOf(l);
                if (arg instanceof Integer i) return String.valueOf(i);
                if (arg instanceof String s && !s.isEmpty()) return s;
            }
            return null;
        }
        // SpEL 解析
        try {
            Expression expression = parser.parseExpression(expr);
            EvaluationContext ctx = new StandardEvaluationContext();
            Parameter[] params = method.getParameters();
            String[] names = paramDiscoverer.getParameterNames(method);
            if (names != null) {
                for (int i = 0; i < params.length; i++) {
                    ((StandardEvaluationContext) ctx).setVariable(names[i], args[i]);
                }
            }
            Object val = expression.getValue(ctx);
            return val != null ? String.valueOf(val) : null;
        } catch (Exception ex) {
            log.warn("[Audit] SpEL 解析失败 action={} expr={}: {}", ann.action(), expr, ex.getMessage());
            return null;
        }
    }

    private Map<String, Object> buildDetail(MethodSignature sig, Object[] args) {
        Map<String, Object> d = new HashMap<>();
        String[] names = sig.getParameterNames();
        if (names != null) {
            for (int i = 0; i < names.length; i++) {
                // 脱敏: 不记录密码/token/secret
                String n = names[i].toLowerCase();
                if (n.contains("password") || n.contains("secret") || n.contains("token") || n.contains("key")) {
                    d.put(names[i], "******");
                } else {
                    Object arg = args[i];
                    d.put(names[i], arg == null ? null : arg.toString());
                }
            }
        }
        return d;
    }

    @Async
    public void asyncRecord(String action, String resourceType, String resourceId,
                            Map<String, Object> detail, String result, long durationMs) {
        try {
            auditRecorder.record(
                    AuditContext.getUserId(),
                    AuditContext.getUsername(),
                    action,
                    resourceType,
                    resourceId,
                    detail,
                    result,
                    null,
                    getCurrentRequest()
            );
        } catch (Exception ex) {
            log.warn("[Audit] 异步写库失败: {}", ex.getMessage());
        }
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attr != null ? attr.getRequest() : null;
        } catch (Exception ex) {
            return null;
        }
    }
}
