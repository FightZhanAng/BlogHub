package com.blog.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Service 层日志切面 — 自动记录方法入参、执行时间、返回值
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    /** 切入点：com.blog.service 包下所有方法 */
    @Pointcut("execution(public * com.blog.service..*.*(..))")
    public void servicePointcut() {}

    /** 方法调用前记录入参 */
    @Before("servicePointcut()")
    public void logBefore(JoinPoint jp) {
        // Controller 调用的 service 方法用 info，内部调用用 debug
        if (log.isDebugEnabled()) {
            MethodSignature sig = (MethodSignature) jp.getSignature();
            String args = Arrays.stream(jp.getArgs())
                    .map(LoggingAspect::safeToString)
                    .collect(Collectors.joining(", "));
            log.debug("→ {}.{}({})",
                    sig.getDeclaringType().getSimpleName(),
                    sig.getName(),
                    args.length() > 200 ? args.substring(0, 200) + "..." : args);
        }
    }

    /**
     * 脱敏输出参数。
     *
     * <p>login / register / changePassword 这类方法会收到 LoginRequest、
     * ChangePasswordRequest 等 DTO，直接 toString() 会把明文密码写进日志
     * （本项目 com.blog 的日志级别是 debug，于是生产日志里全是密码）。</p>
     */
    private static String safeToString(Object a) {
        if (a == null) {
            return "null";
        }
        if (a instanceof CharSequence) {
            return a.toString();
        }
        String name = a.getClass().getSimpleName();
        // 登录/注册/改密相关的 DTO 只记录类型，不记录内容
        if (name.contains("LoginRequest") || name.contains("Password")
                || name.contains("RegisterRequest") || name.contains("RefreshRequest")) {
            return name + "{***redacted***}";
        }
        String s = a.toString();
        // 兜底：把形如 password=xxx 的片段替换掉
        return s.replaceAll("(?i)(password|oldPassword|newPassword|token|apiKey|secret)=([^,\\)\\s]+)",
                "$1=***");
    }

    /** 方法执行后记录返回值 */
    @AfterReturning(value = "servicePointcut()", returning = "result")
    public void logAfterReturning(JoinPoint jp, Object result) {
        if (log.isDebugEnabled()) {
            MethodSignature sig = (MethodSignature) jp.getSignature();
            String ret = result != null ? result.toString() : "void";
            log.debug("← {}.{} → {}",
                    sig.getDeclaringType().getSimpleName(),
                    sig.getName(),
                    ret.length() > 300 ? ret.substring(0, 300) + "..." : ret);
        }
    }

    /** 方法异常后记录错误 */
    @AfterThrowing(value = "servicePointcut()", throwing = "ex")
    public void logAfterThrowing(JoinPoint jp, Throwable ex) {
        MethodSignature sig = (MethodSignature) jp.getSignature();
        log.error("✗ {}.{} 抛出异常: {}",
                sig.getDeclaringType().getSimpleName(),
                sig.getName(),
                ex.getMessage());
    }

    /** 环绕通知 — 记录执行耗时 */
    @Around("servicePointcut()")
    public Object logExecutionTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > 200) {
                MethodSignature sig = (MethodSignature) pjp.getSignature();
                log.warn("⏱ {}.{} 执行耗时: {}ms", sig.getDeclaringType().getSimpleName(), sig.getName(), elapsed);
            }
            return result;
        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - start;
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            log.error("⏱ {}.{} 执行失败: {}ms, 异常: {}", sig.getDeclaringType().getSimpleName(), sig.getName(), elapsed, e.getMessage());
            throw e;
        }
    }
}
