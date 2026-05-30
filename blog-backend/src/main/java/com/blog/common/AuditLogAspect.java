package com.blog.common;

import com.blog.config.JwtUtil;
import com.blog.entity.SysLog;
import com.blog.mapper.SysLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;

/**
 * 操作日志切面（支持 SpEL 表达式解析）
 */
@Aspect
@Component
@lombok.RequiredArgsConstructor
public class AuditLogAspect {

    private final SysLogMapper sysLogMapper;

    private final JwtUtil jwtUtil;

    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        Object result = pjp.proceed();

        try {
            HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes()).getRequest();

            Long userId = null;
            String username = null;
            String auth = req.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring(7);
                userId = jwtUtil.getUserId(token);
                username = jwtUtil.getUsername(token);
            }

            // 构建 SpEL 上下文
            StandardEvaluationContext context = new StandardEvaluationContext();
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Method method = signature.getMethod();
            Parameter[] parameters = method.getParameters();
            Object[] args = pjp.getArgs();
            for (int i = 0; i < parameters.length; i++) {
                if (args[i] != null) {
                    context.setVariable(parameters[i].getName(), args[i]);
                }
            }

            // 解析 SpEL 表达式
            String action = resolveExpression(auditLog.action(), context);
            String resource = resolveExpression(auditLog.resource(), context);

            SysLog sysLog = new SysLog();
            sysLog.setUserId(userId);
            sysLog.setUsername(username);
            sysLog.setAction(action);
            sysLog.setResource(resource);
            sysLog.setIp(req.getRemoteAddr());
            sysLog.setCreatedAt(LocalDateTime.now());
            sysLogMapper.insert(sysLog);
        } catch (Exception e) {
            // 日志记录失败不影响业务
        }

        return result;
    }

    private String resolveExpression(String expression, StandardEvaluationContext context) {
        if (expression == null || expression.isEmpty()) {
            return "";
        }
        try {
            Object value = parser.parseExpression(expression).getValue(context);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            return expression; // 解析失败返回原始值
        }
    }
}
